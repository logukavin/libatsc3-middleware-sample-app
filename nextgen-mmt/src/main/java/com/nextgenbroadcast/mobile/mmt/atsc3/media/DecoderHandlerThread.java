package com.nextgenbroadcast.mobile.mmt.atsc3.media;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaSync;
import android.media.MediaTimestamp;
import android.media.PlaybackParams;
import android.media.SyncParams;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceView;

import androidx.annotation.NonNull;

import com.nextgenbroadcast.mobile.mmt.atsc3.ServiceHandler;

import org.jetbrains.annotations.NotNull;
import org.ngbp.libatsc3.middleware.android.ATSC3PlayerFlags;
import org.ngbp.libatsc3.middleware.android.DebuggingFlags;
import org.ngbp.libatsc3.middleware.android.application.sync.CodecAACSpecificData;
import org.ngbp.libatsc3.middleware.android.application.sync.mmt.MfuByteBufferFragment;
import org.ngbp.libatsc3.middleware.android.application.sync.mmt.MmtPacketIdContext;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.concurrent.LinkedBlockingDeque;

import static java.lang.System.nanoTime;

//TODO: rename to MMTPlayer
public class DecoderHandlerThread {
    private final Handler decoderHandler;
    private final Handler serviceHandler;

    private Surface myDecoderSurface;

    public static MediaSync sync;

    //todo: investigate int64_t MediaSync::getRealTime(int64_t mediaTimeUs, int64_t nowUs) {
    private Surface syncSurface;

    public static final int PENDING_NAL = 5;
    public static final int CREATE_CODEC = 6;
    public static final int DECODE = 7;
    public static final int DECODE_AV_TUNNEL = 8;
    public static final int DESTROY = -1;

    private LinkedBlockingDeque<Integer> mediaCodecInputBufferVideoQueue = new LinkedBlockingDeque<>();
    private LinkedBlockingDeque<Integer> mediaCodecInputBufferAudioQueue = new LinkedBlockingDeque<>();

    private MediaCodec videoCodec;
    private MediaCodec audioCodec;

    private MediaCodecInputBufferMfuByteBufferFragmentWorker videoInputFragmentRunnableThread;
    private MediaCodecInputBufferMfuByteBufferFragmentWorker audioInputFragmentRunnableThread;

    private volatile boolean invalidateSurface;

    private SurfaceView mSurfaceView1;

    public interface Listener {
        void onPlayerReady();
    }

    public DecoderHandlerThread(SurfaceView surfaceView, Listener listener) {
        mSurfaceView1 = surfaceView;

        HandlerThread handlerThread = new HandlerThread("decoderHandlerThread");
        handlerThread.start();

        decoderHandler = new DecoderHandler(handlerThread.getLooper(), this);
        serviceHandler = new ServiceHandler(Looper.getMainLooper(), listener);
    }

    public void createMediaCodec() {
        if (!ATSC3PlayerFlags.ATSC3PlayerStartPlayback) {
            MfuByteBufferHandler.clearQueues();
            decoderHandler.sendMessage(decoderHandler.obtainMessage(DecoderHandlerThread.CREATE_CODEC));
        }
    }

    public void destroyMediaCodec() {
        if (ATSC3PlayerFlags.ATSC3PlayerStartPlayback) {
            ATSC3PlayerFlags.ATSC3PlayerStartPlayback = false;
            ATSC3PlayerFlags.ATSC3PlayerStopPlayback = true;
            decoderHandler.sendMessage(decoderHandler.obtainMessage(DecoderHandlerThread.DESTROY));
        }
    }

    public void setOutputSurface(Surface surface) {
        myDecoderSurface = surface;
        Log.d("DecoderHandlerThread", String.format("setOutputSurface with surfaceHolder: %s, myDecoderSurface: %s", /*surfaceHolder*/null, myDecoderSurface));
    }

    public void clearOutputSurface() {
        Log.d("DecoderHandlerThread", "clearOutputSurface - clearing myDecoderSurface / surfaceHolder!");

        destroyCodec();
        this.myDecoderSurface = null;
    }

    public Size getVideoSize() {
        return new Size(MmtPacketIdContext.video_packet_statistics.width, MmtPacketIdContext.video_packet_statistics.height);
    }

    private void notifyPlayerReady() {
        serviceHandler.sendMessageDelayed(serviceHandler.obtainMessage(ServiceHandler.VIDEO_RESIZE), 1);
    }

    private void destroyCodec() {
        ATSC3PlayerFlags.ReentrantLock.lock();
        try {
            ATSC3PlayerFlags.ATSC3PlayerStopPlayback = true;
            ATSC3PlayerFlags.ATSC3PlayerStartPlayback = false;

            invalidateSurface = false;

            if (videoInputFragmentRunnableThread != null) {
                videoInputFragmentRunnableThread.shutdown();
                videoInputFragmentRunnableThread = null;
            }
            if (audioInputFragmentRunnableThread != null) {
                audioInputFragmentRunnableThread.shutdown();
                audioInputFragmentRunnableThread = null;
            }

            if (videoCodec != null) {
                videoCodec.release();
                videoCodec = null;
            }
            if (audioCodec != null) {
                audioCodec.release();
                audioCodec = null;
            }

            // must be destroyed after Codec
            if (sync != null) {
                sync.setPlaybackParams(new PlaybackParams().setSpeed(0.0f));
                //sync.flush();
                sync.release();
                Log.d("DecoderHandlerThread", String.format("destroyCodec - released sync"));
                sync = null;
            }

            if (syncSurface != null) {
                syncSurface.release();
                syncSurface = null;
            }

            ATSC3PlayerMMTFragments.mfuBufferQueueVideo.clear();
            ATSC3PlayerMMTFragments.mfuBufferQueueAudio.clear();

            ATSC3PlayerFlags.FirstMfuBufferVideoKeyframeSent = false;
            ATSC3PlayerFlags.FirstMfuBuffer_presentation_time_us_mpu = 0;
        } finally {
            ATSC3PlayerMMTFragments.InitMpuMetadata_HEVC_NAL_Payload = null;
            ATSC3PlayerFlags.FirstMfuBufferVideoKeyframeSent = false;
            ATSC3PlayerFlags.FirstMfuBuffer_presentation_time_us_mpu = 0;

            ATSC3PlayerFlags.ReentrantLock.unlock();
        }

    }

    private void createMfuOuterMediaCodec() throws IOException {
        ATSC3PlayerFlags.ReentrantLock.lock();

        try {
            ATSC3PlayerFlags.ATSC3PlayerStartPlayback = true;
            ATSC3PlayerFlags.ATSC3PlayerStopPlayback = false;
            ATSC3PlayerFlags.FirstMfuBufferVideoKeyframeSent = false;
            ATSC3PlayerFlags.FirstMfuBuffer_presentation_time_us_mpu = 0;

            //TODO: remove this...spinlock...
            while (!ATSC3PlayerFlags.ATSC3PlayerStopPlayback && ATSC3PlayerMMTFragments.InitMpuMetadata_HEVC_NAL_Payload == null) {
                Log.d("createMfuOuterMediaCodec", "waiting for initMpuMetadata_HEVC_NAL_Payload != null");
                try {
                    Thread.sleep(100);
                } catch (Exception ex) {
                    //
                }
            }
            //spin for at least one video and one audio frame
            while (!ATSC3PlayerFlags.ATSC3PlayerStopPlayback && (ATSC3PlayerMMTFragments.mfuBufferQueueVideo.peek() == null || ATSC3PlayerMMTFragments.mfuBufferQueueAudio.peek() == null)) {
                Log.d("createMfuOuterMediaCodec", String.format("waiting for mfuBufferQueueVideo, size: %d, mfuBufferQueueAudio, size: %d", ATSC3PlayerMMTFragments.mfuBufferQueueVideo.size(), ATSC3PlayerMMTFragments.mfuBufferQueueAudio.size()));
                try {
                    Thread.sleep(100);
                } catch (Exception ex) {
                    //
                }
            }

            //bail early
            if (ATSC3PlayerFlags.ATSC3PlayerStopPlayback) {
                return;
            }

            //TODO: use proper values from init box avcc/hvcc/mp4a/etc...
            String videoMimeType = "video/hevc";

            int audioSampleRate = 48000;
            int audioChannelCount = 2;
            String audioMimeType = "audio/mp4a-latm";

            sync = createMediaSync(audioSampleRate);
            if (sync == null) return;

            videoCodec = createVideoCodec(sync, videoMimeType);
            if (videoCodec == null) return;

            audioCodec = createAudioCodec(sync, audioMimeType, audioSampleRate, audioChannelCount);
            if (audioCodec == null) return;

            videoCodec.start();
            audioCodec.start();

            notifyPlayerReady();

            startSurfaceInvalidationThread();

            /*
            The client can optionally pre-fill audio/video buffers by setting playback rate to 0.0,
            and then feed audio/video buffers to corresponding components.

            This can reduce possible initial underrun.

            https://developer.android.com/reference/android/media/MediaSync
             */

            Log.d("createMfuOuterMediaCodec", String.format("codecInfo: %s, inputFormat: %s", videoCodec.getCodecInfo(), videoCodec.getInputFormat()));

            MediaFormat videoInputFormat = videoCodec.getInputFormat();
            MediaFormat audioInputFormat = audioCodec.getInputFormat();

            Log.d("createMfuOuterMediaCodec", String.format("video format: %s, audio format: %s", videoInputFormat.toString(), audioInputFormat.toString()));

            videoInputFragmentRunnableThread = new MediaCodecInputBufferMfuByteBufferFragmentWorker("video", MmtPacketIdContext.video_packet_statistics, videoCodec, mediaCodecInputBufferVideoQueue, ATSC3PlayerMMTFragments.mfuBufferQueueVideo) {
                @Override
                public void onIteration(MfuByteBufferFragment toProcessMfuByteBufferFragment, long computedPresentationTimestampUs) {
                    if (DebuggingFlags.MFU_STATS_RENDERING) {
                        Message msg = serviceHandler.obtainMessage(ServiceHandler.DRAW_TEXT_FRAME_VIDEO_ENQUEUE_US,
                                String.format(Locale.US, "V:onFrameEnque: mpu_seq_num: %d\n s: %d, fr: %d, enQ: %d\n ptsUs: %d",
                                        toProcessMfuByteBufferFragment.mpu_sequence_number,
                                        toProcessMfuByteBufferFragment.sample_number,
                                        MmtPacketIdContext.video_packet_statistics.decoder_buffer_queue_input_count,
                                        ATSC3PlayerMMTFragments.mfuBufferQueueVideo.size(),
                                        computedPresentationTimestampUs
                                )
                        );
                        serviceHandler.sendMessage(msg);
                    }
                }
            };
            videoInputFragmentRunnableThread.setName("videoMediaCodecInputMfuByteBufferThread");
            videoInputFragmentRunnableThread.start();

            audioInputFragmentRunnableThread = new MediaCodecInputBufferMfuByteBufferFragmentWorker("audio", MmtPacketIdContext.audio_packet_statistics, audioCodec, mediaCodecInputBufferAudioQueue, ATSC3PlayerMMTFragments.mfuBufferQueueAudio) {
                @Override
                public void onIteration(MfuByteBufferFragment toProcessMfuByteBufferFragment, long computedPresentationTimestampUs) {
                    if (DebuggingFlags.MFU_STATS_RENDERING) {
                        Message msg = serviceHandler.obtainMessage(ServiceHandler.DRAW_TEXT_FRAME_AUDIO_ENQUEUE_US,
                                String.format(Locale.US, "A:onFrameEnque: mpu_seq_num: %d\n s: %d, fr: %d, enQ: %d\n ptsUs: %d",
                                        toProcessMfuByteBufferFragment.mpu_sequence_number,
                                        toProcessMfuByteBufferFragment.sample_number,
                                        MmtPacketIdContext.audio_packet_statistics.decoder_buffer_queue_input_count,
                                        ATSC3PlayerMMTFragments.mfuBufferQueueAudio.size(),
                                        computedPresentationTimestampUs
                                )
                        );
                        serviceHandler.sendMessage(msg);
                    }
                }
            };
            audioInputFragmentRunnableThread.setName("audioMediaCodecInputMfuByteBufferThread");
            audioInputFragmentRunnableThread.start();
        } finally {
            ATSC3PlayerFlags.ReentrantLock.unlock();
        }
    }

    private MediaSync createMediaSync(int audioSampleRate) {
        final MediaSync sync;
        try {
            sync = new MediaSync();
            sync.setSurface(myDecoderSurface);
            syncSurface = sync.createInputSurface();
        } catch (Exception ex) {
            Log.d("createMfuOuterMediaCodec", String.format("exception when creating mediaSync - ex: %s", ex));
            //jjustman-2019-12-29 - TODO: failed to connect to surface with error -22
            return null;
        }

        int buffsize = AudioTrack.getMinBufferSize(audioSampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, audioSampleRate,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                buffsize,
                AudioTrack.MODE_STREAM);

        sync.setAudioTrack(audioTrack);

        HandlerThread syncThread = new HandlerThread("sync");
        syncThread.start();
        Handler syncHandler = new Handler(syncThread.getLooper()); //getMainLooper() //Looper.getMainLooper()
        //Handler syncHandler = new Handler(Looper.getMainLooper()); //getMainLooper() //Looper.getMainLooper()

        sync.setCallback(new MediaSync.Callback() {
            @Override
            public void onAudioBufferConsumed(@NonNull MediaSync mediaSync, @NonNull ByteBuffer byteBuffer, int i) {
                if (MediaCodecInputBufferMfuByteBufferFragmentWorker.IsSoftFlushingFromAVPtsDiscontinuity.get()) {
                    return;
                }
                try {
                    audioCodec.releaseOutputBuffer(i, false);

                    if (false) {
                        Log.d("MediaSync", "onAudioBufferConsumed i " + i);
                    }
                } catch (Exception ex) {
                    Log.w("MediaSync.callback", String.format("exception is: " + ex));
                }
            }
        }, syncHandler);

        sync.setOnErrorListener((mediaSync, i, i1) -> Log.d("MediaSync", "onError " + i + " " + i1), syncHandler);


        SyncParams syncParams = new SyncParams().allowDefaults();
        sync.setPlaybackParams(new PlaybackParams().setSpeed(1.0f));

        //float pcapTargetRational = (float)(MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us/1000000.0);
        float pcapTargetRational = (float) 1000000.0 / MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us;
        syncParams.setFrameRate(pcapTargetRational);
        syncParams.setTolerance(0.5f);


        //sync.setPlaybackParams(new PlaybackParams().setSpeed(0.0f));
        //syncParams.setSyncSource(SyncParams.SYNC_SOURCE_VSYNC); // backlog of vframes queueing up
        // syncParams.setSyncSource(SyncParams.SYNC_SOURCE_DEFAULT);
        //syncParams.setSyncSource(SyncParams.SYNC_SOURCE_SYSTEM_CLOCK); //jitter - lots of jitter
        syncParams.setSyncSource(SyncParams.SYNC_SOURCE_AUDIO); //video frames queue up?

        sync.setSyncParams(syncParams);

        return sync;
    }

    private MediaCodec createVideoCodec(MediaSync sync, String videoMimeType) {
        final MediaCodec codec;
        try {
            codec = MediaCodec.createDecoderByType(videoMimeType);
        } catch (Exception e) {
            e.printStackTrace();
            serviceHandler.dispatchMessage(serviceHandler.obtainMessage(ServiceHandler.TOAST, "Unable to instantiate V: " + videoMimeType + " decoder!"));
            return null;
        }

        MediaFormat videoMediaFormat;
        if (MmtPacketIdContext.video_packet_statistics.width > 0 && MmtPacketIdContext.video_packet_statistics.height > 0) {
            videoMediaFormat = MediaFormat.createVideoFormat(videoMimeType, MmtPacketIdContext.video_packet_statistics.width, MmtPacketIdContext.video_packet_statistics.height);
        } else {
            //fallback to a "safe" size
            videoMediaFormat = MediaFormat.createVideoFormat(videoMimeType, MmtPacketIdContext.MmtMfuStatistics.FALLBACK_WIDTH, MmtPacketIdContext.MmtMfuStatistics.FALLBACK_HEIGHT);
        }
        //mf.setFeatureEnabled(MediaCodecInfo.CodecCapabilities.FEATURE_TunneledPlayback, true);


        byte[] nal_check = new byte[8];
        ATSC3PlayerMMTFragments.InitMpuMetadata_HEVC_NAL_Payload.myByteBuffer.get(nal_check, 0, 8);
        Log.d("createMfuOuterMediaCodec", String.format("HEVC NAL is: 0x%02x 0x%02x 0x%02x 0x%02x 0x%02x 0x%02x 0x%02x 0x%02x, len: %d",
                nal_check[0], nal_check[1], nal_check[2], nal_check[3],
                nal_check[4], nal_check[5], nal_check[6], nal_check[7], ATSC3PlayerMMTFragments.InitMpuMetadata_HEVC_NAL_Payload.myByteBuffer.capacity()));

        ATSC3PlayerMMTFragments.InitMpuMetadata_HEVC_NAL_Payload.myByteBuffer.rewind();

        videoMediaFormat.setByteBuffer("csd-0", ATSC3PlayerMMTFragments.InitMpuMetadata_HEVC_NAL_Payload.myByteBuffer);
        //jjustman-2019-10-19 - for testing
        //videoMediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0);
        videoMediaFormat.setInteger("allow-frame-drop", 0);
        //videoMediaFormat.setInteger("allow-frame-drop", 1);

        HandlerThread videoCodecHandlerThread = new HandlerThread("videoCodecHandlerThread");
        videoCodecHandlerThread.start();
        Handler videoCodecHandler = new Handler(videoCodecHandlerThread.getLooper());
        //Handler videoCodecHandler = new Handler(Looper.getMainLooper());

        codec.setCallback(new MediaCodec.Callback() {
            @Override
            public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int i) {
                /*
                after flush:
                    we only have to ignore outputBuffers, not inputBuffers
                    if(MediaCodecInputBufferMfuByteBufferFragmentWorkerRunnable.IsSoftFlushingFromAVPtsDiscontinuity) {
                     return;
                    }
                */
                mediaCodecInputBufferVideoQueue.add(i);
                Log.d("Video", String.format("onInputBufferAvailable: %d", i));
            }

            @Override
            public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int i, @NonNull MediaCodec.BufferInfo bufferInfo) {
                if (MediaCodecInputBufferMfuByteBufferFragmentWorker.IsSoftFlushingFromAVPtsDiscontinuity.get()) {
                    return;
                }
                MediaTimestamp mediaTimestamp = sync.getTimestamp();
                if (mediaTimestamp != null) {
                    Log.d("\tVideo", String.format("\tmediaTimestamp\tbufPts deltaMS:\t%f\tdelta anchorMedia-anchorSysMS:\t%f\tanchorMedia:\t%d\tanchorSystem:\t%d\trate:\t%f",
                            (bufferInfo.presentationTimeUs * 1000 - mediaTimestamp.getAnchorSytemNanoTime()) / 1000000.0, ((mediaTimestamp.getAnchorMediaTimeUs() * 1000) - mediaTimestamp.getAnchorSytemNanoTime()) / 1000000.0, mediaTimestamp.getAnchorMediaTimeUs() * 1000, mediaTimestamp.getAnchorSytemNanoTime(), mediaTimestamp.getMediaClockRate()));

                }
                if (0 != (MediaCodec.BUFFER_FLAG_END_OF_STREAM & bufferInfo.flags)) {
                    Log.d("Video", "onOutputBufferAvailable BUFFER_FLAG_END_OF_STREAM");
                    // return;
                }
                try {
                    long nanoTime = nanoTime();
                    //remap from bufferInfo.presentationTimeUs
                    long deltaNanoTime = ((33 + bufferInfo.presentationTimeUs) * 1000) - nanoTime;

                    if (true || DebuggingFlags.OutputBuferLoggingEnabled) {
                        Log.e("\tVideo\tonOutputBufferAvailable", String.format("\tbufferInfoNs:\t%d\tbufferInfoMS:\t%d\tnanoTime:\t%d\tnanoTimeMS:\t%f\tdeltaNanoTime\t%d\tdeltaNanoTimeMS\t%f",
                                bufferInfo.presentationTimeUs * 1000,
                                bufferInfo.presentationTimeUs / 1000,
                                nanoTime,
                                nanoTime / 1000000.0,
                                deltaNanoTime,
                                deltaNanoTime / 1000000.0));
                    }

//                    if(deltaNanoTime > 33000) {
//                        long toSleepMs = (deltaNanoTime - 33000) / 1000000;
//                        Log.e("Video", String.format("toSleepMs: %d", toSleepMs));
//
//                        Thread.sleep(toSleepMs);
//                    } else {
//                        Log.e("Video", String.format("negative deltaNanoTime: %d", deltaNanoTime));
//
//                    }

                    //TODO: update this
                    //mediaCodec.releaseOutputBuffer(i, bufferInfo.presentationTimeUs * 1000);
                    mediaCodec.releaseOutputBuffer(i, true);

                } catch (IllegalStateException ise) {
                    Log.e("\tVideo\tonOutputBufferAvailable", String.format("illegal state exception with idx: %d, and pts ns: %d", i, bufferInfo.presentationTimeUs * 1000));
                    //throw ise;
                }
//                catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
                String videoReleaseDeltaTString = "";
                //TODO: fix me
                if (MmtPacketIdContext.video_packet_statistics.last_mfu_release_microseconds.get(i) != null) {
                    Long videoReleaseDeltaT = bufferInfo.presentationTimeUs - MmtPacketIdContext.video_packet_statistics.last_mfu_release_microseconds.get(i);

                    // videoReleaseDeltaTString = "\n deltaT release "+videoReleaseDeltaT+"\n instant FPS: "+(videoReleaseDeltaT != 0 ? 1000000 / videoReleaseDeltaT: 0);

                }
                MmtPacketIdContext.video_packet_statistics.last_output_buffer_presentationTimeUs = bufferInfo.presentationTimeUs;
                if (DebuggingFlags.MFU_STATS_RENDERING) {
                    Message msg = serviceHandler.obtainMessage(ServiceHandler.DRAW_TEXT_FRAME_VIDEO_RELEASE_RENDERER, "onOutputBuffer.release ms:  " + (bufferInfo.presentationTimeUs) + " " + videoReleaseDeltaTString);
                    serviceHandler.sendMessage(msg);
                }
                MmtPacketIdContext.video_packet_statistics.last_mfu_release_microseconds.put(i, bufferInfo.presentationTimeUs);
                //jjustman-2019-10-20: imsc1 queue iteration
                MfuByteBufferFragment imscFragment = null;
                if ((imscFragment = ATSC3PlayerMMTFragments.mfuBufferQueueStpp.poll()) != null) {
                    serviceHandler.sendMessage(serviceHandler.obtainMessage(ServiceHandler.STPP_IMSC1_AVAILABLE, imscFragment));
                }
            }

            @Override
            public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {
                Log.d("Video", "onError");
                e.printStackTrace();
            }

            @Override
            public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec, @NonNull MediaFormat mediaFormat) {
                Log.d("Video", "onOutputFormatChanged");
            }
        }, videoCodecHandler);

        codec.configure(videoMediaFormat, syncSurface, null, 0);

        return codec;
    }

    private MediaCodec createAudioCodec(MediaSync sync, String audioMimeType, int audioSampleRate, int audioChannelCount) {
        MediaFormat audioMediaFormat = CodecAACSpecificData.BuildAACCodecSpecificData(MediaCodecInfo.CodecProfileLevel.AACObjectLC, audioSampleRate, audioChannelCount);

        //jjustman-2020-08-19 - hack-ish for AC-4 audio testing on samsung S10+ with mmt
        if (true) {
            audioMimeType = "audio/ac4";
            audioMediaFormat = new MediaFormat();
            audioMediaFormat.setString(MediaFormat.KEY_MIME, audioMimeType);
            audioMediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2);
            audioMediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, audioSampleRate);
            //mediaFormat.setInteger("ac4-is-sync", 1); ?
            //audioMediaFormat.setInteger("ac4-is-sync", 0);
        }

        final MediaCodec codec;
        try {
            codec = MediaCodec.createDecoderByType(audioMimeType);
        } catch (Exception e) {
            e.printStackTrace();
            serviceHandler.dispatchMessage(serviceHandler.obtainMessage(ServiceHandler.TOAST, "Unable to instantiate A: " + audioMimeType + " decoder!"));
            return null;
        }

        HandlerThread audioCodecHandlerThread = new HandlerThread("audioCodecHandlerThread");
        audioCodecHandlerThread.start();
        Handler audioCodecHandler = new Handler(audioCodecHandlerThread.getLooper());
        //Handler audioCodecHandler = new Handler(Looper.getMainLooper());//getMainLooper()

        codec.setCallback(new MediaCodec.Callback() {
            @Override
            public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int i) {
                mediaCodecInputBufferAudioQueue.add(i);
                Log.d("Audio", String.format("onInputBufferAvailable: %d", i));
            }

            @Override
            public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int i, @NonNull MediaCodec.BufferInfo bufferInfo) {

                if (MediaCodecInputBufferMfuByteBufferFragmentWorker.IsSoftFlushingFromAVPtsDiscontinuity.get()) {
                    return;
                }
                MediaTimestamp mediaTimestamp = sync.getTimestamp();
                if (mediaTimestamp != null) {
                    //Log.d("\tAudio", String.format("\tmediaTimestamp\tbufPts deltaMS:\t%f\tdelta anchorMedia-anchorSysMS:\t%f\tanchorMedia:\t%d\tanchorSystem:\t%d\trate:\t%f", (bufferInfo.presentationTimeUs*1000 - mediaTimestamp.getAnchorSytemNanoTime())/1000000.0, ((mediaTimestamp.getAnchorMediaTimeUs()*1000) -  mediaTimestamp.getAnchorSytemNanoTime())/1000000.0, mediaTimestamp.getAnchorMediaTimeUs()*1000, mediaTimestamp.getAnchorSytemNanoTime(), mediaTimestamp.getMediaClockRate()));

                }

                try {
                    long nanoTime = nanoTime();
                    //remap from bufferInfo.presentationTimeUs
                    long deltaNanoTime = ((33 + bufferInfo.presentationTimeUs) * 1000) - nanoTime;

                    if (true || DebuggingFlags.OutputBuferLoggingEnabled) {
                        Log.e("\tAudio\tonOutputBufferAvailable", String.format("\tbufferInfoNs:\t%d\tbufferInfoMS:\t%d\tnanoTime:\t%d\tnanoTimeMS:\t%f\tdeltaNanoTime\t%d\tdeltaNanoTimeMS\t%f",
                                bufferInfo.presentationTimeUs * 1000,
                                bufferInfo.presentationTimeUs / 1000,
                                nanoTime,
                                nanoTime / 1000000.0,
                                deltaNanoTime,
                                deltaNanoTime / 1000000.0));
                    }
                } catch (Exception ex) {
                    //
                }

                ByteBuffer decoderBuffer = codec.getOutputBuffer(i);
                sync.queueAudio(decoderBuffer, i, bufferInfo.presentationTimeUs);

                MmtPacketIdContext.audio_packet_statistics.last_output_buffer_presentationTimeUs = bufferInfo.presentationTimeUs;
                if (DebuggingFlags.MFU_STATS_RENDERING) {
                    Message msg = serviceHandler.obtainMessage(ServiceHandler.DRAW_TEXT_FRAME_AUDIO_RELEASE_RENDERER,
                            String.format(Locale.US, "onOutputBuffer.release ms:\n %d", bufferInfo.presentationTimeUs));
                    serviceHandler.sendMessage(msg);
                }
            }

            @Override
            public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {
                Log.d("Audio", "onError");
                e.printStackTrace();
            }

            @Override
            public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec, @NonNull MediaFormat mediaFormat) {
                Log.d("Audio", "onOutputFormatChanged");
            }
        }, audioCodecHandler);

        codec.configure(audioMediaFormat, null, null, 0);
        return codec;
    }

    private void startSurfaceInvalidationThread() {
        Runnable runnable = () -> mSurfaceView1.invalidate();

        Thread forcedInvalidate = new Thread(() -> {
            while (invalidateSurface) {
                serviceHandler.post(runnable);

                try {
                    Thread.sleep(33);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        });

        invalidateSurface = true;
        forcedInvalidate.start();
    }

    private static class DecoderHandler extends Handler {
        private final WeakReference<DecoderHandlerThread> decoderRef;

        public DecoderHandler(@NonNull Looper looper, DecoderHandlerThread decoder) {
            super(looper);
            decoderRef = new WeakReference<>(decoder);
        }

        @Override
        public void handleMessage(@NotNull Message msg) {
            // process incoming messages here
            // this will run in non-ui/background thread

            DecoderHandlerThread decoder = decoderRef.get();
            if (decoder == null) return;

            switch (msg.what) {
                case CREATE_CODEC:
                    if (!ATSC3PlayerFlags.ATSC3PlayerStartPlayback) {
                        Log.d("DecoderHandlerThread", "CREATE_CODEC: mfuBufferQueueVideo size: " + ATSC3PlayerMMTFragments.mfuBufferQueueVideo.size() + "mfuBufferQueueAudio size: " + ATSC3PlayerMMTFragments.mfuBufferQueueAudio.size());

                        try {
                            decoder.createMfuOuterMediaCodec();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        // decoderHandler.sendMessage(decoderHandler.obtainMessage(DECODE, "codecCreated"));
                    }
                    break;

                case DESTROY:
                    decoder.destroyCodec();
                    break;

                default:
                    Log.d("DecoderHandlerThread", String.format("handleMessage: unknown: %d", msg.what));
                    break;
            }
        }
    }
}