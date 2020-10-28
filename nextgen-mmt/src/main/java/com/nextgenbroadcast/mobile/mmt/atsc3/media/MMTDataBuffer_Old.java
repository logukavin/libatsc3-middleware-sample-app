package com.nextgenbroadcast.mobile.mmt.atsc3.media;

import android.media.MediaFormat;
import android.util.Log;

import org.ngbp.libatsc3.middleware.android.ATSC3PlayerFlags;
import org.ngbp.libatsc3.middleware.android.DebuggingFlags;
import org.ngbp.libatsc3.middleware.android.application.sync.mmt.MfuByteBufferFragment;
import org.ngbp.libatsc3.middleware.android.application.sync.mmt.MmtPacketIdContext;
import org.ngbp.libatsc3.middleware.android.application.sync.mmt.MpuMetadata_HEVC_NAL_Payload;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

public class MMTDataBuffer_Old {
    private final LinkedHashMap<Long, Long> MapVideoMfuPresentationTimestampUsAnchorSystemTimeUs = new LinkedHashMap<>();
    private final LinkedHashMap<Long, Long> MapAudioMfuPresentationTimestampUsAnchorSystemTimeUs = new LinkedHashMap<>();

    private MpuMetadata_HEVC_NAL_Payload InitMpuMetadata_HEVC_NAL_Payload = null;

    final LinkedBlockingDeque<MfuByteBufferFragment> mfuBufferQueueVideo = new LinkedBlockingDeque<>(); //Collections.synchronizedList(new ArrayList<MfuByteBufferFragment>());
    final LinkedBlockingDeque<MfuByteBufferFragment> mfuBufferQueueAudio = new LinkedBlockingDeque<>(); //Collections.synchronizedList(new ArrayList<MfuByteBufferFragment>());
    final LinkedBlockingDeque<MfuByteBufferFragment> mfuBufferQueueStpp = new LinkedBlockingDeque<>(); //Collections.synchronizedList(new ArrayList<MfuByteBufferFragment>());

    final AtomicBoolean IsSoftFlushingFromAVPtsDiscontinuity = new AtomicBoolean(false);    //only discard the next few inputBuffer and reset our anchors

    public MMTDataBuffer_Old() {
    }

    void release() {
        clearQueues();
        clearTimeCache();
    }

    void clearQueues() {
        mfuBufferQueueVideo.clear();
        mfuBufferQueueAudio.clear();
        mfuBufferQueueStpp.clear();
    }

    void clearTimeCache() {
        MapAudioMfuPresentationTimestampUsAnchorSystemTimeUs.clear();
        MapVideoMfuPresentationTimestampUsAnchorSystemTimeUs.clear();
    }

    public void InitMpuMetadata_HEVC_NAL_Payload(MpuMetadata_HEVC_NAL_Payload payload) {
        if (ATSC3PlayerFlags.ATSC3PlayerStartPlayback && InitMpuMetadata_HEVC_NAL_Payload == null) {
            InitMpuMetadata_HEVC_NAL_Payload = payload;
        } else {
            payload.releaseByteBuffer();
        }
    }

    public void PushMfuByteBufferFragment(MfuByteBufferFragment mfuByteBufferFragment) {
        if (!ATSC3PlayerFlags.ATSC3PlayerStartPlayback) {
            mfuByteBufferFragment.myByteBuffer = null;
            return;
        }

        //jjustman-2020-08-19 - hack-ish workaround for ac-4 and mmt_atsc3_message signalling information w/ sample duration (or avoiding parsing the trun box)
        if (MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us != 0 || MmtPacketIdContext.audio_packet_statistics.extracted_sample_duration_us == 0) {
            if(MmtPacketIdContext.audio_packet_id == mfuByteBufferFragment.packet_id && mfuByteBufferFragment.sample_number==1) {
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

        if (IsSoftFlushingFromAVPtsDiscontinuity.get()
                || MediaCodecInputBufferMfuByteBufferFragmentWorker.IsHardCodecFlushingFromAVPtsDiscontinuity
                || MediaCodecInputBufferMfuByteBufferFragmentWorker.IsResettingCodecFromDiscontinuity) {
            mfuByteBufferFragment.unreferenceByteBuffer();
            return;
        }

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
        mfuBufferQueueVideo.add(mfuByteBufferFragment);

        if (mfuByteBufferFragment.sample_number == 1) {
            if (!ATSC3PlayerFlags.FirstMfuBufferVideoKeyframeSent) {
                Log.d("pushMfuByteBufferFragment", String.format("V: pushing FIRST: queueSize: %d, sampleNumber: %d, size: %d, mpuPresentationTimeUs: %d",
                        mfuBufferQueueVideo.size(),
                        mfuByteBufferFragment.sample_number,
                        mfuByteBufferFragment.bytebuffer_length,
                        mfuByteBufferFragment.mpu_presentation_time_uS_from_SI));
            }
            ATSC3PlayerFlags.FirstMfuBufferVideoKeyframeSent = true;

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
                            mfuBufferQueueVideo.size()));
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

        mfuBufferQueueAudio.add(mfuByteBufferFragment);

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
                    mfuBufferQueueVideo.size()));
        }
    }

    private void addSubtitleFragment(MfuByteBufferFragment mfuByteBufferFragment) {
        if (!ATSC3PlayerFlags.FirstMfuBufferVideoKeyframeSent) {
            return;
        }

        mfuBufferQueueStpp.add(mfuByteBufferFragment);
        MmtPacketIdContext.stpp_packet_statistics.total_mfu_samples_count++;
    }

    void getMediaFormat(MediaFormat mediaFormat) {
        byte[] nal_check = new byte[8];
        InitMpuMetadata_HEVC_NAL_Payload.myByteBuffer.get(nal_check, 0, 8);
        Log.d("createMfuOuterMediaCodec", String.format("HEVC NAL is: 0x%02x 0x%02x 0x%02x 0x%02x 0x%02x 0x%02x 0x%02x 0x%02x, len: %d",
                nal_check[0], nal_check[1], nal_check[2], nal_check[3],
                nal_check[4], nal_check[5], nal_check[6], nal_check[7], InitMpuMetadata_HEVC_NAL_Payload.myByteBuffer.capacity()));

        InitMpuMetadata_HEVC_NAL_Payload.myByteBuffer.rewind();

        mediaFormat.setByteBuffer("csd-0", InitMpuMetadata_HEVC_NAL_Payload.myByteBuffer);
    }

    boolean hasMpuMetadata() {
        return InitMpuMetadata_HEVC_NAL_Payload != null;
    }

    long getPresentationTimestampUs(MfuByteBufferFragment toProcessMfuByteBufferFragment) {
        long ptsOffsetUs = 66000L;

        //by default for any missing MMT SI emissions or flash-cut into MMT flow emission, use now_Us + 66000uS for our presentationTimestampUs
        long computedPresentationTimestampUs = System.currentTimeMillis() * 1000 + ptsOffsetUs;

        if (toProcessMfuByteBufferFragment.mfu_presentation_time_uS_computed != null && toProcessMfuByteBufferFragment.mfu_presentation_time_uS_computed > 0) {
            //default values here as fallback
            long anchorMfuPresentationTimestampUs = toProcessMfuByteBufferFragment.mpu_presentation_time_uS_from_SI;
            long anchorSystemTimeUs = System.currentTimeMillis() * 1000;

            //todo: expand size as needed, every ~ mfu_presentation_time_uS_computed 1000000uS
            if (toProcessMfuByteBufferFragment.packet_id == MmtPacketIdContext.video_packet_id) {

                if (MapVideoMfuPresentationTimestampUsAnchorSystemTimeUs.size() == 0) {
                    MapVideoMfuPresentationTimestampUsAnchorSystemTimeUs.put(toProcessMfuByteBufferFragment.mfu_presentation_time_uS_computed, System.currentTimeMillis() * 1000);
                }
                for (Map.Entry<Long, Long> anchor : MapVideoMfuPresentationTimestampUsAnchorSystemTimeUs.entrySet()) {
                    anchorMfuPresentationTimestampUs = anchor.getKey();
                    anchorSystemTimeUs = anchor.getValue();
                }
            } else  if (toProcessMfuByteBufferFragment.packet_id == MmtPacketIdContext.audio_packet_id) {
                if (MapAudioMfuPresentationTimestampUsAnchorSystemTimeUs.size() == 0) {
                    MapAudioMfuPresentationTimestampUsAnchorSystemTimeUs.put(toProcessMfuByteBufferFragment.mfu_presentation_time_uS_computed, System.currentTimeMillis() * 1000);
                }
                for (Map.Entry<Long, Long> anchor : MapAudioMfuPresentationTimestampUsAnchorSystemTimeUs.entrySet()) {
                    anchorMfuPresentationTimestampUs = anchor.getKey();
                    anchorSystemTimeUs = anchor.getValue();
                }
            }

            long mpuPresentationTimestampDeltaUs = toProcessMfuByteBufferFragment.mfu_presentation_time_uS_computed - anchorMfuPresentationTimestampUs;
            computedPresentationTimestampUs = anchorSystemTimeUs + mpuPresentationTimestampDeltaUs + ptsOffsetUs;
        }

        return computedPresentationTimestampUs;
    }
}