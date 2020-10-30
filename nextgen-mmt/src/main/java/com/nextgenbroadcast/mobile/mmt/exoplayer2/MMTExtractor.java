package com.nextgenbroadcast.mobile.mmt.exoplayer2;

import android.util.Log;
import android.util.SparseArray;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.ParserException;
import com.google.android.exoplayer2.extractor.Extractor;
import com.google.android.exoplayer2.extractor.ExtractorInput;
import com.google.android.exoplayer2.extractor.ExtractorOutput;
import com.google.android.exoplayer2.extractor.PositionHolder;
import com.google.android.exoplayer2.extractor.SeekMap;
import com.google.android.exoplayer2.extractor.TrackOutput;
import com.google.android.exoplayer2.util.MimeTypes;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;

public class MMTExtractor implements Extractor {
    private ExtractorOutput extractorOutput;

    private int currentSampleBytesRemaining;
    private int currentSampleSize;
    private long currentSampleTimeUs;
    private byte currentSampleType;
    private boolean currentSampleIsKey;
    private boolean hasOutputFormat;
    private boolean hasOutputSeekMap;

    private final ByteBuffer sampleHeaderBuffer = ByteBuffer.allocate(MMTDef.SIZE_SAMPLE_HEADER);
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
        ByteBuffer buffer = ByteBuffer.allocate(MMTDef.mmtSignature.length);
        input.readFully(buffer.array(), /* offset= */ 0, /* length= */ MMTDef.mmtSignature.length);
        buffer.rewind();

        return Arrays.equals(buffer.array(), MMTDef.mmtSignature);
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
            extractorInput.skipFully(currentSampleBytesRemaining);
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

        extractorInput.readFully(sampleHeaderBuffer.array(), /* offset= */ 0, /* length= */ MMTDef.SIZE_SAMPLE_HEADER);

        sampleHeaderBuffer.rewind();

        currentSampleType = sampleHeaderBuffer.get();
        currentSampleSize = sampleHeaderBuffer.getInt();
        currentSampleTimeUs = sampleHeaderBuffer.getLong();
        currentSampleIsKey = sampleHeaderBuffer.get() == 1;
    }

    private void maybeOutputFormat(ExtractorInput input) throws IOException, InterruptedException {
        if (!hasOutputFormat) {
            hasOutputFormat = true;

            ByteBuffer buffer = ByteBuffer.allocate(MMTDef.SIZE_HEADER);

            input.readFully(buffer.array(), /* offset= */ 0, /* length= */ MMTDef.SIZE_HEADER);

            buffer.rewind();

            int videoType = buffer.get();
            int audioType = buffer.get();
            int textType = buffer.get();

            int videoWidth = buffer.getInt();
            int videoHeight = buffer.getInt();
            float videoFrameRate = buffer.getFloat();
            int initialDataSize = buffer.getInt();

            int audioChannelCount = buffer.getInt();
            int audioSampleRate = buffer.getInt();

            long defaultSampleDurationUs = buffer.getLong();

            byte[] data = new byte[initialDataSize];
            input.readFully(data, /* offset= */ 0, /* length= */ initialDataSize);

            if (videoType == MMTDef.TRACK_VIDEO_HEVC) {
                TrackOutput trackOutput = extractorOutput.track(/* id= */ 1, C.TRACK_TYPE_VIDEO);
                tracks.put(C.TRACK_TYPE_VIDEO, new MmtTrack(trackOutput, defaultSampleDurationUs));

                trackOutput.format(
                        Format.createVideoSampleFormat(
                                null,
                                MimeTypes.VIDEO_H265,
                                null,
                                Format.NO_VALUE,
                                Format.NO_VALUE,
                                videoWidth,
                                videoHeight,
                                videoFrameRate,
                                Collections.singletonList(data),
                                null)
                );
            }

            if (audioType == MMTDef.TRACK_AUDIO_AC4) {
                TrackOutput trackOutput = extractorOutput.track(/* id= */ 2, C.TRACK_TYPE_AUDIO);
                tracks.put(C.TRACK_TYPE_AUDIO, new MmtTrack(trackOutput, defaultSampleDurationUs));

                trackOutput.format(
                        Format.createAudioSampleFormat(
                                null,
                                MimeTypes.AUDIO_AC4,
                                null,
                                Format.NO_VALUE,
                                Format.NO_VALUE,
                                audioChannelCount,
                                audioSampleRate,
                                null,
                                null,
                                Format.NO_VALUE,
                                null)
                );
            }

            if (textType == MMTDef.TRACK_TEXT_TTML) {
                TrackOutput trackOutput = extractorOutput.track(/* id= */ 3, C.TRACK_TYPE_TEXT);
                tracks.put(C.TRACK_TYPE_TEXT, new MmtTrack(trackOutput, 0));

                trackOutput.format(
                        Format.createTextSampleFormat(
                                null,
                                MimeTypes.APPLICATION_TTML,
                                0,
                                null)
                );
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
