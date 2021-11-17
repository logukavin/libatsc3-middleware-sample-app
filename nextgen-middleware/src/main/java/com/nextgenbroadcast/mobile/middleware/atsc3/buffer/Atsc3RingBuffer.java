package com.nextgenbroadcast.mobile.middleware.atsc3.buffer;

import com.nextgenbroadcast.mobile.core.LOG;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Atsc3RingBuffer {
    public static final String TAG = Atsc3RingBuffer.class.getSimpleName();

    public static final int RESULT_FAILED = -1;
    public static final int RESULT_RETRY = -2;

    public static final byte RING_BUFFER_PAGE_INIT = 1;
    public static final byte RING_BUFFER_PAGE_FRAGMENT = 2;

    // check RingBufferPageHeader
    public static final int RING_BUFFER_PAGE_HEADER_SIZE =
            Byte.BYTES /* page lock */
                    + Integer.BYTES /* page number */
                    + Integer.BYTES /* segment number */
                    + Integer.BYTES /* payload length */
                    + Byte.BYTES /* page type */
                    + Integer.BYTES /* header length */;

    private static final int RING_BUFFER_FRAGMENT_HEADER_OFFSET =
            RING_BUFFER_PAGE_HEADER_SIZE
                    - Byte.BYTES /* page type */
                    - Integer.BYTES /* header length */;

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
        int position = outBuffer.position();
        int pageSize = readNextPage(position, outBuffer.limit(), outBuffer.array(), zeroBuffer);
        if (pageSize > 0) {
            outBuffer.position(position + RING_BUFFER_FRAGMENT_HEADER_OFFSET);
            pageSize -= RING_BUFFER_FRAGMENT_HEADER_OFFSET;
        }
        return pageSize;
    }

    /**
     * Reads one or several pages to get full Fragment data from Ring Buffer.
     * @param offset - start offset in outBuffer
     * @param readLength - maximum amount of bytes to read to outBuffer
     * @param outBuffer - buffer where to read bytes
     * @param tailBuffer - optional. if page data bigger then readLength then tailBuffer buffer will be used to store extra data or all page will be skiped.
     * @return actual amount of bytes was stored in outBuffer
     */
    public int readNextPage(int offset, int readLength, byte[] outBuffer, ByteBuffer tailBuffer) {
        /*
            Ring Buffer page structure:
            +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            +   Page Header   +   Fragment Header   +                     Payload                     +
            +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++

            Page Header:
                int8  page_lock; - indicate page lock state
                int32 page_num; - page sequence number
                int8  segment_num; - page segment number, starts from 0
                int32 payload_length; - payload size in bytes
                int8  page_type; - ring buffer page type
                int32 header_length; - fragment header size in bytes

            Fragment Header:
                int8[] * Protobuf object bytes - fragment header Protobuf bytes
         */

        // Go to the head if we at the end of buffer
        if (buffer.remaining() < pageSize) {
            buffer.position(0);
        }

        int ringPosition = buffer.position();
        int saveRingPosition = ringPosition;

        //----------------------------------- Read Page Header ------------------------------------

        // Peek page lock state and sequence number
        boolean earlyIsLocked = buffer.get() != 0;
        int earlyPageNum = getInt(buffer);
        if (earlyIsLocked || earlyPageNum <= currentPageNumber) {
            buffer.position(ringPosition);
            return RESULT_FAILED;
        }

        int segmentNum = getInt(buffer);

        // calculate payload size
        int earlyPayloadLen = getInt(buffer);
        skip(Byte.BYTES); // skip page type
        int earlyHeaderSize = getInt(buffer);
        int fullHeaderSize = RING_BUFFER_PAGE_HEADER_SIZE + earlyHeaderSize;
        int fullPacketSize = fullHeaderSize + earlyPayloadLen;
        int bytesRemaining = fullPacketSize;
        int maxPagePayloadSize = pageSize - fullHeaderSize;
        int packetPageCount = (int) Math.ceil((float) earlyPayloadLen / maxPagePayloadSize);

        // skip if page is not the first page of sequence
        if (segmentNum != 0) {
            LOG.d(TAG, "Skip first page with wrong segment number: " + segmentNum + ", pages in sequence: " + packetPageCount);
            skip(ringPosition, packetPageCount, segmentNum);
            return RESULT_RETRY;
        }

        // skip if payload bigger than out buffer
        if (bytesRemaining > readLength + tailBuffer.capacity()) {
            LOG.d(TAG, "Skip extra large fragment: " + bytesRemaining);
            skip(ringPosition, pageSize * packetPageCount);
            return RESULT_RETRY;
        }

        tailBuffer.clear();
        int outBufferBytesRemaining = readLength;

        //--------------------------------- Read Fragment Payload ---------------------------------

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
        while (bytesRemaining > 0) {
            segmentNum++;

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
                Thread.yield();
            }

            int segmentPageNum = getInt(buffer);
            if (segmentIsLocked || earlyPageNum != segmentPageNum) {
                LOG.d(TAG, "Skip page in sequence with wrong page number or locked, is locked: " + segmentIsLocked);
                if (segmentIsLocked) {
                    skip(ringPosition, packetPageCount, segmentNum);
                } else {
                    skip(ringPosition, pageSize);
                }
                tailBuffer.limit(0);
                return RESULT_FAILED;
            }

            // Check next page segment number
            int currentSegmentNum = getInt(buffer);
            if (segmentNum != currentSegmentNum) {
                LOG.d(TAG, "Skip page in sequence with wrong segment number: " + currentSegmentNum + ", expected: " + segmentNum + ", pages in sequence: " + packetPageCount);
                skip(ringPosition, packetPageCount, segmentNum);
                tailBuffer.limit(0);
                return RESULT_FAILED;
            }

            // Skip current page header
            skip(ringPosition, fullHeaderSize);

            // Read current page payload
            int pageBytesToRead = Math.min(bytesRemaining, maxPagePayloadSize);
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

            bytesRemaining -= pageBytesToRead;
        }

        //------------------------------------ Prepare Result ------------------------------------

        // Check page lock state and page sequence number
        // Skip page if it's locked or page number differs from peeked out earlier
        int outPosition = offset;
        boolean isLocked = outBuffer[outPosition] != 0;
        outPosition++; /* lock */
        int currentPageNum = getInt(outPosition, outBuffer);
        outPosition += Integer.BYTES; /* page num */
        outPosition += Integer.BYTES; /* segment num */
        int payloadLength = getInt(outPosition, outBuffer);
        outPosition += Integer.BYTES; /* payload length */
        outPosition += Byte.BYTES; /* page_type */
        int payloadHeaderSize = getInt(outPosition, outBuffer);

        if (!isLocked && currentPageNum == earlyPageNum) {
            lastPageNumber = currentPageNumber;
            lastBufferPosition = saveRingPosition;
            buffer.position(ringPosition + pageSize);
            currentPageNumber = currentPageNum;

            tailBuffer.limit(fullPacketSize > readLength ? fullPacketSize - readLength : 0);

            return RING_BUFFER_PAGE_HEADER_SIZE + payloadHeaderSize + payloadLength;
        } else {
            buffer.position(ringPosition);
            tailBuffer.limit(0);

            return RESULT_FAILED;
        }
    }

    private void skip(int bytesToSkip) {
        skip(buffer.position(), bytesToSkip);
    }

    private void skip(int ringPosition, int packetPageCount, int currentSegmentNum) {
        if (currentSegmentNum > 0 && currentSegmentNum < packetPageCount) {
            skip(ringPosition, pageSize * (packetPageCount - currentSegmentNum));
        } else {
            skip(ringPosition, pageSize);
        }
    }

    private void skip(int ringPosition, int bytesToSkip) {
        if (buffer.remaining() < bytesToSkip) {
            buffer.position(bytesToSkip - (buffer.limit() - ringPosition));
        } else {
            buffer.position(ringPosition + bytesToSkip);
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
