package com.nextgenbroadcast.mobile.mmt.exoplayer2;

import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ParserException;
import com.google.android.exoplayer2.extractor.Extractor;
import com.google.android.exoplayer2.extractor.ExtractorInput;
import com.google.android.exoplayer2.extractor.ExtractorOutput;
import com.google.android.exoplayer2.extractor.PositionHolder;
import com.google.android.exoplayer2.extractor.SeekMap;
import com.google.android.exoplayer2.extractor.TrackOutput;
import com.google.android.exoplayer2.util.ParsableByteArray;
import com.google.android.exoplayer2.util.Util;

import org.ngbp.libatsc3.middleware.Atsc3NdkMediaMMTBridge;
import org.ngbp.libatsc3.middleware.android.ATSC3PlayerFlags;
import org.ngbp.libatsc3.middleware.android.DebuggingFlags;
import org.ngbp.libatsc3.middleware.android.application.interfaces.IAtsc3NdkMediaMMTBridgeCallbacks;
import org.ngbp.libatsc3.middleware.android.mmt.MfuByteBufferFragment;
import org.ngbp.libatsc3.middleware.android.mmt.MmtPacketIdContext;
import org.ngbp.libatsc3.middleware.android.mmt.MpuMetadata_HEVC_NAL_Payload;
import org.ngbp.libatsc3.middleware.android.mmt.models.MMTAudioDecoderConfigurationRecord;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;

public class MMTExtractor implements Extractor, IAtsc3NdkMediaMMTBridgeCallbacks {
    public final static String TAG = MMTExtractor.class.getSimpleName();

    public static final long PTS_OFFSET_US = 66000L;

    public static final int IP_HEADER_SIZE = 20;
    public static final int UDP_PACKET_SIZE = 3000;

    private static final int IPVERSION = 4;
    private static final int IPPROTO_UDP = 17;

    private final Atsc3NdkMediaMMTBridge atsc3NdkMediaMMTBridge = new Atsc3NdkMediaMMTBridge(this);

    private ExtractorOutput extractorOutput;

    private boolean hasOutputFormat;
    private boolean hasOutputSeekMap;

    private final ByteBuffer udpPacketBuffer = ByteBuffer.allocateDirect(UDP_PACKET_SIZE);
    private final ParsableByteArray currentSampleBuffer = new ParsableByteArray();
    private final SparseArray<MmtTrack> tracks = new SparseArray<>();

    private final LinkedBlockingDeque<MfuByteBufferFragment> mfuBufferQueue = new LinkedBlockingDeque<>();
    private final ArrayMap<MMTAudioDecoderConfigurationRecord, Boolean> audioConfigurationMap = new ArrayMap<>();
    private MpuMetadata_HEVC_NAL_Payload initMpuMetadata_HEVC_NAL_Payload = null;
    private boolean firstMfuBufferVideoKeyframeSent = false;
    private long videoMfuPresentationTimestampUs;
    private long audioMfuPresentationTimestampUs;

    public MMTExtractor() {
    }

    @Override
    public boolean sniff(ExtractorInput input) {
        return true;
    }

    @Override
    public void init(ExtractorOutput output) {
        extractorOutput = output;
    }

    @Override
    public int read(ExtractorInput input, PositionHolder seekPosition) throws IOException, InterruptedException {
        if (input.getPosition() == 0) {
            if (!readMMTInitialPayload(input)) {
                throw new ParserException("Could not find MMT header.");
            }
        }
        maybeOutputFormat();
        int sampleReadResult = readSample(input);
        maybeOutputSeekMap();
        return sampleReadResult;
    }

    @Override
    public void seek(long position, long timeUs) {
    }

    @Override
    public void release() {
        ATSC3PlayerFlags.ATSC3PlayerStartPlayback = false;
    }

    @Override
    public void showMsgFromNative(String message) {
        Log.d(TAG, message);
    }

    @Override
    public void pushMfuByteBufferFragment(MfuByteBufferFragment mfuByteBufferFragment) {
        //jjustman-2020-11-19 - remove this hack workaround, as we may be losing audio sync due to incorrect frame calculation without analyzing trun box
        //jjustman-2020-08-19 - hack-ish workaround for ac-4 and mmt_atsc3_message signalling information w/ sample duration (or avoiding parsing the trun box)
//        if (MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us != 0 || MmtPacketIdContext.audio_packet_statistics.extracted_sample_duration_us == 0) {
//            if (MmtPacketIdContext.audio_packet_id == mfuByteBufferFragment.packet_id && isKeySample(mfuByteBufferFragment)) {
//                Log.d("PushMfuByteBufferFragment:INFO", String.format(" packet_id: %d, mpu_sequence_number: %d, setting audio_packet_statistics.extracted_sample_duration_us to follow video: %d * 2",
//                        mfuByteBufferFragment.packet_id, mfuByteBufferFragment.mpu_sequence_number, MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us));
//            }
//            MmtPacketIdContext.audio_packet_statistics.extracted_sample_duration_us = MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us * 2;
//        } else
//
        if (MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us == 0 || MmtPacketIdContext.audio_packet_statistics.extracted_sample_duration_us == 0) {
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

        if (isVideoSample(mfuByteBufferFragment)) {
            addVideoFragment(mfuByteBufferFragment);
        } else if (isAudioSample(mfuByteBufferFragment)) {
            addAudioFragment(mfuByteBufferFragment);
        } else if (isTextSample(mfuByteBufferFragment)) {
            addSubtitleFragment(mfuByteBufferFragment);
        } else {
            mfuByteBufferFragment.unreferenceByteBuffer();
        }
    }

    @Override
    public void pushMpuMetadata_HEVC_NAL_Payload(MpuMetadata_HEVC_NAL_Payload payload) {
        ATSC3PlayerFlags.ATSC3PlayerStartPlayback = true;

        if (initMpuMetadata_HEVC_NAL_Payload == null) {
            initMpuMetadata_HEVC_NAL_Payload = payload;
        } else {
            payload.releaseByteBuffer();
        }
    }

    @Override
    public void pushAudioDecoderConfigurationRecord(MMTAudioDecoderConfigurationRecord mmtAudioDecoderConfigurationRecord) {
        //TODO: remove audioConfigurationMap.isEmpty()
        if (audioConfigurationMap.isEmpty()) {
            audioConfigurationMap.put(mmtAudioDecoderConfigurationRecord, false);
        }
    }

    private boolean readMMTInitialPayload(ExtractorInput input) throws IOException, InterruptedException {
        while (initMpuMetadata_HEVC_NAL_Payload == null) {
            readNextUdpPacket(input);
        }

        while (!skipUntilKeyFrame()) {
            readNextUdpPacket(input);
        }

        return true;
    }

    private MfuByteBufferFragment pollNextSample(ExtractorInput input) throws IOException, InterruptedException {
        MfuByteBufferFragment nextSample = mfuBufferQueue.poll();
        while (nextSample == null) {
            readNextUdpPacket(input);
            nextSample = mfuBufferQueue.poll();
        }

        return nextSample;
    }

    private int readSample(ExtractorInput extractorInput) throws IOException, InterruptedException {
        MfuByteBufferFragment currentSample;
        try {
            currentSample = pollNextSample(extractorInput);
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.w(TAG, "readSample - Exception, returning END_OF_INPUT - causing ExoPlayer DataSource teardown/unwind, ex: ", ex);

            return Extractor.RESULT_END_OF_INPUT;
        }

        byte currentSampleType;
        if (isVideoSample(currentSample)) {
            currentSampleType = C.TRACK_TYPE_VIDEO;
        } else if (isAudioSample(currentSample)) {
            currentSampleType = C.TRACK_TYPE_AUDIO;
        } else if (isTextSample(currentSample)) {
            currentSampleType = C.TRACK_TYPE_TEXT;
        } else {
            currentSampleType = C.TRACK_TYPE_UNKNOWN;
        }

        MmtTrack track = tracks.get(currentSampleType);
        if (track == null) {
            dereferenceSample(currentSample);
            Log.d(TAG, "SKIP sample, Type: " + currentSampleType);
            return Extractor.RESULT_CONTINUE;
        }

        int currentSampleSize = currentSample.bytebuffer_length;
        long currentSampleTimeUs = getPresentationTimestampUs(currentSample);
        boolean currentSampleIsKey = isKeySample(currentSample);

        Log.d(TAG, "process sample, Type: " + currentSampleType + ", sample TimeUs: " + currentSampleTimeUs + ",  sample size: " + currentSampleSize);

        TrackOutput trackOutput = track.trackOutput;

        currentSampleBuffer.reset(currentSample.myByteBuffer.array());
        trackOutput.sampleData(currentSampleBuffer, currentSampleSize);

        dereferenceSample(currentSample);

        @C.BufferFlags int sampleFlags = 0;
        if (currentSampleIsKey) {
            sampleFlags = C.BUFFER_FLAG_KEY_FRAME;
        }

        long correctSampleTime = track.correctSampleTime(currentSampleTimeUs);

        trackOutput.sampleMetadata(
                correctSampleTime,
                sampleFlags,
                currentSampleSize,
                /* offset= */ 0,
                /* encryptionData= */ null);

        return Extractor.RESULT_CONTINUE;
    }

    private void dereferenceSample(MfuByteBufferFragment sample) {
        currentSampleBuffer.reset(Util.EMPTY_BYTE_ARRAY);
        sample.unreferenceByteBuffer();
    }

    private static int getUnsignedByte(ByteBuffer bb) {
        return (bb.get() & 0xff);
    }

    private static void skip(ByteBuffer bb, int bytes) {
        bb.position(bb.position() + bytes);
    }

    private int readNextUdpPacket(ExtractorInput input) throws IOException, InterruptedException {
        int packetLength;
        while (true) {
            udpPacketBuffer.rewind();
            input.readFully(udpPacketBuffer.array(), udpPacketBuffer.arrayOffset(), IP_HEADER_SIZE);
            udpPacketBuffer.position(0);

            //read ip header
            /*
             * struct ip {
             * #if BYTE_ORDER == LITTLE_ENDIAN
             * 	            u_char	ip_hl:4,		// header length
             *                      ip_v:4;			// version
             * #endif
             * #if BYTE_ORDER == BIG_ENDIAN
             *             u_char	ip_v:4,			// version
             *                      ip_hl:4;		// header length
             * #endif
             *             u_char	ip_tos;			// type of service
             *             short	ip_len;			// total length
             *             u_short	ip_id;			// identification
             *             short	ip_off;			// fragment offset field
             * #define	IP_DF 0x4000    			// dont fragment flag
             * #define	IP_MF 0x2000	    		// more fragments flag
             *             u_char	ip_ttl;			// time to live
             *             u_char	ip_p;			// protocol
             *             u_short	ip_sum;			// checksum
             *             struct	in_addr ip_src,ip_dst;	// source and dest address
             * };
             *
             */
            int firstByte = getUnsignedByte(udpPacketBuffer);
            int ip_v = firstByte >> 4;
            skip(udpPacketBuffer, 1 /* ip_tos */);
            int ip_len = udpPacketBuffer.getShort();
            skip(udpPacketBuffer, 2 + 2 + 1/* ip_id + ip_off + ip_ttl */);
            int ip_p = getUnsignedByte(udpPacketBuffer);
            skip(udpPacketBuffer, 2 + 4 + 4 /* ip_sum + ip_src + ip_dst*/);

            //Log.d(TAG, "id: " + ip_id + ", len: " + ip_len + ", v: " + ip_v);

            if (ip_v == IPVERSION && ip_p == IPPROTO_UDP) {
                packetLength = ip_len;
                break;
            }

            input.skipFully(ip_len - IP_HEADER_SIZE);
        }

        input.readFully(udpPacketBuffer.array(), udpPacketBuffer.arrayOffset() + IP_HEADER_SIZE, packetLength - IP_HEADER_SIZE);
        udpPacketBuffer.position(0);

        atsc3NdkMediaMMTBridge.atsc3_process_mmtp_udp_packet(udpPacketBuffer, packetLength);

        return packetLength;
    }

    private void maybeOutputFormat() {
        if (!hasOutputFormat) {
            hasOutputFormat = true;

            //TODO: get actual track format !!!
            int videoType = Util.getIntegerCodeForString("hev1");
            int textType = Util.getIntegerCodeForString("stpp");

            int videoWidth = MmtPacketIdContext.video_packet_statistics.width > 0 ? MmtPacketIdContext.video_packet_statistics.width : MmtPacketIdContext.MmtMfuStatistics.FALLBACK_WIDTH;
            int videoHeight = MmtPacketIdContext.video_packet_statistics.height > 0 ? MmtPacketIdContext.video_packet_statistics.height : MmtPacketIdContext.MmtMfuStatistics.FALLBACK_HEIGHT;
            float videoFrameRate = (float) 1000000.0 / MmtPacketIdContext.video_packet_statistics.extracted_sample_duration_us;

            //TODO: copy it?
            ByteBuffer initBuffer = initMpuMetadata_HEVC_NAL_Payload.myByteBuffer;
            TrackOutput videoOutput = MediaTrackUtils.createVideoOutput(extractorOutput, /* id */1, videoType, videoWidth, videoHeight, videoFrameRate, initBuffer.array());
            if (videoOutput != null) {
                tracks.put(C.TRACK_TYPE_VIDEO, new MmtTrack(videoOutput, PTS_OFFSET_US));
            }

            if (!audioConfigurationMap.isEmpty()) {
                audioConfigurationMap.entrySet().forEach(entry -> {
                    if (!entry.getValue()) {
                        MMTAudioDecoderConfigurationRecord record = entry.getKey();
                        if (record.audioAC4SampleEntryBox != null) {
                            TrackOutput audioOutput = MediaTrackUtils.createAudioOutput(extractorOutput, record.packet_id, record.audioAC4SampleEntryBox.type, record.channel_count, record.sample_rate);
                            if (audioOutput != null) {
                                entry.setValue(true);
                                tracks.put(C.TRACK_TYPE_AUDIO, new MmtTrack(audioOutput, PTS_OFFSET_US));
                            }
                        }
                    }
                });
            }

            TrackOutput textOutput = MediaTrackUtils.createTextOutput(extractorOutput, /* id */3, textType);
            if (textOutput != null) {
                tracks.put(C.TRACK_TYPE_TEXT, new MmtTrack(textOutput, 0));
            }

            extractorOutput.endTracks();
        }
    }

    private void maybeOutputSeekMap() {
        if (hasOutputSeekMap) {
            return;
        }

        extractorOutput.seekMap(new SeekMap.Unseekable(C.TIME_UNSET));
        hasOutputSeekMap = true;
    }

    private static final class MmtTrack {

        public final TrackOutput trackOutput;
        private final long defaultSampleDurationUs;

        private long timeOffsetUs;
        private long someTime;

        public MmtTrack(TrackOutput trackOutput, long defaultSampleDurationUs) {
            this.trackOutput = trackOutput;
            this.defaultSampleDurationUs = defaultSampleDurationUs;
        }

        public long correctSampleTime(long sampleTimeUs) {
            if (sampleTimeUs <= 0) {
                final long offset;
                if (someTime > 0) {
                    offset = (System.currentTimeMillis() - someTime) * 1000;
                } else {
                    offset = 0;
                    someTime = System.currentTimeMillis();
                }

                //by default for any missing MMT SI emissions or flash-cut into MMT flow emission, use now_Us + 66000uS for our presentationTimestampUs
                return timeOffsetUs + offset + defaultSampleDurationUs;
            } else {
                someTime = 0;
                timeOffsetUs = sampleTimeUs;

                return sampleTimeUs;
            }
        }
    }

    private void addVideoFragment(MfuByteBufferFragment mfuByteBufferFragment) {
        // if(mfuByteBufferFragment.sample_number == 1 || firstMfuBufferVideoKeyframeSent) {
        //normal flow...
        mfuBufferQueue.add(mfuByteBufferFragment);

        if (isKeySample(mfuByteBufferFragment)) {
            if (!firstMfuBufferVideoKeyframeSent) {
                Log.d("pushMfuByteBufferFragment", String.format("V: pushing FIRST: queueSize: %d, sampleNumber: %d, size: %d, mpuPresentationTimeUs: %d",
                        mfuBufferQueue.size(),
                        mfuByteBufferFragment.sample_number,
                        mfuByteBufferFragment.bytebuffer_length,
                        mfuByteBufferFragment.mpu_presentation_time_uS_from_SI));
            }
            firstMfuBufferVideoKeyframeSent = true;

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
        if (!firstMfuBufferVideoKeyframeSent) {
            return;
        }

        mfuBufferQueue.add(mfuByteBufferFragment);
        MmtPacketIdContext.stpp_packet_statistics.total_mfu_samples_count++;
    }

    private boolean skipUntilKeyFrame() {
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

    private boolean isKeySample(MfuByteBufferFragment fragment) {
        return fragment.sample_number == 1;
    }

    public boolean isVideoSample(MfuByteBufferFragment sample) {
        return MmtPacketIdContext.video_packet_id == sample.packet_id;
    }

    public boolean isAudioSample(MfuByteBufferFragment sample) {
        if (!audioConfigurationMap.isEmpty()) {
            for (Map.Entry<MMTAudioDecoderConfigurationRecord, Boolean> entry : audioConfigurationMap.entrySet()) {
                if (entry.getValue() && entry.getKey().packet_id == sample.packet_id) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isTextSample(MfuByteBufferFragment sample) {
        return MmtPacketIdContext.stpp_packet_id == sample.packet_id;
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
            return mpuPresentationTimestampDeltaUs + PTS_OFFSET_US;
        }

        return 0;
    }
}