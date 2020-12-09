package com.nextgenbroadcast.mobile.mmt.atsc3.media;

import android.util.Log;

import com.nextgenbroadcast.mobile.core.media.IMMTDataConsumer;

import org.ngbp.libatsc3.middleware.android.DebuggingFlags;
import org.ngbp.libatsc3.middleware.android.mmt.MfuByteBufferFragment;
import org.ngbp.libatsc3.middleware.android.mmt.MmtPacketIdContext;
import org.ngbp.libatsc3.middleware.android.mmt.MpuMetadata_HEVC_NAL_Payload;

import java.nio.ByteBuffer;
import java.util.ConcurrentModificationException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class MMTDataBuffer implements IMMTDataConsumer<MpuMetadata_HEVC_NAL_Payload, MfuByteBufferFragment> {
    private final LinkedBlockingDeque<MfuByteBufferFragment> mfuBufferQueue = new LinkedBlockingDeque<>();

    private long videoMfuPresentationTimestampUs;
    private long audioMfuPresentationTimestampUs;

    private MpuMetadata_HEVC_NAL_Payload InitMpuMetadata_HEVC_NAL_Payload = null;

    private boolean FirstMfuBufferVideoKeyframeSent = false;
    private volatile boolean isActive = false;
    private volatile boolean isReleased = false;

    public MMTDataBuffer() {
    }

    public MfuByteBufferFragment poll(long timeoutMs) throws InterruptedException {
        return mfuBufferQueue.poll(timeoutMs, TimeUnit.MILLISECONDS);
    }

    public int getVideoWidth() {
        return MmtPacketIdContext.video_packet_statistics.width > 0 ? MmtPacketIdContext.video_packet_statistics.width : MmtPacketIdContext.MmtMfuStatistics.FALLBACK_WIDTH;
    }

    public int getVideoHeight() {
        return MmtPacketIdContext.video_packet_statistics.height > 0 ? MmtPacketIdContext.video_packet_statistics.height : MmtPacketIdContext.MmtMfuStatistics.FALLBACK_HEIGHT;
    }

    public float getVideoFrameRate() {
        return (float) 1000000.0 / MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us;
    }

    public boolean isVideoSample(MfuByteBufferFragment sample) {
        return MmtPacketIdContext.video_packet_id == sample.packet_id;
    }

    public int getAudioChannelCount() {
        return 2;
    }

    public int getAudioSampleRate() {
        return 48000;
    }

    public boolean isAudioSample(MfuByteBufferFragment sample) {
        return MmtPacketIdContext.audio_packet_id == sample.packet_id;
    }

    public boolean isTextSample(MfuByteBufferFragment sample) {
        return MmtPacketIdContext.stpp_packet_id == sample.packet_id;
    }

    public boolean isKeySample(MfuByteBufferFragment fragment) {
        return fragment.sample_number == 1;
    }

    public int getMpuMetadataSize() {
        if (InitMpuMetadata_HEVC_NAL_Payload == null) return 0;

        ByteBuffer initBuffer = InitMpuMetadata_HEVC_NAL_Payload.myByteBuffer;
        initBuffer.rewind();
        return initBuffer.remaining();
    }

    public int readMpuMetadata(byte[] buffer, int offset, int length) {
        if (InitMpuMetadata_HEVC_NAL_Payload == null) return 0;

        ByteBuffer initBuffer = InitMpuMetadata_HEVC_NAL_Payload.myByteBuffer;
        int bytesToRead = Math.min(initBuffer.remaining(), length);
        initBuffer.get(buffer, offset, bytesToRead);
        return bytesToRead;
    }

    @Override
    public synchronized void open() throws ConcurrentModificationException {
        if (isActive) {
            try {
                wait(1000);
            } catch (InterruptedException e) {
                throw new ConcurrentModificationException("Buffer is occupied by another object");
            }
        }

        if (isReleased) throw new IllegalStateException("Buffer was released");

        isActive = true;
        FirstMfuBufferVideoKeyframeSent = false;
    }

    @Override
    public synchronized void close() {
        isActive = false;

        mfuBufferQueue.clear();

        videoMfuPresentationTimestampUs = 0;
        audioMfuPresentationTimestampUs = 0;

        notify();
    }

    @Override
    public void release() {
        isReleased = true;

        close();
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    @Override
    public void InitMpuMetadata_HEVC_NAL_Payload(MpuMetadata_HEVC_NAL_Payload payload) {
        if (isActive && InitMpuMetadata_HEVC_NAL_Payload == null) {
            InitMpuMetadata_HEVC_NAL_Payload = payload;
        } else {
            payload.releaseByteBuffer();
        }
    }

    @Override
    public void PushMfuByteBufferFragment(MfuByteBufferFragment mfuByteBufferFragment) {
        if (!isActive) {
            mfuByteBufferFragment.unreferenceByteBuffer();
            return;
        }

        //jjustman-2020-08-19 - hack-ish workaround for ac-4 and mmt_atsc3_message signalling information w/ sample duration (or avoiding parsing the trun box)
        if (MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us != 0 || MmtPacketIdContext.audio_packet_statistics.extracted_sample_duration_us == 0) {
            if (MmtPacketIdContext.audio_packet_id == mfuByteBufferFragment.packet_id && isKeySample(mfuByteBufferFragment)) {
                Log.d("PushMfuByteBufferFragment:INFO", String.format(" packet_id: %d, mpu_sequence_number: %d, setting audio_packet_statistics.extracted_sample_duration_us to follow video: %d * 2",
                        mfuByteBufferFragment.packet_id, mfuByteBufferFragment.mpu_sequence_number, MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us));
            }
            MmtPacketIdContext.audio_packet_statistics.extracted_sample_duration_us = MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us * 2;
        } else if (MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us == 0 || MmtPacketIdContext.audio_packet_statistics.extracted_sample_duration_us == 0) {
            Log.d("PushMfuByteBufferFragment:WARN", String.format(" packet_id: %d, mpu_sequence_number: %d, video.duration_us: %d, audio.duration_us: %d, missing extracted_sample_duration",
                    mfuByteBufferFragment.packet_id, mfuByteBufferFragment.mpu_sequence_number,
                    MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us,
                    MmtPacketIdContext.audio_packet_statistics.extracted_sample_duration_us));
        }

//        if (IsSoftFlushingFromAVPtsDiscontinuity.get()
//                || MediaCodecInputBufferMfuByteBufferFragmentWorker.IsHardCodecFlushingFromAVPtsDiscontinuity
//                || MediaCodecInputBufferMfuByteBufferFragmentWorker.IsResettingCodecFromDiscontinuity) {
//            mfuByteBufferFragment.unreferenceByteBuffer();
//            return;
//        }

        if (MmtPacketIdContext.video_packet_id == mfuByteBufferFragment.packet_id) {
            addVideoFragment(mfuByteBufferFragment);
        } else if (MmtPacketIdContext.audio_packet_id == mfuByteBufferFragment.packet_id) {
            addAudioFragment(mfuByteBufferFragment);
        } else if (MmtPacketIdContext.stpp_packet_id == mfuByteBufferFragment.packet_id) {
            addSubtitleFragment(mfuByteBufferFragment);
        }
    }

    private void addVideoFragment(MfuByteBufferFragment mfuByteBufferFragment) {
        // if(mfuByteBufferFragment.sample_number == 1 || firstMfuBufferVideoKeyframeSent) {
        //normal flow...
        mfuBufferQueue.add(mfuByteBufferFragment);

        if (isKeySample(mfuByteBufferFragment)) {
            if (!FirstMfuBufferVideoKeyframeSent) {
                Log.d("pushMfuByteBufferFragment", String.format("V: pushing FIRST: queueSize: %d, sampleNumber: %d, size: %d, mpuPresentationTimeUs: %d",
                        mfuBufferQueue.size(),
                        mfuByteBufferFragment.sample_number,
                        mfuByteBufferFragment.bytebuffer_length,
                        mfuByteBufferFragment.mpu_presentation_time_uS_from_SI));
            }
            FirstMfuBufferVideoKeyframeSent = true;

            MmtPacketIdContext.video_packet_statistics.video_mfu_i_frame_count++;
        } else {
            MmtPacketIdContext.video_packet_statistics.video_mfu_pb_frame_count++;
        }

        if (mfuByteBufferFragment.mfu_fragment_count_expected == mfuByteBufferFragment.mfu_fragment_count_rebuilt) {
            MmtPacketIdContext.video_packet_statistics.complete_mfu_samples_count++;
        } else {
            MmtPacketIdContext.video_packet_statistics.corrupt_mfu_samples_count++;
        }

        //TODO: jjustman-2019-10-23: manual missing statistics, context callback doesn't compute this properly yet.
        if (MmtPacketIdContext.video_packet_statistics.last_mpu_sequence_number != mfuByteBufferFragment.mpu_sequence_number) {
            MmtPacketIdContext.video_packet_statistics.total_mpu_count++;
            //compute trailing mfu's missing

            //compute leading mfu's missing
            if (mfuByteBufferFragment.sample_number > 1) {
                MmtPacketIdContext.video_packet_statistics.missing_mfu_samples_count += (mfuByteBufferFragment.sample_number - 1);
            }
        } else {
            MmtPacketIdContext.video_packet_statistics.missing_mfu_samples_count += mfuByteBufferFragment.sample_number - (1 + MmtPacketIdContext.video_packet_statistics.last_mfu_sample_number);
        }

        MmtPacketIdContext.video_packet_statistics.last_mfu_sample_number = mfuByteBufferFragment.sample_number;
        MmtPacketIdContext.video_packet_statistics.last_mpu_sequence_number = mfuByteBufferFragment.mpu_sequence_number;

        //todo - build mpu stats from tail of mfuBufferQueueVideo

        MmtPacketIdContext.video_packet_statistics.total_mfu_samples_count++;

        if ((MmtPacketIdContext.video_packet_statistics.total_mfu_samples_count % DebuggingFlags.DEBUG_LOG_MFU_STATS_FRAME_COUNT) == 0) {
            Log.d("pushMfuByteBufferFragment",
                    String.format("V: appending MFU: mpu_sequence_number: %d, sampleNumber: %d, size: %d, mpuPresentationTimeUs: %d, queueSize: %d",
                            mfuByteBufferFragment.mpu_sequence_number,
                            mfuByteBufferFragment.sample_number,
                            mfuByteBufferFragment.bytebuffer_length,
                            mfuByteBufferFragment.get_safe_mfu_presentation_time_uS_computed(),
                            mfuBufferQueue.size()));
        }
//            } else {
//                //discard
////                Log.d("pushMfuByteBufferFragment", "discarding video: firstMfuBufferVideoKeyframeSent: "+firstMfuBufferVideoKeyframeSent
////                        +", sampleNumber: "+mfuByteBufferFragment.sample_number
////                        +", size: "+mfuByteBufferFragment.bytebuffer_length
////                        +", presentationTimeUs: "+mfuByteBufferFragment.mpu_presentation_time_us_mpu);
//                mfuByteBufferFragment.myByteBuffer = null;
//            }
    }

    private void addAudioFragment(MfuByteBufferFragment mfuByteBufferFragment) {
//            if(!firstMfuBufferVideoKeyframeSent) {
//                //discard
//                return;
//            }

        mfuBufferQueue.add(mfuByteBufferFragment);

        if (mfuByteBufferFragment.mfu_fragment_count_expected == mfuByteBufferFragment.mfu_fragment_count_rebuilt) {
            MmtPacketIdContext.audio_packet_statistics.complete_mfu_samples_count++;
        } else {
            MmtPacketIdContext.audio_packet_statistics.corrupt_mfu_samples_count++;
        }

        //todo - build mpu stats from tail of mfuBufferQueueVideo

        MmtPacketIdContext.audio_packet_statistics.total_mfu_samples_count++;

        if (MmtPacketIdContext.audio_packet_statistics.last_mpu_sequence_number != mfuByteBufferFragment.mpu_sequence_number) {
            MmtPacketIdContext.audio_packet_statistics.total_mpu_count++;
            //compute trailing mfu's missing

            //compute leading mfu's missing
            if (mfuByteBufferFragment.sample_number > 1) {
                MmtPacketIdContext.audio_packet_statistics.missing_mfu_samples_count += (mfuByteBufferFragment.sample_number - 1);
            }
        } else {
            MmtPacketIdContext.audio_packet_statistics.missing_mfu_samples_count += mfuByteBufferFragment.sample_number - (1 + MmtPacketIdContext.audio_packet_statistics.last_mfu_sample_number);
        }

        MmtPacketIdContext.audio_packet_statistics.last_mfu_sample_number = mfuByteBufferFragment.sample_number;
        MmtPacketIdContext.audio_packet_statistics.last_mpu_sequence_number = mfuByteBufferFragment.mpu_sequence_number;


        if ((MmtPacketIdContext.audio_packet_statistics.total_mfu_samples_count % DebuggingFlags.DEBUG_LOG_MFU_STATS_FRAME_COUNT) == 0) {

            Log.d("pushMfuByteBufferFragment", String.format("A: appending MFU: mpu_sequence_number: %d, sampleNumber: %d, size: %d, mpuPresentationTimeUs: %d, queueSize: %d",
                    mfuByteBufferFragment.mpu_sequence_number,
                    mfuByteBufferFragment.sample_number,
                    mfuByteBufferFragment.bytebuffer_length,
                    mfuByteBufferFragment.get_safe_mfu_presentation_time_uS_computed(),
                    mfuBufferQueue.size()));
        }
    }

    private void addSubtitleFragment(MfuByteBufferFragment mfuByteBufferFragment) {
        if (!FirstMfuBufferVideoKeyframeSent) {
            return;
        }

        mfuBufferQueue.add(mfuByteBufferFragment);
        MmtPacketIdContext.stpp_packet_statistics.total_mfu_samples_count++;
    }

    public boolean hasMpuMetadata() {
        return InitMpuMetadata_HEVC_NAL_Payload != null;
    }

    public boolean skipUntilKeyFrame() {
        MfuByteBufferFragment fragment;
        while ((fragment = mfuBufferQueue.peek()) != null) {
            if (isKeySample(fragment) && !isTextSample(fragment)) {
                return true;
            } else {
                fragment.unreferenceByteBuffer();
                mfuBufferQueue.remove();
            }
        }

        return false;
    }

    public long ptsOffsetUs() {
        return 66000L;
    }

    public long getPresentationTimestampUs(MfuByteBufferFragment toProcessMfuByteBufferFragment) {
        if (toProcessMfuByteBufferFragment.mfu_presentation_time_uS_computed != null && toProcessMfuByteBufferFragment.mfu_presentation_time_uS_computed > 0) {
            //default values here as fallback
            long anchorMfuPresentationTimestampUs = toProcessMfuByteBufferFragment.mpu_presentation_time_uS_from_SI;

            //todo: expand size as needed, every ~ mfu_presentation_time_uS_computed 1000000uS
            if (toProcessMfuByteBufferFragment.packet_id == MmtPacketIdContext.video_packet_id) {
                if (videoMfuPresentationTimestampUs == 0) {
                    videoMfuPresentationTimestampUs = toProcessMfuByteBufferFragment.mfu_presentation_time_uS_computed;
                }
                anchorMfuPresentationTimestampUs = videoMfuPresentationTimestampUs;
            } else if (toProcessMfuByteBufferFragment.packet_id == MmtPacketIdContext.audio_packet_id) {
                if (audioMfuPresentationTimestampUs == 0) {
                    audioMfuPresentationTimestampUs = toProcessMfuByteBufferFragment.mfu_presentation_time_uS_computed;
                }
                anchorMfuPresentationTimestampUs = audioMfuPresentationTimestampUs;
            }

            long mpuPresentationTimestampDeltaUs = toProcessMfuByteBufferFragment.mfu_presentation_time_uS_computed - anchorMfuPresentationTimestampUs;
            return mpuPresentationTimestampDeltaUs + ptsOffsetUs();
        }

        return 0;
    }
}
