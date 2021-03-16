package com.nextgenbroadcast.mobile.middleware.atsc3.buffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Atsc3RingBuffer {
    public static final int RING_BUFFER_PAGE_HEADER_SIZE = 37; // sizeof(RingBufferPageHeader)

    private final int pageSize;
    private final byte[] data = new byte[Long.BYTES];
    private final ByteBuffer buffer;
    private final boolean invertByteOrder;

    private int currentPageNumber;
    private int lastPageNumber;
    private int lastBufferPosition;

    public Atsc3RingBuffer(ByteBuffer buffer, int pageSize) {
        this.buffer = buffer;
        this.pageSize = pageSize;

        invertByteOrder = buffer.order() != ByteOrder.nativeOrder();
    }

    public int readNextPage(ByteBuffer outBuffer) {
        // Go to head if we at the tail of buffer
        if (buffer.remaining() < pageSize) {
            buffer.position(0);
        }

        int ringPosition = buffer.position();
        int saveRingPosition = ringPosition;

        // Peek the current segment lock state and number
        boolean earlyIsLocked = buffer.get() != 0;
        int earlyPageNum = getInt(buffer);
        if (earlyIsLocked || earlyPageNum <= currentPageNumber) {
            buffer.position(ringPosition);
            outBuffer.limit(0);
            return -1;
        }

        // Calculate fragment size
        int earlyBufferLen = getInt(buffer);
        int fullPacketSize = RING_BUFFER_PAGE_HEADER_SIZE + earlyBufferLen;
        int remaining = fullPacketSize;
        int segmentsInFragment = (int) Math.ceil((float) fullPacketSize / pageSize);

        // Skip fragment if it's bigger that the out buffer
        if (remaining > outBuffer.capacity()) {
            setBufferPosition(ringPosition + pageSize * segmentsInFragment);
            outBuffer.limit(0);
            return -2;
        }

        // read fragment from current segment
        buffer.position(ringPosition);
        int bytesToRead = Math.min(remaining, pageSize);
        outBuffer.clear();
        buffer.get(outBuffer.array(), 0, bytesToRead);

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
                setBufferPosition(ringPosition + pageSize * (segmentsInFragment - segmentNum));
                outBuffer.limit(0);
                return -1;
            }

            // Read next segment payload
            int segmentOffset = ringPosition + RING_BUFFER_PAGE_HEADER_SIZE;
            buffer.position(segmentOffset);
            int segmentBytesToRead = Math.min(remaining, pageSize - RING_BUFFER_PAGE_HEADER_SIZE);
            buffer.get(outBuffer.array(), fullPacketSize - remaining, segmentBytesToRead);

            byte nextSegmentNum = buffer.get(segmentOffset - 7 /* reserved part 7 bytes */);
            if (segmentNum != nextSegmentNum) {
                setBufferPosition(ringPosition + pageSize * (segmentsInFragment - segmentNum));
                outBuffer.limit(0);
                return -1;
            }

            segmentNum++;
            remaining -= segmentBytesToRead;
        }

        // Check lock state and segment number in fragment was read.
        // Skip the fragment if it's locked or segment number differs from that was peeked out
        outBuffer.rewind();
        boolean isLocked = outBuffer.get() != 0;
        int pageNum = getInt(outBuffer);
        if (!isLocked && pageNum == earlyPageNum) {
            lastPageNumber = currentPageNumber;
            lastBufferPosition = saveRingPosition;
            setBufferPosition(ringPosition + pageSize);
            currentPageNumber = pageNum;

            return getInt(outBuffer); // length
        } else {
            buffer.position(ringPosition);
            outBuffer.limit(0);

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
}
