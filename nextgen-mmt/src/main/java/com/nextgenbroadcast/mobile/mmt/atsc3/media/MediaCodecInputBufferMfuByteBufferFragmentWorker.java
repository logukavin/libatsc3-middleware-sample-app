package com.nextgenbroadcast.mobile.mmt.atsc3.media;

import android.media.MediaCodec;
import android.util.Log;

import org.ngbp.libatsc3.middleware.android.application.sync.mmt.MfuByteBufferFragment;
import org.ngbp.libatsc3.middleware.android.application.sync.mmt.MmtPacketIdContext;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class MediaCodecInputBufferMfuByteBufferFragmentWorker extends Thread {
    private final DecoderHandlerThread player;
    private final String type;

    private MmtPacketIdContext.MmtMfuStatistics mmtMfuStatistics;
    private MediaCodec mediaCodec;
    private LinkedBlockingDeque<MfuByteBufferFragment> mfuByteBufferQueue;
    private LinkedBlockingDeque<Integer> mediaCodecInputBufferQueue;

    private volatile boolean shouldRun = true;
    private volatile boolean isShutdown = false;

    public static boolean IsHardCodecFlushingFromAVPtsDiscontinuity = false;    //flush MediaCodec pipelines
    public static boolean IsResettingCodecFromDiscontinuity = false;            //flush MediaCodec pipelines

    public MediaCodecInputBufferMfuByteBufferFragmentWorker(DecoderHandlerThread player, String type, MmtPacketIdContext.MmtMfuStatistics mmtMfuStatistics, MediaCodec mediaCodec, LinkedBlockingDeque<Integer> mediaCodecInputBufferQueue, LinkedBlockingDeque<MfuByteBufferFragment> mfuByteBufferQueue) {
        this.player = player;
        this.type = type;
        this.mmtMfuStatistics = mmtMfuStatistics;
        this.mediaCodec = mediaCodec;
        this.mediaCodecInputBufferQueue = mediaCodecInputBufferQueue;
        this.mfuByteBufferQueue = mfuByteBufferQueue;
    }

    public void shutdown() {
        shouldRun = false;
        while (!isShutdown) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        mediaCodecInputBufferQueue.clear();
        mediaCodecInputBufferQueue = null;
        mfuByteBufferQueue.clear();
        mfuByteBufferQueue = null;

        mediaCodec = null;
        mmtMfuStatistics = null;
    }

    @Override
    public void run() {
        while (shouldRun) {
            Integer toProcessMediaCodecInputBufferIndex = null;
            MfuByteBufferFragment toProcessMfuByteBufferFragment = null;
            try {
                while (player.getMediaSource().IsSoftFlushingFromAVPtsDiscontinuity.get() || IsHardCodecFlushingFromAVPtsDiscontinuity || IsResettingCodecFromDiscontinuity) {
                    Thread.sleep(100);
                }

                while (shouldRun && (toProcessMediaCodecInputBufferIndex = mediaCodecInputBufferQueue.poll(100, TimeUnit.MILLISECONDS)) == null) {
                    Thread.sleep(1);
                }

                while (shouldRun && (toProcessMfuByteBufferFragment = mfuByteBufferQueue.poll(100, TimeUnit.MILLISECONDS)) == null) {
                    Thread.sleep(1);
                }

                if (!shouldRun) {
                    continue;
                }

                long computedPresentationTimestampUs = player.getMediaSource().getPresentationTimestampUs(toProcessMfuByteBufferFragment);

                Log.d("computePresentationTimestampUs", String.format("type: %s, mpu_sequence_number: %d, sample_number: %d, mpu_ts: %d, mfu_ts: %d, computedPtsUs: %d",
                        this.type,
                        toProcessMfuByteBufferFragment.mpu_sequence_number,
                        toProcessMfuByteBufferFragment.sample_number,
                        toProcessMfuByteBufferFragment.mpu_presentation_time_uS_from_SI,
                        toProcessMfuByteBufferFragment.mfu_presentation_time_uS_computed,
                        computedPresentationTimestampUs));

                //timestamp discontinuity > 1000000 (1s) or mpu_sequence_number in the past, flush mediaCodec to reset
                if ((mmtMfuStatistics.last_computedPresentationTimestampUs != null && computedPresentationTimestampUs < (mmtMfuStatistics.last_computedPresentationTimestampUs - 1000000)) ||
                        (mmtMfuStatistics.last_mpu_sequence_number_inputBufferQueued != null && mmtMfuStatistics.last_mpu_sequence_number_inputBufferQueued > toProcessMfuByteBufferFragment.mpu_sequence_number)) {

                    Log.d("MediaCodecInputBuffer", String.format("softFlushAVPtsDiscontinuity: Flushing, as last_computedPresentationTimestampUs: %d, computedPresentationTimestampUs: %d, last_mpu_sequence_number: %d, current_mpu_sequence_number: %d",
                            mmtMfuStatistics.last_computedPresentationTimestampUs, computedPresentationTimestampUs, mmtMfuStatistics.last_mpu_sequence_number_inputBufferQueued, toProcessMfuByteBufferFragment.mpu_sequence_number));

                    player.softFlushAVPtsDiscontinuity();
                    continue;
                }


                mmtMfuStatistics.last_computedPresentationTimestampUs = computedPresentationTimestampUs;
                mmtMfuStatistics.last_mpu_sequence_number_inputBufferQueued = toProcessMfuByteBufferFragment.mpu_sequence_number;

                if (MediaCodecInputBufferMfuByteBufferFragmentWorker.IsResettingCodecFromDiscontinuity) {
                    //skip writing this toProcessMfuByteBufferFragment
                    toProcessMfuByteBufferFragment.unreferenceByteBuffer();
                    continue;
                } else if (MediaCodecInputBufferMfuByteBufferFragmentWorker.IsHardCodecFlushingFromAVPtsDiscontinuity) {
                    //push front our input buffer index
                    mediaCodecInputBufferQueue.addFirst(toProcessMediaCodecInputBufferIndex);
                    toProcessMfuByteBufferFragment.unreferenceByteBuffer();
                    return;
                }

                MediaCodecInputBuffer.WriteToInputBufferFromMfuByteBufferFragment(mediaCodec, toProcessMediaCodecInputBufferIndex, toProcessMfuByteBufferFragment);
                MediaCodecInputBuffer.QueueInputBuffer(mediaCodec, toProcessMediaCodecInputBufferIndex, toProcessMfuByteBufferFragment, computedPresentationTimestampUs, mmtMfuStatistics);

                mmtMfuStatistics.decoder_buffer_queue_input_count++;

                onIteration(toProcessMfuByteBufferFragment, computedPresentationTimestampUs);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        isShutdown = true;
    }

    public void onIteration(MfuByteBufferFragment toProcessMfuByteBufferFragment, long computedPresentationTimestampUs) {
        // override for logging
    }
}