package com.nextgenbroadcast.mobile.middleware.provider.mmt;

import android.os.ProxyFileDescriptorCallback;
import android.system.ErrnoException;
import android.system.OsConstants;
import android.util.Log;
import android.util.Pair;

import com.nextgenbroadcast.mmt.exoplayer2.ext.MMTExtractor;
import com.nextgenbroadcast.mobile.core.atsc3.mmt.MMTConstants;

import org.ngbp.libatsc3.middleware.android.mmt.MfuByteBufferFragment;
import org.ngbp.libatsc3.middleware.android.mmt.MmtPacketIdContext;
import org.ngbp.libatsc3.middleware.android.mmt.MpuMetadata_HEVC_NAL_Payload;
import org.ngbp.libatsc3.middleware.android.mmt.models.MMTAudioDecoderConfigurationRecord;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedDeque;

public class MMTFileDescriptor extends ProxyFileDescriptorCallback {
    public static final String TAG = MMTFileDescriptor.class.getSimpleName();

    private static final int MAX_QUEUE_SIZE = 120;
    private static final int MAX_KEY_FRAME_WAIT_TIME = 5000;

    private final ConcurrentLinkedDeque<Pair<MfuByteBufferFragment, ByteBuffer>> mfuBufferQueue = new ConcurrentLinkedDeque<>();
    private final ByteBuffer sampleHeaderBuffer = ByteBuffer.allocate(MMTConstants.SIZE_SAMPLE_HEADER);

    private ByteBuffer InitMpuMetadata_HEVC_NAL_Payload = null;
    private ByteBuffer currentSample = null;
    private ByteBuffer headerBuffer;

    private long videoMfuPresentationTimestampUs = Long.MAX_VALUE;
    private long audioMfuPresentationTimestampUs = Long.MAX_VALUE;
    private long stppMfuPresentationTimestampUs = Long.MAX_VALUE;

    private int audioChannelCount;
    private int audioSampleRate;

    private boolean isActive = true;
    private boolean sendFileHeader = true;
    private boolean readSampleHeader = false;
    private long keyFrameWaitingStartTime;

    public MMTFileDescriptor() {
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
                if (!hasMpuMetadata()) {
                    return 0;
                }

                if (keyFrameWaitingStartTime == 0) {
                    keyFrameWaitingStartTime = System.currentTimeMillis();
                }

                if (!skipUntilKeyFrame() && (System.currentTimeMillis() - keyFrameWaitingStartTime) < MAX_KEY_FRAME_WAIT_TIME) {
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
        int mpuMetadataSize = getMpuMetadataSize();
        int headerSize = MMTConstants.mmtSignature.length + MMTConstants.SIZE_HEADER + mpuMetadataSize;

        ByteBuffer fileHeaderBuffer = ByteBuffer.allocate(headerSize);

        // Read identification header
        fileHeaderBuffer.put(MMTConstants.mmtSignature);

        // write stream Header data
        //jjustman-2020-12-02 - TODO - pass-thru fourcc from
        int videoFormat = getIntegerCodeForString("hev1");
        int audioFormat = getIntegerCodeForString("ac-4");
//                if(true) {
//                    audioFormat = Util.getIntegerCodeForString("mp4a");
//                    /* jjustman-2020-11-30 - needs special audio codec specific metadata */
//                }
        int ttmlFormat = getIntegerCodeForString("stpp");
        fileHeaderBuffer.putInt(videoFormat)
                .putInt(audioFormat)
                .putInt(ttmlFormat)
                .putInt(getVideoWidth())
                .putInt(getVideoHeight())
                .putFloat(getVideoFrameRate())
                .putInt(mpuMetadataSize)
                .putInt(audioChannelCount)
                .putInt(audioSampleRate)
                .putLong(MMTContentProvider.PTS_OFFSET_US);

        // write initial MFU Metadata
        InitMpuMetadata_HEVC_NAL_Payload.rewind();
        fileHeaderBuffer.put(InitMpuMetadata_HEVC_NAL_Payload);

        fileHeaderBuffer.rewind();

        return fileHeaderBuffer;
    }

    private int readBufferFully(int offset, int readLength, byte[] buffer) {
        //Log.d("!!!", "readBufferFully: offset = " + offset + ", readLength = " + readLength + ", buffer.length = " + buffer.length);
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
                        .put((byte) MMTConstants.TRACK_TYPE_UNKNOWN)
                        .putInt(bytesRead - sampleHeaderBuffer.limit())
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

        // get next sample and fill it's header
        if (currentSample == null) {
            final Pair<MfuByteBufferFragment, ByteBuffer> sample;
            if ((sample = mfuBufferQueue.poll()) == null) {
                return 0;
            }

            currentSample = sample.second;
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

//            if(lastComputedPresentationTimestampUs - computedPresentationTimestampUs > 10000000) {
//                Log.w("MMTDataSource", "JJ: >>> packetId had timestamp wraparound!");
//            }
//            lastComputedPresentationTimestampUs = computedPresentationTimestampUs;
//
//            if((DEBUG_COUNTER++) % 10 == 0) {
//                Log.d("MMTDataSource", String.format("JJ: >>> packet_id: %d, computedPresentationTimestampUs: %d (%d) mpu_sequence number: %d, sample number: %d, debug_counter: %d",
//                        currentSample.packet_id,
//                        computedPresentationTimestampUs,
//                        currentSample.get_safe_mfu_presentation_time_uS_computed(),
//                        currentSample.mpu_sequence_number,
//                        currentSample.sample_number,
//                        DEBUG_COUNTER));
//            }
//
////            Log.d("!!!", ">>> sample TimeUs: " + computedPresentationTimestampUs
////                    + ",  sample size: " + toProcessMfuByteBufferFragment.bytebuffer_length
////                    + ",  sample type: " + sampleType
////                    + ", sequence number: " + toProcessMfuByteBufferFragment.mpu_sequence_number);

            sampleHeaderBuffer.clear();
            sampleHeaderBuffer
                    .put(sampleType)
                    .putInt(fragment.bytebuffer_length)
                    .putLong(computedPresentationTimestampUs)
                    .put(isKeySample(fragment) ? (byte) 1 : (byte) 0);
            sampleHeaderBuffer.rewind();

            readSampleHeader = true;
        }

        // read the sample header
        if (readSampleHeader) {
            return readSampleHeader(offset, readLength, buffer);
        }

        // read the sample buffer
        int bytesToRead = Math.min(currentSample.remaining(), readLength);
        //Log.d("!!!", "currentSample !!! bytesToRead: " + bytesToRead + ", remaining: " + currentSample.second/*.myByteBuffer*/.remaining());
        if (bytesToRead == 0) {
            currentSample = null;
            return 0;
        }

        currentSample.get(buffer, offset, bytesToRead);

        if (currentSample.remaining() == 0) {
            // Do not clear, it could be used by another descriptor currentSample.unreferenceByteBuffer();
            currentSample = null;
        }

        return bytesToRead;
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
        return MmtPacketIdContext.audio_packet_id == sample.packet_id;
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
            //default values here as fallback
            long anchorMfuPresentationTimestampUs = toProcessMfuByteBufferFragment.mpu_presentation_time_uS_from_SI;

            //todo: expand size as needed, every ~ mfu_presentation_time_uS_computed 1000000uS
            if (isVideoSample(toProcessMfuByteBufferFragment)) {
                if (videoMfuPresentationTimestampUs == Long.MAX_VALUE) {
                    videoMfuPresentationTimestampUs = toProcessMfuByteBufferFragment.mfu_presentation_time_uS_computed;
                }
            } else if (isAudioSample(toProcessMfuByteBufferFragment)) {
                if (audioMfuPresentationTimestampUs == Long.MAX_VALUE) {
                    audioMfuPresentationTimestampUs = toProcessMfuByteBufferFragment.mfu_presentation_time_uS_computed;
                }
            } else if (isTextSample(toProcessMfuByteBufferFragment)) {
                if (stppMfuPresentationTimestampUs == Long.MAX_VALUE) {
                    stppMfuPresentationTimestampUs = toProcessMfuByteBufferFragment.mfu_presentation_time_uS_computed;
                }
            }
            anchorMfuPresentationTimestampUs = getMinNonZeroMfuPresentationTimestampForAnchor();

            long mpuPresentationTimestampDeltaUs = toProcessMfuByteBufferFragment.mfu_presentation_time_uS_computed - anchorMfuPresentationTimestampUs;
            return mpuPresentationTimestampDeltaUs + MMTContentProvider.PTS_OFFSET_US;
        }

        return 0;
    }

    private long getMinNonZeroMfuPresentationTimestampForAnchor() {
        long minNonZeroMfuPresentationTimestampForAnchor = Long.MAX_VALUE;

        if (videoMfuPresentationTimestampUs != Long.MAX_VALUE) {
            minNonZeroMfuPresentationTimestampForAnchor = videoMfuPresentationTimestampUs;
            if (audioMfuPresentationTimestampUs != Long.MAX_VALUE) {
                minNonZeroMfuPresentationTimestampForAnchor = Math.min(minNonZeroMfuPresentationTimestampForAnchor, audioMfuPresentationTimestampUs);
                if (stppMfuPresentationTimestampUs != Long.MAX_VALUE) {
                    minNonZeroMfuPresentationTimestampForAnchor = Math.min(minNonZeroMfuPresentationTimestampForAnchor, stppMfuPresentationTimestampUs);
                }
            }
        } else if (audioMfuPresentationTimestampUs != Long.MAX_VALUE) {
            minNonZeroMfuPresentationTimestampForAnchor = audioMfuPresentationTimestampUs;
            if (stppMfuPresentationTimestampUs != Long.MAX_VALUE) {
                minNonZeroMfuPresentationTimestampForAnchor = Math.min(minNonZeroMfuPresentationTimestampForAnchor, stppMfuPresentationTimestampUs);
            }
        } else if (stppMfuPresentationTimestampUs != Long.MAX_VALUE) {
            minNonZeroMfuPresentationTimestampForAnchor = stppMfuPresentationTimestampUs;
        }

        if(MMTExtractor.SystemClockAnchor == 0) {
            MMTExtractor.SystemClockAnchor = System.currentTimeMillis();
        }

        if(MMTExtractor.MfuClockAnchor < minNonZeroMfuPresentationTimestampForAnchor ) {
            MMTExtractor.MfuClockAnchor = minNonZeroMfuPresentationTimestampForAnchor;
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

    public void PushMfuByteBufferFragment(MfuByteBufferFragment mfuByteBufferFragment) {
        if (!isActive) return;

        //jjustman-2020-12-09 - hacks to make sure we don't fall too far behind wall-clock
        if (mfuBufferQueue.size() > MAX_QUEUE_SIZE) {
            Log.w(TAG, String.format("PushMfuByteBufferFragment: clearing queue, length: %d",
                    mfuBufferQueue.size()));
            mfuBufferQueue.clear();
        }

        if (isVideoSample(mfuByteBufferFragment)
                || isAudioSample(mfuByteBufferFragment)
                || isTextSample(mfuByteBufferFragment))
        {
            mfuBufferQueue.add(
                    Pair.create(mfuByteBufferFragment, mfuByteBufferFragment.myByteBuffer.duplicate())
            );
        }
    }

    public void pushAudioDecoderConfigurationRecord(MMTAudioDecoderConfigurationRecord mmtAudioDecoderConfigurationRecord) {
        //TODO: remove audioConfigurationMap.isEmpty()
        //if (audioConfigurationMap.isEmpty()) {
        //    audioConfigurationMap.put(mmtAudioDecoderConfigurationRecord, false);
        //}

        audioChannelCount = mmtAudioDecoderConfigurationRecord.channel_count;
        audioSampleRate = mmtAudioDecoderConfigurationRecord.sample_rate;
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
