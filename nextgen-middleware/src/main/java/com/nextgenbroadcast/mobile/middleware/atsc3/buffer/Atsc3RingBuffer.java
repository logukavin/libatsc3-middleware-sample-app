package com.nextgenbroadcast.mobile.middleware.atsc3.buffer;

import android.util.Log;

import com.nextgenbroadcast.mobile.core.LOG;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Atsc3RingBuffer {
    /*
        typedef struct {
            // page header
            int8_t  page_lock;
            int32_t page_num;
            int32_t payload_length;
            // payload header
            int8_t  type;
            int32_t service_id;
            int32_t packet_id;
            int32_t sample_number;
            int64_t presentationUs;
            // reserved for feature use
            int8_t  reserved[7];
        } RingBufferPageHeader;

     */

    public static final String TAG = Atsc3RingBuffer.class.getSimpleName();

    public static final int RING_BUFFER_PAGE_HEADER_SIZE = 37; // sizeof(RingBufferPageHeader)
    public static final int RING_BUFFER_PAYLOAD_HEADER_OFFSET = Byte.BYTES /* page lock */ + Integer.BYTES /* page number */ + Integer.BYTES /* payload length */;

    private final int pageSize;
    private final byte[] data = new byte[Long.BYTES];
    private final ByteBuffer buffer;
    private final boolean invertByteOrder;
    private final ByteBuffer zeroBuffer = ByteBuffer.allocate(0);

    private int currentPageNumber = 0;
    private int lastPageNumber;
    private int lastBufferPosition;

    public Atsc3RingBuffer(ByteBuffer buffer, int pageSize) {
        this.buffer = buffer;
        this.pageSize = pageSize;

        invertByteOrder = buffer.order() != ByteOrder.nativeOrder();
    }

    public int readNextPage(ByteBuffer outBuffer) {
        outBuffer.clear();
        int pageSize = readNextPage(0, outBuffer.capacity(), outBuffer.array(), zeroBuffer);
        if (pageSize > 0) {
            outBuffer.position(RING_BUFFER_PAYLOAD_HEADER_OFFSET);
        }
        return pageSize;
    }

    public int readNextPage(int offset, int readLength, byte[] outBuffer, ByteBuffer tailBuffer) {
        // Go to the head if we at the end of buffer
        if (buffer.remaining() < pageSize) {
            buffer.position(0);
        }

        int ringPosition = buffer.position();
        int saveRingPosition = ringPosition;

        // Peek page lock state and sequence number
        boolean earlyIsLocked = buffer.get() != 0;
        int earlyPageNum = getInt(buffer);
        if (earlyIsLocked || earlyPageNum <= currentPageNumber) {
            buffer.position(ringPosition);
            return -1;
        }

        // calculate payload size
        int earlyBufferLen = getInt(buffer);
        int fullPacketSize = RING_BUFFER_PAGE_HEADER_SIZE + earlyBufferLen;
        int bytesRemaining = fullPacketSize;
        int packetPageCount = (int) Math.ceil((float) fullPacketSize / pageSize);

        // skip page if payload bigger than out buffer
        if (bytesRemaining > readLength + tailBuffer.capacity()) {
            LOG.d(TAG, "Skip extra large fragment: " + bytesRemaining);
            buffer.position(ringPosition + pageSize * packetPageCount);
            return -2;
        }

        tailBuffer.clear();
        int outBufferBytesRemaining = readLength;

        // read payload from page
        buffer.position(ringPosition);
        int bytesToRead = Math.min(bytesRemaining, pageSize);
        if (bytesToRead > readLength) {
            buffer.get(outBuffer, offset, readLength);
            outBufferBytesRemaining -= readLength;
            buffer.get(tailBuffer.array(), 0, bytesToRead - readLength);
        } else {
            buffer.get(outBuffer, offset, bytesToRead);
            outBufferBytesRemaining -= bytesToRead;
        }

        bytesRemaining -= bytesToRead;

        // If payload is bigger then one page then read all related pages or skip all pages if something went wrong
        int segmentNum = 1;
        while (bytesRemaining > 0) {
            // Position to next page
            buffer.position(ringPosition + pageSize);
            if (buffer.remaining() < pageSize) {
                buffer.position(0);
            }

            ringPosition = buffer.position();

            // Peek next page lock state and sequence page number, drop if page is locked or sequency number is wrong
            boolean segmentIsLocked;
            // short await loop
            int retryCount = 5;
            while ((segmentIsLocked = (buffer.get() != 0)) && (retryCount > 0)) {
                buffer.position(buffer.position() - 1);
                retryCount--;
            }

            int segmentPageNum = getInt(buffer);
            if (segmentIsLocked || earlyPageNum != segmentPageNum) {

                /*
                    jjustman-2021-09-01 - todo: isolate how the below exception occured:

                        2021-09-01 20:43:20.932 5771-5999/com.nextgenbroadcast.mobile.middleware.sample I/chatty: uid=10118(com.nextgenbroadcast.mobile.middleware.sample) MMT Pipe #1 identical 2 lines
                        2021-09-01 20:43:20.932 5771-5999/com.nextgenbroadcast.mobile.middleware.sample D/MMTContentProvider: writeToFile:: after writer.write, bytesRead: 0
                        2021-09-01 20:43:20.933 5771-5997/com.nextgenbroadcast.mobile.middleware.sample W/Atsc3MMTExtractor: readSample - packet_id: 200, Exception, returning END_OF_INPUT - causing ExoPlayer DataSource teardown/unwind, ex: java.io.EOFException, messgae: null,  Type: 1, sample TimeUs: 3014511311,  sample size: 500
                        2021-09-01 20:43:20.934 5771-5996/com.nextgenbroadcast.mobile.middleware.sample D/ExoPlayerImplInternal: updatePlaybackPositions: rendererPositionUs: 3014231000, playbackInfo.positionUs: 3014231000
                        2021-09-01 20:43:20.944 5771-5996/com.nextgenbroadcast.mobile.middleware.sample D/ExoPlayerImplInternal: updatePlaybackPositions: rendererPositionUs: 3014241000, playbackInfo.positionUs: 3014241000
                        2021-09-01 20:43:20.945 5771-5996/com.nextgenbroadcast.mobile.middleware.sample W/AudioTrackPositionTracker: pause invoked
                        2021-09-01 20:43:20.945 5771-5996/com.nextgenbroadcast.mobile.middleware.sample W/AudioTimestampPoller: reset with updateState(STATE_INITIALZING)
                        2021-09-01 20:43:20.945 5771-5996/com.nextgenbroadcast.mobile.middleware.sample W/DefaultAudioSink: JJ: calling pause()!
                        2021-09-01 20:43:20.946 5771-5996/com.nextgenbroadcast.mobile.middleware.sample I/ExoPlayerImplInternal: JJ:calling default setState(Player.STATE_BUFFERING) and stopRenderers!
                        2021-09-01 20:43:20.948 5771-5999/com.nextgenbroadcast.mobile.middleware.sample E/MMTContentProvider: Failed to read MMT media stream
                            java.lang.IllegalArgumentException: Bad position 663552/655360   (63552 - 655360 = 8192)
                                at java.nio.Buffer.position(Buffer.java:259)
                                at com.nextgenbroadcast.mobile.middleware.atsc3.buffer.Atsc3RingBuffer.readNextPage(Atsc3RingBuffer.java:126)
                                at com.nextgenbroadcast.mobile.middleware.atsc3.buffer.Atsc3RingBuffer.readNextPage(Atsc3RingBuffer.java:51)
                                at com.nextgenbroadcast.mobile.middleware.provider.mmt.MMTFragmentWriter.readFragment(MMTFragmentWriter.java:227)
                                at com.nextgenbroadcast.mobile.middleware.provider.mmt.MMTFragmentWriter.writeQueue(MMTFragmentWriter.java:199)
                                at com.nextgenbroadcast.mobile.middleware.provider.mmt.MMTFragmentWriter.write(MMTFragmentWriter.java:112)
                                at com.nextgenbroadcast.mobile.middleware.provider.mmt.MMTContentProvider.writeToFile(MMTContentProvider.java:233)
                                at com.nextgenbroadcast.mobile.middleware.provider.mmt.MMTContentProvider.lambda$openFile$1$MMTContentProvider(MMTContentProvider.java:205)
                                at com.nextgenbroadcast.mobile.middleware.provider.mmt.MMTContentProvider$$ExternalSyntheticLambda0.run(Unknown Source:8)
                                at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1167)
                                at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:641)
                                at java.lang.Thread.run(Thread.java:764)
                        2021-09-01 20:43:20.954 5771-5996/com.nextgenbroadcast.mobile.middleware.sample D/ExoPlayerImplInternal: updatePlaybackPositions: rendererPositionUs: 3014251000, playbackInfo.positionUs: 3014251000
                        2021-09-01 20:43:20.955 5771-5996/com.nextgenbroadcast.mobile.middleware.sample W/AudioTimestampPoller: reset with updateState(STATE_INITIALZING)
                        2021-09-01 20:43:20.963 5771-5996/com.nextgenbroadcast.mobile.middleware.sample D/ExoPlayerImplInternal: updatePlaybackPositions: rendererPositionUs: 3014261000, playbackInfo.positionUs: 3014261000
                        2021-09-01 20:43:20.964 5771-5996/com.nextgenbroadcast.mobile.middleware.sample W/AudioTrackPositionTracker: JJ - NOT invoking listener.onUnderrun: bufferSize: 61504, bufferSizeMS: 320
                        2021-09-01 20:43:20.973 5771-5996/com.nextgenbroadcast.mobile.middleware.sample D/ExoPlayerImplInternal: updatePlaybackPositions: rendererPositionUs: 3014271000, playbackInfo.positionUs: 3014271000
                        2021-09-01 20:43:20.975 5771-5996/com.nextgenbroadcast.mobile.middleware.sample D/MediaCodecVideoRenderer: JJ: processOutputBuffer, dropOutputBuffer with: bufferIndex: 2, presentationTimeUs: 3012071213, earlyUs: -2221507, elapsedRealtimeUs: 27372394000, isLastBuffer: false, count: 0
                        2021-09-01 20:43:20.983 5771-5996/com.nextgenbroadcast.mobile.middleware.sample D/ExoPlayerImplInternal: updatePlaybackPositions: rendererPositionUs: 3014281000, playbackInfo.positionUs: 3014281000
                        2021-09-01 20:43:20.993 5771-5996/com.nextgenbroadcast.mobile.middleware.sample D/ExoPlayerImplInternal: updatePlaybackPositions: rendererPositionUs: 3014291000, playbackInfo.positionUs: 3014291000
                        2021-09-01 20:43:21.003 5771-5996/com.nextgenbroadcast.mobile.middleware.sample D/ExoPlayerImplInternal: updatePlaybackPositions: rendererPositionUs: 3014301000, playbackInfo.positionUs: 3014301000
                        2021-09-01 20:43:21.013 5771-5996/com.nextgenbroadcast.mobile.middleware.sample D/ExoPlayerImplInternal: updatePlaybackPositions: rendererPositionUs: 3014311000, playbackInfo.positionUs: 3014311000
                        2021-09-01 20:43:21.023 5771-5996/com.nextgenbroadcast.mobile.middleware.sample D/ExoPlayerImplInternal: updatePlaybackPositions: rendererPositionUs: 3014321000, playbackInfo.positionUs: 3014321000
                        2021-09-01 20:43:21.033 5771-5996/com.nextgenbroadcast.mobile.middleware.sample D/ExoPlayerImplInternal: updatePlaybackPositions: rendererPositionUs: 3014331000, playbackInfo.positionUs: 3014331000
                        2021-09-01 20:43:21.045 5771-5996/com.nextgenbroadcast.mobile.middleware.sample D/ExoPlayerImplInternal: updatePlaybackPositions: rendererPositionUs: 3014343000, playbackInfo.positionUs: 3014343000
                        2021-09-01 20:43:21.056 5771-5996/com.nextgenbroadcast.mobile.middleware.sample D/ExoPlayerImplInternal: updatePlaybackPositions: rendererPositionUs: 3014354000, playbackInfo.positionUs: 3014354000
                        2021-09-01 20:43:21.066 5771-5996/com.nextgenbroadcast.mobile.middleware.sample D/ExoPlayerImplInternal: updatePlaybackPositions: rendererPositionUs: 3014364000, playbackInfo.positionUs: 3014364000
                        2021-09-01 20:43:21.067 5771-5996/com.nextgenbroadcast.mobile.middleware.sample W/AudioTrackPositionTracker: handleEndOfStream: writtenFrames: 117741571
                        2021-09-01 20:43:21.067 5771-5996/com.nextgenbroadcast.mobile.middleware.sample D/AudioTrack: stop() called with 117741571 frames delivered
                 */

                int newSeekPosition = ringPosition + pageSize * (packetPageCount - segmentNum);
                if(newSeekPosition >= 0 && newSeekPosition < buffer.limit()) {
                    buffer.position(newSeekPosition);
                } else {
                    Log.w(TAG, String.format("readNextPage would position past end of buffer limit (overshoot: %d), newSeekPosition: %d, buffer.limit: %d, buffer.capacity: %d, current buffer.position: %d, seeking to 0!",
                                                                               (newSeekPosition - buffer.limit()), newSeekPosition, buffer.limit(), buffer.capacity(), buffer.position()));
                    buffer.position(0);
                }
                tailBuffer.limit(0);
                return -1;
            }

            // Check next page segment number
            byte nextSegmentNum = buffer.get(ringPosition + RING_BUFFER_PAGE_HEADER_SIZE  - 7 /* reserved part 7 bytes */);
            if (segmentNum != nextSegmentNum) {
                buffer.position(ringPosition + pageSize * (packetPageCount - segmentNum));
                tailBuffer.limit(0);
                return -1;
            }

            // Skip next page header
            buffer.position(ringPosition + RING_BUFFER_PAGE_HEADER_SIZE);
            int pageBytesToRead = Math.min(bytesRemaining, pageSize - RING_BUFFER_PAGE_HEADER_SIZE);

            // Read payload from next page payload
            if (outBufferBytesRemaining > 0) {
                int outBytesToRead = Math.min(outBufferBytesRemaining, pageBytesToRead);
                buffer.get(outBuffer, offset + fullPacketSize - bytesRemaining, outBytesToRead);
                outBufferBytesRemaining -= outBytesToRead;
                if (outBytesToRead < pageBytesToRead) {
                    buffer.get(tailBuffer.array(), fullPacketSize - bytesRemaining - readLength + outBytesToRead, pageBytesToRead - outBytesToRead);
                }
            } else {
                buffer.get(tailBuffer.array(), fullPacketSize - bytesRemaining - readLength, pageBytesToRead);
            }

            segmentNum++;
            bytesRemaining -= pageBytesToRead;
        }

        // Check page lock state and page sequence number
        // Skip page if it's locked or page number differs from peeked out earlier
        int outPosition = offset;
        boolean isLocked = outBuffer[outPosition] != 0;
        outPosition++;
        int currentPageNum = getInt(outPosition, outBuffer);
        outPosition += Integer.BYTES;
        int payloadLength = getInt(outPosition, outBuffer);
        if (!isLocked && currentPageNum == earlyPageNum) {
            lastPageNumber = currentPageNumber;
            lastBufferPosition = saveRingPosition;
            buffer.position(ringPosition + pageSize);
            currentPageNumber = currentPageNum;

            tailBuffer.limit(fullPacketSize > readLength ? fullPacketSize - readLength : 0);

            return payloadLength + RING_BUFFER_PAGE_HEADER_SIZE;
        } else {
            buffer.position(ringPosition);
            tailBuffer.limit(0);

            return -1;
        }
    }

    public void gotoPreviousPage() {
        buffer.position(lastBufferPosition);
        currentPageNumber = lastPageNumber;
    }

    public int getInt(ByteBuffer buffer) {
        buffer.get(data, 0, Integer.BYTES);

        if (invertByteOrder) {
            return (this.data[3] & 0xff) << 24
                    | (this.data[2] & 0xff) << 16
                    | (this.data[1] & 0xff) << 8
                    | this.data[0] & 0xff;
        } else {
            return (this.data[0] & 0xff) << 24
                    | (this.data[1] & 0xff) << 16
                    | (this.data[2] & 0xff) << 8
                    | this.data[3] & 0xff;
        }
    }

    public long getLong(ByteBuffer buffer) {
        buffer.get(data, 0, Long.BYTES);

        if (invertByteOrder) {
            return ((long) this.data[7] & 0xff) << 56
                    | ((long) this.data[6] & 0xff) << 48
                    | ((long) this.data[5] & 0xff) << 40
                    | ((long) this.data[4] & 0xff) << 32
                    | ((long) this.data[3] & 0xff) << 24
                    | ((long) this.data[2] & 0xff) << 16
                    | ((long) this.data[1] & 0xff) << 8
                    | (long) this.data[0] & 0xff;
        } else {
            return ((long) this.data[0] & 0xff) << 56
                    | ((long) this.data[1] & 0xff) << 48
                    | ((long) this.data[2] & 0xff) << 40
                    | ((long) this.data[3] & 0xff) << 32
                    | ((long) this.data[4] & 0xff) << 24
                    | ((long) this.data[5] & 0xff) << 16
                    | ((long) this.data[6] & 0xff) << 8
                    | (long) this.data[7] & 0xff;
        }
    }

    public int getInt(int offset, byte[] buffer) {
        if (invertByteOrder) {
            return (buffer[offset + 3] & 0xff) << 24
                    | (buffer[offset + 2] & 0xff) << 16
                    | (buffer[offset + 1] & 0xff) << 8
                    | buffer[offset] & 0xff;
        } else {
            return (buffer[offset] & 0xff) << 24
                    | (buffer[offset + 1] & 0xff) << 16
                    | (buffer[offset + 2] & 0xff) << 8
                    | buffer[offset + 3] & 0xff;
        }
    }

    public long getLong(int offset, byte[] buffer) {
        if (invertByteOrder) {
            return ((long) buffer[offset + 7] & 0xff) << 56
                    | ((long) buffer[offset + 6] & 0xff) << 48
                    | ((long) buffer[offset + 5] & 0xff) << 40
                    | ((long) buffer[offset + 4] & 0xff) << 32
                    | ((long) buffer[offset + 3] & 0xff) << 24
                    | ((long) buffer[offset + 2] & 0xff) << 16
                    | ((long) buffer[offset + 1] & 0xff) << 8
                    | (long) buffer[offset] & 0xff;
        } else {
            return ((long) buffer[offset] & 0xff) << 56
                    | ((long) buffer[offset + 1] & 0xff) << 48
                    | ((long) buffer[offset + 2] & 0xff) << 40
                    | ((long) buffer[offset + 3] & 0xff) << 32
                    | ((long) buffer[offset + 4] & 0xff) << 24
                    | ((long) buffer[offset + 5] & 0xff) << 16
                    | ((long) buffer[offset + 6] & 0xff) << 8
                    | (long) buffer[offset + 7] & 0xff;
        }
    }

    public void setInt(int offset, byte[] buffer, int value) {
        buffer[offset] = (byte) (value >> 24);
        buffer[offset + 1] = (byte) (value >> 16);
        buffer[offset + 2] = (byte) (value >> 8);
        buffer[offset + 3] = (byte) (value);
    }

    public void setLong(int offset, byte[] buffer, long value) {
        buffer[offset] = (byte) (value >> 56);
        buffer[offset + 1] = (byte) (value >> 48);
        buffer[offset + 2] = (byte) (value >> 40);
        buffer[offset + 3] = (byte) (value >> 32);
        buffer[offset + 4] = (byte) (value >> 24);
        buffer[offset + 5] = (byte) (value >> 16);
        buffer[offset + 6] = (byte) (value >> 8);
        buffer[offset + 7] = (byte) (value);
    }
}
