package com.nextgenbroadcast.mobile.middleware.provider.mmt;

import android.util.ArrayMap;
import android.util.Log;
import android.util.Pair;

import com.nextgenbroadcast.mmt.exoplayer2.ext.MMTClockAnchor;
import com.nextgenbroadcast.mobile.player.MMTConstants;

import org.ngbp.libatsc3.middleware.android.mmt.MfuByteBufferFragment;
import org.ngbp.libatsc3.middleware.android.mmt.MmtPacketIdContext;
import org.ngbp.libatsc3.middleware.android.mmt.MpuMetadata_HEVC_NAL_Payload;
import org.ngbp.libatsc3.middleware.android.mmt.models.MMTAudioDecoderConfigurationRecord;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class MMTFragmentWriter {
    public static final String TAG = MMTFragmentWriter.class.getSimpleName();

    private static final int MAX_QUEUE_SIZE = 120;
    private static final int MAX_FIRST_MFU_WAIT_TIME = 5000;
    private static final int MAX_KEY_FRAME_WAIT_TIME = 5000;
    private static final int MAX_FRAGMENT_WAIT_TIME = 100;

    private final Boolean audioOnly;

    private final LinkedBlockingQueue<Pair<MfuByteBufferFragment, ByteBuffer>> mfuBufferQueue = new LinkedBlockingQueue<>();
    private final ByteBuffer sampleHeaderBuffer = ByteBuffer.allocate(MMTConstants.SIZE_SAMPLE_HEADER);
    private final ArrayMap<Integer, MMTAudioDecoderConfigurationRecord> audioConfigurationMap = new ArrayMap<>();

    private ByteBuffer InitMpuMetadata_HEVC_NAL_Payload = null;
    private ByteBuffer headerBuffer;

    private long videoMfuPresentationTimestampUs = Long.MAX_VALUE;
    private final ArrayMap<Integer, Long> audioMfuPresentationTimestampMap = new ArrayMap<>();
    private long stppMfuPresentationTimestampUs = Long.MAX_VALUE;

    private volatile boolean isActive = true;
    private boolean sendFileHeader = true;

    private long mpuWaitingStartTime;
    private long keyFrameWaitingStartTime;

    public MMTFragmentWriter(boolean audioOnly) {
        this.audioOnly = audioOnly;
    }

    public int write(FileOutputStream out) throws IOException {
        if (!isActive) throw new IOException("Reader is not active");

        int bytesRead = 0;
        if (sendFileHeader) {
            if (!audioOnly) {
                if (!hasMpuMetadata()) {
                    if (mpuWaitingStartTime == 0) {
                        mpuWaitingStartTime = System.currentTimeMillis();
                    }

                    if ((System.currentTimeMillis() - mpuWaitingStartTime) < MAX_FIRST_MFU_WAIT_TIME) {
                        return 0;
                    } else {
                        throw new IOException("Can't get MPU Metadata");
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

            bytesRead += writeBuffer(out, headerBuffer);
            out.flush();

            sendFileHeader = false;
        }

        bytesRead += writeQueue(out);

        return bytesRead;
    }

    public void close() {
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
        fileHeaderBuffer.putInt(headerSize);

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

    private int writeQueue(FileOutputStream out) throws IOException {
        int bytesRead = 0;
        while (isActive) {
            // get next sample and fill it's header
            final Pair<MfuByteBufferFragment, ByteBuffer> sample;
            try {
                if ((sample = mfuBufferQueue.poll(MAX_FRAGMENT_WAIT_TIME, TimeUnit.MILLISECONDS)) == null) {
                    break;
                }
            } catch (InterruptedException e) {
                break;
            }

            final ByteBuffer currentSample = sample.second;
            final MfuByteBufferFragment fragment = sample.first;

            byte sampleType = MMTConstants.TRACK_TYPE_UNKNOWN;
            if (isVideoSample(fragment)) {
                sampleType = MMTConstants.TRACK_TYPE_VIDEO;
            } else if (isAudioSample(fragment)) {
                sampleType = MMTConstants.TRACK_TYPE_AUDIO;
            } else if (isTextSample(fragment)) {
                sampleType = MMTConstants.TRACK_TYPE_TEXT;
            }

            long computedPresentationTimestampUs = getPresentationTimestampUs(fragment);

            sampleHeaderBuffer.clear();
            sampleHeaderBuffer
                    .put(sampleType)
                    .putInt(fragment.bytebuffer_length)
                    .putInt(fragment.packet_id)
                    .putLong(computedPresentationTimestampUs)
                    .put(isKeySample(fragment) ? (byte) 1 : (byte) 0);
            sampleHeaderBuffer.rewind();

            // read the sample header
            bytesRead += writeBuffer(out, sampleHeaderBuffer);

            // read the sample buffer
            bytesRead += writeBuffer(out, currentSample);

            out.flush();
        }

        return bytesRead;
    }

    private int writeBuffer(FileOutputStream out, ByteBuffer buffer) throws IOException {
        int bytesToWrite = buffer.remaining();
        out.write(buffer.array(), buffer.position(), bytesToWrite);
        return bytesToWrite;
    }

    private boolean hasMpuMetadata() {
        return InitMpuMetadata_HEVC_NAL_Payload != null;
    }

    private boolean skipUntilKeyFrame() {
        Pair<MfuByteBufferFragment, ByteBuffer> fragment;
        while ((fragment = mfuBufferQueue.peek()) != null) {
            if (isKeySample(fragment.first) && !isTextSample(fragment.first)) {
                return true;
            } else {
                mfuBufferQueue.remove();
            }
        }

        return false;
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

    private boolean isVideoSample(MfuByteBufferFragment sample) {
        return MmtPacketIdContext.video_packet_id == sample.packet_id;
    }

    private boolean isAudioSample(MfuByteBufferFragment sample) {
        return audioConfigurationMap.containsKey(sample.packet_id);
    }

    private boolean isTextSample(MfuByteBufferFragment sample) {
        return MmtPacketIdContext.stpp_packet_id == sample.packet_id;
    }

    private boolean isKeySample(MfuByteBufferFragment fragment) {
        return fragment.sample_number == 1;
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
            return toProcessMfuByteBufferFragment.mfu_presentation_time_uS_computed - anchorMfuPresentationTimestampUs;
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

        if (MMTClockAnchor.SystemClockAnchor == 0) {
            MMTClockAnchor.SystemClockAnchor = System.currentTimeMillis();
        }

        if (MMTClockAnchor.MfuClockAnchor < minNonZeroMfuPresentationTimestampForAnchor) {
            MMTClockAnchor.MfuClockAnchor = minNonZeroMfuPresentationTimestampForAnchor;
        }


        return minNonZeroMfuPresentationTimestampForAnchor;
    }

    public void InitMpuMetadata_HEVC_NAL_Payload(MpuMetadata_HEVC_NAL_Payload payload) {
        if (!isActive) return;

        if (InitMpuMetadata_HEVC_NAL_Payload == null) {
            InitMpuMetadata_HEVC_NAL_Payload = payload.myByteBuffer.duplicate();
        } /*else {
            //Do not free it here payload.releaseByteBuffer();
        }*/
    }

    private final byte[] header = {(byte) 0xAC, 0x40, (byte) 0xFF, (byte) 0xFF, 0x00, 0x00, 0x00};

    public void PushMfuByteBufferFragment(MfuByteBufferFragment mfuByteBufferFragment) {
        if (!isActive) return;

        //jjustman-2020-12-09 - hacks to make sure we don't fall too far behind wall-clock
        if (mfuBufferQueue.size() > MAX_QUEUE_SIZE) {
            Log.w(TAG, String.format("PushMfuByteBufferFragment: clearing queue, length: %d",
                    mfuBufferQueue.size()));
            mfuBufferQueue.clear();
        }

        // Temporary solution to fix AC-4 header for alternative audio tracks
        boolean isVideo = isVideoSample(mfuByteBufferFragment);
        boolean isAudio = isAudioSample(mfuByteBufferFragment);
        boolean isText = isTextSample(mfuByteBufferFragment);
        if (isVideo || isAudio || isText) {
            // hack that fixes alternative to MmtPacketIdContext.audio_packet_id audio tracks
            final ByteBuffer buffer;
            if (isAudio) {
                int length = mfuByteBufferFragment.myByteBuffer.remaining();
                int id = mfuByteBufferFragment.myByteBuffer.getInt(mfuByteBufferFragment.myByteBuffer.position());
                if (id != 0xAC40FFFF) {
                    header[4] = (byte) (length >> 16 & 0xFF);
                    header[5] = (byte) (length >> 8 & 0xFF);
                    header[6] = (byte) (length & 0xFF);

                    length += 7;
                    buffer = ByteBuffer.allocate(length);
                    buffer.put(header);
                    buffer.put(mfuByteBufferFragment.myByteBuffer);
                    buffer.rewind();

                    mfuByteBufferFragment.bytebuffer_length = length;
                } else {
                    buffer = mfuByteBufferFragment.myByteBuffer.duplicate();
                }
            } else {
                buffer = mfuByteBufferFragment.myByteBuffer.duplicate();
            }

            if (mfuByteBufferFragment.mfu_presentation_time_uS_computed == null) {
                MmtPacketIdContext.MmtMfuStatistics statistic;

                if (mfuByteBufferFragment.mpu_presentation_time_uS_from_SI != null && mfuByteBufferFragment.mpu_presentation_time_uS_from_SI > 0) {
                    if (isVideo && MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us > 0) {
                        mfuByteBufferFragment.mfu_presentation_time_uS_computed = mfuByteBufferFragment.mpu_presentation_time_uS_from_SI + (mfuByteBufferFragment.sample_number - 1) * MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us;
                    } else if (isAudio && ((statistic = MmtPacketIdContext.getAudioPacketStatistic(mfuByteBufferFragment.packet_id)) != null && statistic.extracted_sample_duration_us > 0)) {
                        mfuByteBufferFragment.mfu_presentation_time_uS_computed = mfuByteBufferFragment.mpu_presentation_time_uS_from_SI + (mfuByteBufferFragment.sample_number - 1) * statistic.extracted_sample_duration_us;
                    } else if (isText && MmtPacketIdContext.stpp_packet_statistics.extracted_sample_duration_us > 0) {
                        mfuByteBufferFragment.mfu_presentation_time_uS_computed = mfuByteBufferFragment.mpu_presentation_time_uS_from_SI + (mfuByteBufferFragment.sample_number - 1) * MmtPacketIdContext.stpp_packet_statistics.extracted_sample_duration_us;
                    }
                    Log.i(TAG, String.format("mfuByteBufferFragment.mfu_presentation_time_uS_computed is NULL for packet_id: %d, mpu_sequence_number: %d, sample_number: %d, COMPUTED mpu_presentation_time_uS_from_SI: %d", mfuByteBufferFragment.packet_id, mfuByteBufferFragment.mpu_sequence_number, mfuByteBufferFragment.sample_number, mfuByteBufferFragment.mpu_presentation_time_uS_from_SI));

                } else {
                    Log.w(TAG, String.format("mfuByteBufferFragment.mfu_presentation_time_uS_computed is NULL for packet_id: %d, mpu_sequence_number: %d, sample_number: %d, ORIGINAL mpu_presentation_time_uS_from_SI: %d", mfuByteBufferFragment.packet_id, mfuByteBufferFragment.mpu_sequence_number, mfuByteBufferFragment.sample_number, mfuByteBufferFragment.mpu_presentation_time_uS_from_SI));
                }
            }

            mfuBufferQueue.add(
                    Pair.create(mfuByteBufferFragment, buffer)
            );
        }
    }

    public void pushAudioDecoderConfigurationRecord(MMTAudioDecoderConfigurationRecord mmtAudioDecoderConfigurationRecord) {
        audioConfigurationMap.put(mmtAudioDecoderConfigurationRecord.packet_id, mmtAudioDecoderConfigurationRecord);
    }

    public int getQueueSize() {
        return mfuBufferQueue.size();
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
