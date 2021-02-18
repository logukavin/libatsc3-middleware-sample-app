package com.nextgenbroadcast.mobile.player.exoplayer;

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
import com.google.android.exoplayer2.util.ParsableByteArray;
import com.nextgenbroadcast.mmt.exoplayer2.ext.MMTMediaTrackUtils;
import com.nextgenbroadcast.mobile.player.MMTConstants;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class Atsc3MMTExtractor implements Extractor {
    private ExtractorOutput extractorOutput;

    private int currentSampleBytesRemaining;
    private int currentSampleSize;
    private int currentSampleId;
    private long currentSampleTimeUs;
    private byte currentSampleType;
    private boolean currentSampleIsKey;
    private boolean hasOutputFormat;
    private boolean hasOutputSeekMap;

    private final ParsableByteArray buffer = new ParsableByteArray(64 * 1024);
    private final SparseArray<MmtTrack> tracks = new SparseArray<>();

    public Atsc3MMTExtractor() {
        buffer.setPosition(buffer.limit());
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
        try {
            if (currentSampleBytesRemaining == 0) {
                if (buffer.bytesLeft() < MMTConstants.SIZE_SAMPLE_HEADER) {
                    int offset = 0;
                    if (buffer.bytesLeft() > 0) {
                        offset = buffer.bytesLeft();
                        System.arraycopy(buffer.data, buffer.getPosition(), buffer.data, 0, buffer.bytesLeft());
                    }

                    extractorInput.readFully(buffer.data, /* offset= */ offset, /* length= */ /*MMTConstants.SIZE_SAMPLE_HEADER*/ buffer.limit() - offset);
                    buffer.setPosition(0);
                }

                currentSampleType = (byte) buffer.readUnsignedByte();
                currentSampleSize = buffer.readInt();
                currentSampleId = buffer.readInt();
                currentSampleTimeUs = buffer.readLong();
                currentSampleIsKey = buffer.readUnsignedByte() == 1;
                currentSampleBytesRemaining = currentSampleSize;
                //Log.d("!!!", "sid: " + currentSampleId + ", sample Type: " + currentSampleType + ", sample TimeUs: " + currentSampleTimeUs + ",  sample size: " + currentSampleSize);
            } else if (buffer.bytesLeft() == 0) {
                extractorInput.readFully(buffer.data, /* offset= */ 0, /* length= */ buffer.limit());
                buffer.setPosition(0);
            }
        } catch (Exception ex) {
            Log.w("MMTExtractor", "readSample - Exception, returning END_OF_INPUT - causing ExoPlayer DataSource teardown/unwind, ex: " + ex + ", messgae: " + ex.getMessage() + ",  Type: " + currentSampleType + ", sample TimeUs: " + currentSampleTimeUs + ",  sample size: " + currentSampleSize);

            return Extractor.RESULT_END_OF_INPUT;
        }

        MmtTrack track = tracks.get(currentSampleId);
        if (track == null) {
            if (currentSampleBytesRemaining > 0) {
                int skipped = 0;
                if (buffer.bytesLeft() > 0) {
                    skipped = Math.min(currentSampleBytesRemaining, buffer.bytesLeft());
                    buffer.skipBytes(skipped);
                }
                currentSampleBytesRemaining -= skipped;
            }

            // Empty sample means we don't have more samples in buffer. Wait for data to come before next iteration.
            if (currentSampleType == MMTConstants.TRACK_TYPE_EMPTY) {
                //jjustman-2021-02-18 - don't sleep here, let the ExoPlayerImpl eventloop doSomeWork() be responsible for timing
                // Thread.sleep(50);
            }

            return Extractor.RESULT_CONTINUE;
        }

        TrackOutput trackOutput = track.trackOutput;

        int bytesAppended = 0;
        if (buffer.bytesLeft() > 0) {
            int read = Math.min(buffer.bytesLeft(), currentSampleBytesRemaining);
            trackOutput.sampleData(buffer, read);
            bytesAppended += read;
        }

        currentSampleBytesRemaining -= bytesAppended;
        if (currentSampleBytesRemaining > 0) {
            return Extractor.RESULT_CONTINUE;
        }

        @C.BufferFlags int sampleFlags = 0;
        if (currentSampleIsKey) {
            sampleFlags = C.BUFFER_FLAG_KEY_FRAME;
        }

        /*
            jjustman-2020-12-25: NOTE:
                track.correctSampleTime will try to re-base from System.currentTimeMillis() - trackStartTime, BUT we have already done that in:

                    MMTDataByteBuffer.getPresentationTimestampUs

                NOTE: for track a/v sync, the trackStartTime needs to be the periodStartTime in millis, otherwise we will have track positionUs differences
                        that are unable to reconcile against MMT's mpu_timestamp_descriptor NTP timestamp for track syncronization,
                        even if super small (e.g. A/V lip sync observable)
        Log.d("Atsc3MMTExtractor",String.format("JJ: readSample: NOT calling track.correctSampleTime(), sample_type: %d, currentSampleTimeUs: %d", currentSampleType, currentSampleTimeUs));

     */
//        long correctSampleTime = track.correctSampleTime(currentSampleTimeUs);
//        //Log.d("Atsc3MMTExtractor",String.format("JJ: readSample: sample_type: %d, correctSampleTime: %d", currentSampleType, correctSampleTime));


        //Log.d("Atsc3MMTExtractor",String.format("JJ: readSample: sample_type: %d, currentSampleTimeUs: %d", currentSampleType, currentSampleTimeUs));

        trackOutput.sampleMetadata(
                currentSampleTimeUs,
                sampleFlags,
                currentSampleSize,
                /* offset= */ 0,
                /* encryptionData= */ null);

        return Extractor.RESULT_CONTINUE;
    }

    private void maybeOutputFormat(ExtractorInput input) throws IOException, InterruptedException {
        if (!hasOutputFormat) {
            hasOutputFormat = true;

            ByteBuffer buffer = ByteBuffer.allocate(MMTConstants.HEADER_SIZE);
            input.readFully(buffer.array(), /* offset= */ 0, /* length= */ MMTConstants.HEADER_SIZE);
            buffer.rewind();

            int mediaHeaderSize = buffer.getInt() - MMTConstants.HEADER_SIZE;
            long defaultSampleDurationUs = buffer.getLong();

            buffer = ByteBuffer.allocate(mediaHeaderSize);
            input.readFully(buffer.array(), /* offset= */ 0, /* length= */ mediaHeaderSize);
            buffer.rewind();

            while (buffer.remaining() > 0) {
                int headerSize = buffer.getInt();
                int trackType = buffer.get();

                switch (trackType) {
                    case MMTConstants.TRACK_TYPE_VIDEO: {
                        int videoType = buffer.getInt();
                        int packetId = buffer.getInt();
                        int videoWidth = buffer.getInt();
                        int videoHeight = buffer.getInt();
                        float videoFrameRate = buffer.getFloat();
                        int initialDataSize = buffer.getInt();

                        byte[] data = new byte[initialDataSize];
                        buffer.get(data, 0, initialDataSize);

                        TrackOutput videoOutput = MMTMediaTrackUtils.createVideoOutput(extractorOutput, packetId, videoType, videoWidth, videoHeight, videoFrameRate, data);
                        if (videoOutput != null) {
                            tracks.put(packetId, new MmtTrack(videoOutput, defaultSampleDurationUs));
                        }
                    }
                    break;

                    case MMTConstants.TRACK_TYPE_AUDIO: {
                        int audioType = buffer.getInt();
                        int packetId = buffer.getInt();
                        int audioChannelCount = buffer.getInt();
                        int audioSampleRate = buffer.getInt();

                        Format audioFormat = MMTMediaTrackUtils.createAudioFormat(Integer.toString(packetId),
                                audioType, audioChannelCount, audioSampleRate, 0, null);
                        if (audioFormat != null) {
                            TrackOutput audioOutput = extractorOutput.track(packetId, C.TRACK_TYPE_AUDIO);
                            audioOutput.format(audioFormat);
                            tracks.put(packetId, new MmtTrack(audioOutput, defaultSampleDurationUs));
                        }
                    }
                    break;

                    case MMTConstants.TRACK_TYPE_TEXT: {
                        int textType = buffer.getInt();
                        int packetId = buffer.getInt();

                        TrackOutput textOutput = MMTMediaTrackUtils.createTextOutput(extractorOutput, packetId, textType, "us");
                        if (textOutput != null) {
                            tracks.put(packetId, new MmtTrack(textOutput, 0));
                        }
                    }
                    break;
                }
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

        private long timeOffsetUs = 0;
        private long someTime = 0;

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
