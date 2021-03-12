package com.nextgenbroadcast.mobile.middleware.provider.mmt;

import android.os.ProxyFileDescriptorCallback;
import android.system.ErrnoException;
import android.system.OsConstants;
import android.util.ArrayMap;
import android.util.Log;

import com.nextgenbroadcast.mmt.exoplayer2.ext.MMTClockAnchor;
import com.nextgenbroadcast.mobile.player.MMTConstants;
import com.nextgenbroadcast.mobile.middleware.atsc3.buffer.Atsc3RingBuffer;

import org.ngbp.libatsc3.middleware.android.mmt.MfuByteBufferFragment;
import org.ngbp.libatsc3.middleware.android.mmt.MmtPacketIdContext;
import org.ngbp.libatsc3.middleware.android.mmt.models.MMTAudioDecoderConfigurationRecord;

import java.nio.ByteBuffer;

/*
    deprecated because of Pipe based FileDescriptor usage in MMTContentProvider. See MMTFragmentWriter
 */

@Deprecated
public class MMTFileDescriptor extends ProxyFileDescriptorCallback {
    public static final String TAG = MMTFileDescriptor.class.getSimpleName();

    private static final int MAX_QUEUE_SIZE = 120;
    private static final int MAX_FIRST_MFU_WAIT_TIME = 5000;
    private static final int MAX_KEY_FRAME_WAIT_TIME = 5000;

    private static final byte RING_BUFFER_PAGE_INIT = 1;
    private static final byte RING_BUFFER_PAGE_FRAGMENT = 2;

    private final Boolean audioOnly;

    private final Atsc3RingBuffer fragmentBuffer;
    private final ByteBuffer pageBuffer = ByteBuffer.allocate(150 * 1024);
    private final byte[] header = {(byte) 0xAC, 0x40, (byte) 0xFF, (byte) 0xFF, 0x00, 0x00, 0x00};
    private final ByteBuffer sampleHeaderBuffer = ByteBuffer.allocate(MMTConstants.SIZE_SAMPLE_HEADER);
    private final ArrayMap<Integer, MMTAudioDecoderConfigurationRecord> audioConfigurationMap = new ArrayMap<>();

    private ByteBuffer InitMpuMetadata_HEVC_NAL_Payload = null;
    private ByteBuffer headerBuffer;

    private long videoMfuPresentationTimestampUs = Long.MAX_VALUE;
    private final ArrayMap<Integer, Long> audioMfuPresentationTimestampMap = new ArrayMap<>();
    private long stppMfuPresentationTimestampUs = Long.MAX_VALUE;

    private boolean isActive = true;
    private boolean sendFileHeader = true;
    private boolean readSampleHeader = false;

    private long mpuWaitingStartTime;
    private long keyFrameWaitingStartTime;

    public MMTFileDescriptor(Atsc3RingBuffer fragmentBuffer, boolean audioOnly) {
        this.fragmentBuffer = fragmentBuffer;
        this.audioOnly = audioOnly;

        pageBuffer.limit(0);
    }

    @Override
    public long onGetSize() {
        return Long.MAX_VALUE;
        // UNKNOWN_LENGTH leads to "FuseUnavailableMountException: AppFuse mount point 262 is unavailable" on Android 10+
        //return AssetFileDescriptor.UNKNOWN_LENGTH;
    }

    @Override
    public int onRead(long offset, int size, byte[] data) throws ErrnoException {
        if (!isActive) throw new ErrnoException("onRead", OsConstants.EIO);

        if (size == 0) return 0;

        try {
            final int bytesRead;
            if (sendFileHeader) {
                if (!audioOnly) {
                    if (!hasMpuMetadata()) {
                        if (mpuWaitingStartTime == 0) {
                            mpuWaitingStartTime = System.currentTimeMillis();
                        }

                        scanMpuMetadata(pageBuffer);

                        if (!hasMpuMetadata()) {
                            if ((System.currentTimeMillis() - mpuWaitingStartTime) < MAX_FIRST_MFU_WAIT_TIME) {
                                return 0;
                            } else {
                                throw new ErrnoException("onRead", OsConstants.EIO);
                            }
                        }
                    }

                    if (keyFrameWaitingStartTime == 0) {
                        keyFrameWaitingStartTime = System.currentTimeMillis();
                    }

                    if (!skipUntilKeyFrame() && (System.currentTimeMillis() - keyFrameWaitingStartTime) < MAX_KEY_FRAME_WAIT_TIME) {
                        return 0;
                    }
                }

                //TODO: we need better criteria
                if (audioConfigurationMap.isEmpty()) {
                    return 0;
                }

                if (headerBuffer == null) {
                    headerBuffer = createFileHeader();
                }
                int bytesToRead = Math.min(headerBuffer.remaining(), size);
                if (bytesToRead > 0) {
                    headerBuffer.get(data, 0, bytesToRead);
                }

                sendFileHeader = headerBuffer.remaining() > 0;

                bytesRead = readBufferFully(bytesToRead, size - bytesToRead, data) + bytesToRead;
            } else {
                bytesRead = readBufferFully(0, size, data);
            }

            if (bytesRead < 0) throw new ErrnoException("onRead", OsConstants.EIO);

            return bytesRead;
        } catch (Exception e) {
            Log.d(TAG, "onRead", e);

            throw new ErrnoException("onRead", OsConstants.EIO, e);
        }
    }

    @Override
    public void onRelease() {
        isActive = false;
    }

    private ByteBuffer createFileHeader() {
        byte videoTrackCount = (byte) (audioOnly ? 0 : 1);
        byte audioTrackCount = (byte) audioConfigurationMap.size();
        byte textTrackCount = (byte) (audioOnly ? 0 : 1);

        int mpuMetadataSize = getMpuMetadataSize();
        int videoHeaderSize = MMTConstants.VIDEO_TRACK_HEADER_SIZE * videoTrackCount + mpuMetadataSize;
        int audioHeaderSize = MMTConstants.AUDIO_TRACK_HEADER_SIZE * audioTrackCount;
        int textHeaderSize = MMTConstants.CC_TRACK_HEADER_SIZE * textTrackCount;
        int headerSize = MMTConstants.HEADER_SIZE + videoHeaderSize + audioHeaderSize + textHeaderSize;

        ByteBuffer fileHeaderBuffer = ByteBuffer.allocate(MMTConstants.mmtSignature.length + headerSize);

        // Read identification header
        fileHeaderBuffer.put(MMTConstants.mmtSignature);

        // write stream Header data
        fileHeaderBuffer.putInt(headerSize)
            .putLong(MMTContentProvider.PTS_OFFSET_US);

        if (videoTrackCount > 0) {
            int videoFormat = getIntegerCodeForString("hev1");

            // write initial MFU Metadata
            InitMpuMetadata_HEVC_NAL_Payload.rewind();

            fileHeaderBuffer
                .putInt(MMTConstants.VIDEO_TRACK_HEADER_SIZE + mpuMetadataSize)
                .put((byte) MMTConstants.TRACK_TYPE_VIDEO)
                .putInt(videoFormat)
                .putInt(MmtPacketIdContext.video_packet_id) //TODO: replace MmtPacketIdContext.video_packet_id
                .putInt(getVideoWidth())
                .putInt(getVideoHeight())
                .putFloat(getVideoFrameRate())
                .putInt(mpuMetadataSize)
                .put(InitMpuMetadata_HEVC_NAL_Payload);
        }

        if (audioTrackCount > 0) {
            //jjustman-2020-12-02 - TODO - pass-thru fourcc from
            int audioFormat = getIntegerCodeForString("ac-4");
//                if(true) {
//                    audioFormat = Util.getIntegerCodeForString("mp4a");
//                    /* jjustman-2020-11-30 - needs special audio codec specific metadata */
//                }
            for (MMTAudioDecoderConfigurationRecord info : audioConfigurationMap.values()) {
                fileHeaderBuffer
                        .putInt(MMTConstants.AUDIO_TRACK_HEADER_SIZE)
                        .put((byte) MMTConstants.TRACK_TYPE_AUDIO)
                        .putInt(audioFormat)
                        .putInt(info.packet_id)
                        .putInt(info.channel_count)
                        .putInt(info.sample_rate);
            }
        }

        if (textTrackCount > 0) {
            int ttmlFormat = getIntegerCodeForString("stpp");

            fileHeaderBuffer
                .putInt(MMTConstants.CC_TRACK_HEADER_SIZE)
                .put((byte) MMTConstants.TRACK_TYPE_TEXT)
                .putInt(ttmlFormat)
                .putInt(MmtPacketIdContext.stpp_packet_id); //TODO: replace MmtPacketIdContext.stpp_packet_id
        }

        fileHeaderBuffer.rewind();
        return fileHeaderBuffer;
    }

    private int readBufferFully(int offset, int readLength, byte[] buffer) {
        int bytesAlreadyRead = 0;

        if (readSampleHeader) {
            bytesAlreadyRead = readSampleHeader(offset, readLength, buffer);
        }

        while (bytesAlreadyRead < readLength) {
            int bytesRead = readBuffer(offset + bytesAlreadyRead, readLength - bytesAlreadyRead, buffer);

            if (bytesRead == 0) {
                // Fill with an empty packet to avoid unfilled answer that leads to EOF
                bytesRead = readLength - bytesAlreadyRead;
                sampleHeaderBuffer.clear();
                sampleHeaderBuffer
                        .put((byte) MMTConstants.TRACK_TYPE_EMPTY)
                        .putInt(bytesRead - sampleHeaderBuffer.limit())
                        .putInt(0) // sample id = 0
                        .putLong(0) // PresentationTimestampUs = 0
                        .put((byte) 0); // isKeyFrame = false
                sampleHeaderBuffer.rewind();

                readSampleHeader(offset + bytesAlreadyRead, readLength - bytesAlreadyRead, buffer);
            }
            bytesAlreadyRead += bytesRead;
        }

        return bytesAlreadyRead;
    }

    private int readBuffer(int offset, int readLength, byte[] buffer) {
        if (readLength == 0) {
            return 0;
        }

        if (pageBuffer.remaining() == 0) {
            readFragment(pageBuffer);
        }

        // read the sample buffer
        int bytesToRead = Math.min(pageBuffer.remaining(), readLength);
        if (bytesToRead == 0) {
            return 0;
        }

        pageBuffer.get(buffer, offset, bytesToRead);

        return bytesToRead;
    }

    private void scanMpuMetadata(ByteBuffer buffer) {
        while (true) {
            int bufferLen = fragmentBuffer.readNextPage(buffer);
            if (bufferLen <= 0) {
                buffer.limit(0);
                return;
            }

            int pageType = buffer.get();
            if (pageType != RING_BUFFER_PAGE_INIT) continue;

            if (InitMpuMetadata_HEVC_NAL_Payload == null) {
                ByteBuffer init = ByteBuffer.allocate(bufferLen);
                //TODO: rewrite offset
                int offset = 4 /*packet_id*/ + 4 /*sample_number*/ + 8 /*mpu_presentation_time_uS_from_SI*/ + 7 /*reserved*/;
                init.put(buffer.array(), buffer.position() + offset /* reserved bytes */, bufferLen);
                InitMpuMetadata_HEVC_NAL_Payload = init;
            }

            buffer.limit(0);

            return;
        }
    }

    private void readFragment(ByteBuffer buffer) {
        while (true) {
            int bufferLen = fragmentBuffer.readNextPage(buffer);
            if (bufferLen <= 0) return;

            int pageType = buffer.get();
            if (pageType != RING_BUFFER_PAGE_FRAGMENT) continue;

            int packet_id = fragmentBuffer.getInt(buffer);
            int sample_number = fragmentBuffer.getInt(buffer);
            long mpu_presentation_time_uS_from_SI = fragmentBuffer.getLong(buffer);

            byte sampleType = MMTConstants.TRACK_TYPE_UNKNOWN;
            if (isVideoSample(packet_id)) {
                sampleType = MMTConstants.TRACK_TYPE_VIDEO;
            } else if (isAudioSample(packet_id)) {
                sampleType = MMTConstants.TRACK_TYPE_AUDIO;
            } else if (isTextSample(packet_id)) {
                sampleType = MMTConstants.TRACK_TYPE_TEXT;
            }

            long computedPresentationTimestampUs = getPresentationTimestampUs(packet_id, sample_number, mpu_presentation_time_uS_from_SI);

            int headerDiff = Atsc3RingBuffer.RING_BUFFER_PAGE_HEADER_SIZE - MMTConstants.SIZE_SAMPLE_HEADER;

            if (sampleType == MMTConstants.TRACK_TYPE_AUDIO) {
                headerDiff -= header.length;

                header[4] = (byte) (bufferLen >> 16 & 0xFF);
                header[5] = (byte) (bufferLen >> 8 & 0xFF);
                header[6] = (byte) (bufferLen & 0xFF);

                buffer.position(headerDiff + MMTConstants.SIZE_SAMPLE_HEADER);
                buffer.put(header);

                bufferLen += header.length;
            }

            buffer.position(headerDiff);
            buffer.put(sampleType)
                    .putInt(bufferLen/*fragment.bytebuffer_length*/)
                    .putInt(packet_id)
                    .putLong(computedPresentationTimestampUs)
                    .put(isKeySample(sample_number) ? (byte) 1 : (byte) 0);

            int sampleRemaining = bufferLen + MMTConstants.SIZE_SAMPLE_HEADER + headerDiff;
            int limit = Math.max(sampleRemaining, 0);
            buffer.limit(limit);
            buffer.position(headerDiff);

            return;
        }
    }

    private int readSampleHeader(int offset, int readLength, byte[] buffer) {
        int bytesToRead = Math.min(sampleHeaderBuffer.remaining(), readLength);
        sampleHeaderBuffer.get(buffer, offset, bytesToRead);
        readSampleHeader = sampleHeaderBuffer.remaining() != 0;
        return bytesToRead;
    }

    private boolean hasMpuMetadata() {
        return InitMpuMetadata_HEVC_NAL_Payload != null;
    }

    //TODO: implement
    private boolean skipUntilKeyFrame() {
//        Pair<MfuByteBufferFragment, ByteBuffer> fragment;
//        while ((fragment = mfuBufferQueue.peek()) != null) {
//            if (isKeySample(fragment.first) && !isTextSample(fragment.first)) {
//                return true;
//            } else {
//                mfuBufferQueue.remove();
//            }
//        }
//
//        return false;
        return true;
    }

    private int getVideoWidth() {
        return MmtPacketIdContext.video_packet_statistics.width > 0 ? MmtPacketIdContext.video_packet_statistics.width : MmtPacketIdContext.MmtMfuStatistics.FALLBACK_WIDTH;
    }

    //TODO: refactor to get rid of MmtPacketIdContext
    private int getVideoHeight() {
        return MmtPacketIdContext.video_packet_statistics.height > 0 ? MmtPacketIdContext.video_packet_statistics.height : MmtPacketIdContext.MmtMfuStatistics.FALLBACK_HEIGHT;
    }

    private float getVideoFrameRate() {
        return (float) 1000000.0 / MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us;
    }

    @Deprecated
    private boolean isVideoSample(MfuByteBufferFragment sample) {
        return MmtPacketIdContext.video_packet_id == sample.packet_id;
    }

    private boolean isVideoSample(int packet_id) {
        return MmtPacketIdContext.video_packet_id == packet_id;
    }

    @Deprecated
    private boolean isAudioSample(MfuByteBufferFragment sample) {
        return audioConfigurationMap.containsKey(sample.packet_id);
    }

    private boolean isAudioSample(int packet_id) {
        return MmtPacketIdContext.isAudioPacket(packet_id) && audioConfigurationMap.containsKey(packet_id);
    }

    @Deprecated
    private boolean isTextSample(MfuByteBufferFragment sample) {
        return MmtPacketIdContext.stpp_packet_id == sample.packet_id;
    }

    private boolean isTextSample(int packet_id) {
        return MmtPacketIdContext.stpp_packet_id == packet_id;
    }

    @Deprecated
    private boolean isKeySample(MfuByteBufferFragment fragment) {
        return fragment.sample_number == 1;
    }

    private boolean isKeySample(int sample_number) {
        return sample_number == 1;
    }

    private int getMpuMetadataSize() {
        if (InitMpuMetadata_HEVC_NAL_Payload == null) return 0;
        return InitMpuMetadata_HEVC_NAL_Payload.limit();
    }

    //jjustman-2020-12-22 - TODO: handle when mfu_presentation_time_uS_computed - push last value?
    private long getPresentationTimestampUs(MfuByteBufferFragment toProcessMfuByteBufferFragment) {
        if (toProcessMfuByteBufferFragment.mfu_presentation_time_uS_computed != null && toProcessMfuByteBufferFragment.mfu_presentation_time_uS_computed > 0) {
            //todo: expand size as needed, every ~ mfu_presentation_time_uS_computed 1000000uS
            if (isVideoSample(toProcessMfuByteBufferFragment)) {
                if (videoMfuPresentationTimestampUs == Long.MAX_VALUE) {
                    videoMfuPresentationTimestampUs = toProcessMfuByteBufferFragment.mfu_presentation_time_uS_computed;
                }
            } else if (isAudioSample(toProcessMfuByteBufferFragment)) {
                if (!audioMfuPresentationTimestampMap.containsKey(toProcessMfuByteBufferFragment.packet_id)) {
                    audioMfuPresentationTimestampMap.put(toProcessMfuByteBufferFragment.packet_id, toProcessMfuByteBufferFragment.mfu_presentation_time_uS_computed);
                }
            } else if (isTextSample(toProcessMfuByteBufferFragment)) {
                if (stppMfuPresentationTimestampUs == Long.MAX_VALUE) {
                    stppMfuPresentationTimestampUs = toProcessMfuByteBufferFragment.mfu_presentation_time_uS_computed;
                }
            }
            long anchorMfuPresentationTimestampUs = getMinNonZeroMfuPresentationTimestampForAnchor(toProcessMfuByteBufferFragment.packet_id);
            long mpuPresentationTimestampDeltaUs = toProcessMfuByteBufferFragment.mfu_presentation_time_uS_computed - anchorMfuPresentationTimestampUs;
            return mpuPresentationTimestampDeltaUs + MMTContentProvider.PTS_OFFSET_US;
        }

        return 0;
    }

    private long getPresentationTimestampUs(int packet_id, int sample_number, long mpu_presentation_time_uS_from_SI) {
        if (mpu_presentation_time_uS_from_SI > 0) {
            long mfu_presentation_time_uS_computed = 0;
            long extracted_sample_duration_us;
            if (packet_id == MmtPacketIdContext.video_packet_id && MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us > 0) {
                mfu_presentation_time_uS_computed = mpu_presentation_time_uS_from_SI + (sample_number - 1) * MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us;
            } else if (isAudioSample(packet_id) && (extracted_sample_duration_us = MmtPacketIdContext.getAudioPacketStatistic(packet_id).extracted_sample_duration_us) > 0) {
                mfu_presentation_time_uS_computed = mpu_presentation_time_uS_from_SI + (sample_number - 1) * extracted_sample_duration_us;
            } else if (packet_id == MmtPacketIdContext.stpp_packet_id && MmtPacketIdContext.stpp_packet_statistics.extracted_sample_duration_us > 0) {
                mfu_presentation_time_uS_computed = mpu_presentation_time_uS_from_SI + (sample_number - 1) * MmtPacketIdContext.stpp_packet_statistics.extracted_sample_duration_us;
            }

            if (mfu_presentation_time_uS_computed > 0) {
                //todo: expand size as needed, every ~ mfu_presentation_time_uS_computed 1000000uS
                if (isVideoSample(packet_id)) {
                    if (videoMfuPresentationTimestampUs == Long.MAX_VALUE) {
                        videoMfuPresentationTimestampUs = mfu_presentation_time_uS_computed;
                    }
                } else if (isAudioSample(packet_id)) {
                    if (!audioMfuPresentationTimestampMap.containsKey(packet_id)) {
                        audioMfuPresentationTimestampMap.put(packet_id, mfu_presentation_time_uS_computed);
                    }
                } else if (isTextSample(packet_id)) {
                    if (stppMfuPresentationTimestampUs == Long.MAX_VALUE) {
                        stppMfuPresentationTimestampUs = mfu_presentation_time_uS_computed;
                    }
                }
                long anchorMfuPresentationTimestampUs = getMinNonZeroMfuPresentationTimestampForAnchor(packet_id);
                long mpuPresentationTimestampDeltaUs = mfu_presentation_time_uS_computed - anchorMfuPresentationTimestampUs;
                return mpuPresentationTimestampDeltaUs + MMTContentProvider.PTS_OFFSET_US;
            }
        }

        return 0;
    }

    private long getMinNonZeroMfuPresentationTimestampForAnchor(int packet_id) {
        long minNonZeroMfuPresentationTimestampForAnchor = Long.MAX_VALUE;

        Long audioMfuPresentationTimestampUs = audioMfuPresentationTimestampMap.get(packet_id);
        if (videoMfuPresentationTimestampUs != Long.MAX_VALUE) {
            minNonZeroMfuPresentationTimestampForAnchor = videoMfuPresentationTimestampUs;
            if (audioMfuPresentationTimestampUs != null) {
                minNonZeroMfuPresentationTimestampForAnchor = Math.min(minNonZeroMfuPresentationTimestampForAnchor, audioMfuPresentationTimestampUs);
                if (stppMfuPresentationTimestampUs != Long.MAX_VALUE) {
                    minNonZeroMfuPresentationTimestampForAnchor = Math.min(minNonZeroMfuPresentationTimestampForAnchor, stppMfuPresentationTimestampUs);
                }
            }
        } else if (audioMfuPresentationTimestampUs != null) {
            minNonZeroMfuPresentationTimestampForAnchor = audioMfuPresentationTimestampUs;
            if (stppMfuPresentationTimestampUs != Long.MAX_VALUE) {
                minNonZeroMfuPresentationTimestampForAnchor = Math.min(minNonZeroMfuPresentationTimestampForAnchor, stppMfuPresentationTimestampUs);
            }
        } else if (stppMfuPresentationTimestampUs != Long.MAX_VALUE) {
            minNonZeroMfuPresentationTimestampForAnchor = stppMfuPresentationTimestampUs;
        }

        if(MMTClockAnchor.SystemClockAnchor == 0) {
            MMTClockAnchor.SystemClockAnchor = System.currentTimeMillis();
        }

        if(MMTClockAnchor.MfuClockAnchor < minNonZeroMfuPresentationTimestampForAnchor ) {
            MMTClockAnchor.MfuClockAnchor = minNonZeroMfuPresentationTimestampForAnchor;
        }

        return minNonZeroMfuPresentationTimestampForAnchor;
    }

    public void pushAudioDecoderConfigurationRecord(MMTAudioDecoderConfigurationRecord mmtAudioDecoderConfigurationRecord) {
        audioConfigurationMap.put(mmtAudioDecoderConfigurationRecord.packet_id, mmtAudioDecoderConfigurationRecord);
    }

    /**
     * Copied from ExoPlayer/Utils.java
     */
    private static int getIntegerCodeForString(String string) {
        int length = string.length();
        if (length > 4) throw new IllegalArgumentException();
        int result = 0;
        for (int i = 0; i < length; i++) {
            result <<= 8;
            result |= string.charAt(i);
        }
        return result;
    }
}
