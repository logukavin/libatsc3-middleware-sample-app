package com.nextgenbroadcast.mobile.mmt.exoplayer2;

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
import com.nextgenbroadcast.mmt.exoplayer2.ext.MMTMediaTrackUtils;
import com.nextgenbroadcast.mobile.core.atsc3.mmt.MMTConstants;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Atsc3MMTExtractor implements Extractor {
    private ExtractorOutput extractorOutput;

    private int currentSampleBytesRemaining;
    private int currentSampleSize;
    private long currentSampleTimeUs;
    private byte currentSampleType;
    private boolean currentSampleIsKey;
    private boolean hasOutputFormat;
    private boolean hasOutputSeekMap;

    private final ByteBuffer sampleHeaderBuffer = ByteBuffer.allocate(MMTConstants.SIZE_SAMPLE_HEADER);
    private final SparseArray<MmtTrack> tracks = new SparseArray<>();

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
            if (!readMMTHeader(input)) {
                throw new ParserException("Could not find MMT header.");
            }
        }
        maybeOutputFormat(input);
        int sampleReadResult = readSample(input);
        maybeOutputSeekMap();
        return sampleReadResult;
    }

    @Override
    public void seek(long position, long timeUs) {
        currentSampleTimeUs = 0;
        currentSampleSize = 0;
        currentSampleBytesRemaining = 0;
    }

    @Override
    public void release() {
        // Do nothing
    }

    private boolean readMMTHeader(ExtractorInput input) throws IOException, InterruptedException {
        ByteBuffer buffer = ByteBuffer.allocate(MMTConstants.mmtSignature.length);
        input.readFully(buffer.array(), /* offset= */ 0, /* length= */ MMTConstants.mmtSignature.length);
        buffer.rewind();

        return Arrays.equals(buffer.array(), MMTConstants.mmtSignature);
    }

    private int readSample(ExtractorInput extractorInput) throws IOException, InterruptedException {
        if (currentSampleBytesRemaining == 0) {
            try {
                peekNextSampleHeader(extractorInput);
                //Log.d("!!!", "sample Type: " + currentSampleType + ", sample TimeUs: " + currentSampleTimeUs + ",  sample size: " + currentSampleSize);
            } catch (Exception ex) {
                Log.w("MMTExtractor", "readSample - Exception, returning END_OF_INPUT - causing ExoPlayer DataSource teardown/unwind, ex: "+ex+", messgae: "+ex.getMessage()+",  Type: " + currentSampleType + ", sample TimeUs: " + currentSampleTimeUs + ",  sample size: " + currentSampleSize);

                return Extractor.RESULT_END_OF_INPUT;
            }
            currentSampleBytesRemaining = currentSampleSize;
        }

        MmtTrack track = tracks.get(currentSampleType);
        if (track == null) {
            if (currentSampleBytesRemaining > 0) {
                extractorInput.skipFully(currentSampleBytesRemaining);
            }
            currentSampleBytesRemaining = 0;
            return Extractor.RESULT_CONTINUE;
        }

        TrackOutput trackOutput = track.trackOutput;

        int bytesAppended = trackOutput.sampleData(extractorInput, currentSampleBytesRemaining, /* allowEndOfInput= */ true);
        if (bytesAppended == C.RESULT_END_OF_INPUT) {
            return Extractor.RESULT_END_OF_INPUT;
        }
        currentSampleBytesRemaining -= bytesAppended;
        if (currentSampleBytesRemaining > 0) {
            return Extractor.RESULT_CONTINUE;
        }

        @C.BufferFlags int sampleFlags = 0;
        if (currentSampleIsKey) {
            sampleFlags = C.BUFFER_FLAG_KEY_FRAME;
        }

        long correctSampleTime = track.correctSampleTime(currentSampleTimeUs);
        Log.d("Atsc3MMTExtractor",String.format("JJ: readSample: sample_type: %d, correctSampleTime: %d", currentSampleType, correctSampleTime));
        trackOutput.sampleMetadata(
                correctSampleTime,
                sampleFlags,
                currentSampleSize,
                /* offset= */ 0,
                /* encryptionData= */ null);

        return Extractor.RESULT_CONTINUE;
    }

    private void peekNextSampleHeader(ExtractorInput extractorInput) throws IOException, InterruptedException {
        sampleHeaderBuffer.clear();

        extractorInput.readFully(sampleHeaderBuffer.array(), /* offset= */ 0, /* length= */ MMTConstants.SIZE_SAMPLE_HEADER);

        sampleHeaderBuffer.rewind();

        currentSampleType = sampleHeaderBuffer.get();
        currentSampleSize = sampleHeaderBuffer.getInt();
        currentSampleTimeUs = sampleHeaderBuffer.getLong();
        currentSampleIsKey = sampleHeaderBuffer.get() == 1;
    }

    private void maybeOutputFormat(ExtractorInput input) throws IOException, InterruptedException {
        if (!hasOutputFormat) {
            hasOutputFormat = true;

            ByteBuffer buffer = ByteBuffer.allocate(MMTConstants.SIZE_HEADER);

            input.readFully(buffer.array(), /* offset= */ 0, /* length= */ MMTConstants.SIZE_HEADER);

            buffer.rewind();

            int videoType = buffer.getInt();
            int audioType = buffer.getInt();
            int textType = buffer.getInt();

            int videoWidth = buffer.getInt();
            int videoHeight = buffer.getInt();
            float videoFrameRate = buffer.getFloat();
            int initialDataSize = buffer.getInt();

            int audioChannelCount = buffer.getInt();
            int audioSampleRate = buffer.getInt();

            long defaultSampleDurationUs = buffer.getLong();

            byte[] data = new byte[initialDataSize];
            input.readFully(data, /* offset= */ 0, /* length= */ initialDataSize);

            TrackOutput videoOutput = MMTMediaTrackUtils.createVideoOutput(extractorOutput, /* id */1, videoType, videoWidth, videoHeight, videoFrameRate, data);
            if (videoOutput != null) {
                tracks.put(C.TRACK_TYPE_VIDEO, new MmtTrack(videoOutput, defaultSampleDurationUs));
            }

            TrackOutput audioOutput = MMTMediaTrackUtils.createAudioOutput(extractorOutput, /* id */2, audioType, audioChannelCount, audioSampleRate);
            if (audioOutput != null) {
                tracks.put(C.TRACK_TYPE_AUDIO, new MmtTrack(audioOutput, defaultSampleDurationUs));
            }

            TrackOutput textOutput = MMTMediaTrackUtils.createTextOutput(extractorOutput, /* id */3, textType);
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
}
