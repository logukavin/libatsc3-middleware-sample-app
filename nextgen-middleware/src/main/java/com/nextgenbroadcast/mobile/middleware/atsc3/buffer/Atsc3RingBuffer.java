package com.nextgenbroadcast.mobile.middleware.atsc3.buffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Atsc3RingBuffer {
    public static final int RING_BUFFER_PAGE_HEADER_SIZE = 37; // sizeof(RingBufferPageHeader)
    public static final int RING_BUFFER_PAGE_HEADER_OFFSET = Byte.BYTES /* page lock */ + Integer.BYTES /* page number */ + Integer.BYTES /* payload length */;
    public static final int RING_BUFFER_PAGE_HEADER_FRAGMENT = Integer.BYTES /* packet_id */ + Integer.BYTES /* sample_number */ + Long.BYTES /* mpu_presentation_time_uS_from_SI */ + 7 /* reserved */;

    private final int pageSize;
    private final byte[] data = new byte[Long.BYTES];
    private final ByteBuffer buffer;
    private final boolean invertByteOrder;
    private final ByteBuffer zeroBuffer = ByteBuffer.allocate(0);

    private int currentPageNumber = 1; // should be greater then zero
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
            outBuffer.position(RING_BUFFER_PAGE_HEADER_OFFSET);
        }
        return pageSize;
    }

    public int readNextPage(int offset, int readLength, byte[] outBuffer, ByteBuffer tailBuffer) {
        // Go to head if we at the tail
        if (buffer.remaining() < pageSize) {
            buffer.position(0);
        }

        int ringPosition = buffer.position();
        int saveRingPosition = ringPosition;

        // Peek segment lock state and number
        boolean earlyIsLocked = buffer.get() != 0;
        int earlyPageNum = getInt(buffer);
        if (earlyIsLocked || earlyPageNum <= currentPageNumber) {
            buffer.position(ringPosition);
            return -1;
        }

        // calculate fragment size
        int earlyBufferLen = getInt(buffer);
        int fullPacketSize = RING_BUFFER_PAGE_HEADER_SIZE + earlyBufferLen;
        int remaining = fullPacketSize;
        int segmentsInFragment = (int) Math.ceil((float) fullPacketSize / pageSize);

        // skip fragment if it's bigger than the out buffer size
        if (remaining > readLength + tailBuffer.capacity()) {
            buffer.position(ringPosition + pageSize * segmentsInFragment);
            return -2;
        }

        tailBuffer.clear();
        int outBufferRemaining = readLength;

        // read fragment from segment
        buffer.position(ringPosition);
        int bytesToRead = Math.min(remaining, pageSize);
        if (bytesToRead > readLength) {
            buffer.get(outBuffer, offset, readLength);
            outBufferRemaining -= readLength;
            buffer.get(tailBuffer.array(), 0, bytesToRead - readLength);
        } else {
            buffer.get(outBuffer, offset, bytesToRead);
            outBufferRemaining -= bytesToRead;
        }

        remaining -= bytesToRead;

        // If fragment bigger then one segment then read all related segments or skip fragment if something went wrong
        int segmentNum = 1;
        while (remaining > 0) {
            buffer.position(ringPosition + pageSize);
            if (buffer.remaining() < pageSize) {
                buffer.position(0);
            }

            ringPosition = buffer.position();

            boolean nextIsLocked = buffer.get() != 0;
            int nextPageNum = getInt(buffer);
            if (nextIsLocked || earlyPageNum != nextPageNum) {
                buffer.position(ringPosition + pageSize * (segmentsInFragment - segmentNum));
                tailBuffer.limit(0);
                return -1;
            }

            // Skip page outBuffer
            int segmentOffset = ringPosition + RING_BUFFER_PAGE_HEADER_SIZE;
            buffer.position(segmentOffset);
            int segmentBytesToRead = Math.min(remaining, pageSize - RING_BUFFER_PAGE_HEADER_SIZE);
            //read to different buffers
            if (outBufferRemaining > 0) {
                int outBytesToRead = Math.min(outBufferRemaining, segmentBytesToRead);
                buffer.get(outBuffer, offset + fullPacketSize - remaining, outBytesToRead);
                outBufferRemaining -= outBytesToRead;
                if (outBytesToRead < segmentBytesToRead) {
                    buffer.get(tailBuffer.array(), fullPacketSize - remaining - readLength + outBytesToRead, segmentBytesToRead - outBytesToRead);
                }
            } else {
                buffer.get(tailBuffer.array(), fullPacketSize - remaining - readLength, segmentBytesToRead);
            }

            byte nextSegmentNum = buffer.get(segmentOffset - 7 /* reserved part 7 bytes */);
            if (segmentNum != nextSegmentNum) {
                buffer.position(ringPosition + pageSize * (segmentsInFragment - segmentNum));
                tailBuffer.limit(0);
                return -1;
            }

            segmentNum++;
            remaining -= segmentBytesToRead;
        }

        // Check lock state and segment number in fragment was read.
        // Skip the fragment if it's locked or segment number differs from that was peeked out
        int outPosition = offset;
        boolean isLocked = outBuffer[outPosition] != 0;
        outPosition++;
        int pageNum = getInt(outPosition, outBuffer);
        outPosition += Integer.BYTES;
        int payloadLength = getInt(outPosition, outBuffer);
        if (!isLocked && pageNum == earlyPageNum) {
            lastPageNumber = currentPageNumber;
            lastBufferPosition = saveRingPosition;
            buffer.position(ringPosition + pageSize);
            currentPageNumber = pageNum;

            tailBuffer.limit(fullPacketSize > readLength ? fullPacketSize - readLength : 0);

            return payloadLength + RING_BUFFER_PAGE_HEADER_SIZE;
        } else {
            buffer.position(ringPosition);
            tailBuffer.limit(0);

            return -1;
        }
    }

    private void setBufferPosition(int newPosition) {
        if (newPosition < buffer.limit()) {
            buffer.position(newPosition);
        } else {
            buffer.position(newPosition - buffer.limit());
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
