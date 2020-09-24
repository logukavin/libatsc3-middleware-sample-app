package com.nextgenbroadcast.mobile.mmt.exoplayer2;

import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.ParserException;
import com.google.android.exoplayer2.extractor.Extractor;
import com.google.android.exoplayer2.extractor.ExtractorInput;
import com.google.android.exoplayer2.extractor.ExtractorOutput;
import com.google.android.exoplayer2.extractor.PositionHolder;
import com.google.android.exoplayer2.extractor.SeekMap;
import com.google.android.exoplayer2.extractor.TrackOutput;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;

public class MMTExtractor implements Extractor {
    private ExtractorOutput extractorOutput;
    private TrackOutput trackOutput;

    private long timeOffsetUs;
    private int currentSampleBytesRemaining;
    private int currentSampleSize;
    private long currentSampleTimeUs;
    private boolean hasOutputFormat;
    private boolean hasOutputSeekMap;

    private boolean isKeyFrame = true;
    private ByteBuffer sampleHeaderBuffer = ByteBuffer.allocate(MMTDef.SIZE_SAMPLE_HEADER);

    @Override
    public boolean sniff(ExtractorInput input) {
        return true;
    }

    @Override
    public void init(ExtractorOutput output) {
        extractorOutput = output;
        trackOutput = output.track(/* id= */ 0, C.TRACK_TYPE_VIDEO);
        output.endTracks();
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
        timeOffsetUs = 0;
    }

    @Override
    public void release() {
        // Do nothing
    }

    private boolean readMMTHeader(ExtractorInput input) throws IOException, InterruptedException {
        ByteBuffer buffer = ByteBuffer.allocate(MMTDef.mmtSignature.length);

        input.resetPeekPosition();
        input.peekFully(buffer.array(), /* offset= */ 0, /* length= */ MMTDef.mmtSignature.length);
        input.skipFully(MMTDef.SIZE_HEADER);
        buffer.rewind();

        return Arrays.equals(buffer.array(), MMTDef.mmtSignature);
    }

    private int readSample(ExtractorInput extractorInput) throws IOException, InterruptedException {
        if (currentSampleBytesRemaining == 0) {
            try {
                peekNextSampleHeader(extractorInput);
                Log.d("!!!", "sample TimeUs: " + currentSampleTimeUs + ",  sample size: " + currentSampleSize);
            } catch (EOFException e) {
                return Extractor.RESULT_END_OF_INPUT;
            }
            currentSampleBytesRemaining = currentSampleSize;
        }

        int bytesAppended = trackOutput.sampleData(extractorInput, currentSampleBytesRemaining, /* allowEndOfInput= */ true);
        if (bytesAppended == C.RESULT_END_OF_INPUT) {
            return Extractor.RESULT_END_OF_INPUT;
        }
        currentSampleBytesRemaining -= bytesAppended;
        if (currentSampleBytesRemaining > 0) {
            return Extractor.RESULT_CONTINUE;
        }

        @C.BufferFlags int sampleFlags = 0;
        if (isKeyFrame) {
            isKeyFrame = false;
            sampleFlags = C.BUFFER_FLAG_KEY_FRAME;
            timeOffsetUs = currentSampleTimeUs;
        }

        trackOutput.sampleMetadata(
                currentSampleTimeUs - timeOffsetUs,
                sampleFlags,
                currentSampleSize,
                /* offset= */ 0,
                /* encryptionData= */ null);

        return Extractor.RESULT_CONTINUE;
    }

    private void peekNextSampleHeader(ExtractorInput extractorInput) throws IOException, InterruptedException {
        sampleHeaderBuffer.clear();

        extractorInput.resetPeekPosition();
        extractorInput.peekFully(sampleHeaderBuffer.array(), /* offset= */ 0, /* length= */ MMTDef.SIZE_SAMPLE_HEADER);

        sampleHeaderBuffer.rewind();

        currentSampleSize = sampleHeaderBuffer.getInt();
        currentSampleTimeUs = sampleHeaderBuffer.getLong();

        extractorInput.skipFully(MMTDef.SIZE_SAMPLE_HEADER);
    }

    private void maybeOutputFormat(ExtractorInput input) throws IOException, InterruptedException {
        if (!hasOutputFormat) {
            hasOutputFormat = true;

            ByteBuffer buffer = ByteBuffer.allocate(MMTDef.SIZE_HEADER);

            input.resetPeekPosition();
            input.peekFully(buffer.array(), /* offset= */ 0, /* length= */ MMTDef.SIZE_HEADER);
            input.skipFully(MMTDef.SIZE_HEADER);
            buffer.rewind();

            int videoWidth = buffer.getInt();
            int videoHeight = buffer.getInt();
            float frameRate = buffer.getFloat();
            int initialDataSize = buffer.getInt();

            byte[] data = new byte[initialDataSize];
            input.resetPeekPosition();
            input.peekFully(data, /* offset= */ 0, /* length= */ initialDataSize);
            input.skipFully(initialDataSize);

            trackOutput.format(
                    Format.createVideoSampleFormat(
                            "Video1_1",
                            "video/hevc",
                            null,
                            Format.NO_VALUE,
                            Format.NO_VALUE,
                            videoWidth,
                            videoHeight,
                            frameRate,
                            Collections.singletonList(data),
                            null)
            );
        }
    }

    private void maybeOutputSeekMap() {
        if (hasOutputSeekMap) {
            return;
        }

        extractorOutput.seekMap(new SeekMap.Unseekable(C.TIME_UNSET));
        hasOutputSeekMap = true;
    }
}
