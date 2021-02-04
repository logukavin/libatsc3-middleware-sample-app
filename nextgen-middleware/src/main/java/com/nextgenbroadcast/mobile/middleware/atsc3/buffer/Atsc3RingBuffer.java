package com.nextgenbroadcast.mobile.middleware.atsc3.buffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Atsc3RingBuffer {
    public static final int RING_BUFFER_PAGE_HEADER_SIZE = 33; // sizeof(RingBufferPageHeader)

    private final int pageSize;
    private final byte[] data = new byte[Long.BYTES];
    private final ByteBuffer buffer;
    private final boolean invertByteOrder;

    private int currentPageNumber;

    public Atsc3RingBuffer(ByteBuffer buffer, int pageSize, int pageNumber) {
        this.buffer = buffer;
        this.pageSize = pageSize;

        invertByteOrder = buffer.order() != ByteOrder.nativeOrder();

        currentPageNumber = pageNumber;
    }

    public int readNextPage(ByteBuffer outBuffer) {
        if (buffer.remaining() < pageSize) {
            buffer.position(0);
        }

        int ringPosition = buffer.position();

        boolean earlyIsLocked = buffer.get() != 0;
        int earlyPageNum = getInt(buffer);
        if (earlyIsLocked || earlyPageNum <= currentPageNumber) {
            buffer.position(ringPosition);
            outBuffer.limit(0);
            return -1;
        }

        int earlyBufferLen = getInt(buffer);
        int fullPacketSize = RING_BUFFER_PAGE_HEADER_SIZE + earlyBufferLen;
        int remaining = fullPacketSize;
        int segmentsInFragment = (int) Math.ceil((float) fullPacketSize / pageSize);

        if (remaining > outBuffer.capacity()) {
            buffer.position(ringPosition + pageSize);
            outBuffer.limit(0);
            return -2;
        }

        buffer.position(ringPosition);

        int bytesToRead = Math.min(remaining, pageSize);

        outBuffer.clear();
        buffer.get(outBuffer.array(), 0, bytesToRead);

        remaining -= bytesToRead;

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
                outBuffer.limit(0);
                return -1;
            }

            // Skip page outBuffer
            int segmentOffset = ringPosition + RING_BUFFER_PAGE_HEADER_SIZE;
            buffer.position(segmentOffset);

            int segmentBytesToRead = Math.min(remaining, pageSize - RING_BUFFER_PAGE_HEADER_SIZE);

            buffer.get(outBuffer.array(), fullPacketSize - remaining, segmentBytesToRead);

            byte nextSegmentNum = buffer.get(segmentOffset - 7 /* reserved part 7 bytes */);
            if (segmentNum != nextSegmentNum) {
                buffer.position(ringPosition + pageSize * (segmentsInFragment - segmentNum));
                outBuffer.limit(0);
                return -1;
            }

            segmentNum++;
            remaining -= segmentBytesToRead;
        }

        outBuffer.rewind();
        boolean isLocked = outBuffer.get() != 0;
        int pageNum = getInt(outBuffer);
        if (!isLocked && pageNum == earlyPageNum) {
            buffer.position(ringPosition + pageSize);
            currentPageNumber = pageNum;

            return getInt(outBuffer); // length
        } else {
            buffer.position(ringPosition);
            outBuffer.limit(0);

            return -1;
        }
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
}
