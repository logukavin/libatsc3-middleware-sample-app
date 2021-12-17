#include <bit_buffer.h>

#include <algorithm>

BitBuffer::BitBuffer(uint8_t *data, int position, int size) {
    ptr = data;
    bitIndex = position * 8;
    capacity = limit = size * 8;
}

int BitBuffer::bitRemaining() const {
    return limit - bitIndex;
}

int BitBuffer::bytePosition() const {
    return static_cast<int>(bitIndex / 8 + (bitIndex % 8 != 0 ? 1 : 0));
}

void BitBuffer::bytePosition(int position) {
    bitPosition(position * 8);
}

void BitBuffer::bitPosition(int position) {
    if (position < limit) {
        bitIndex = position;
    } else {
        throw ;
    }
}

int BitBuffer::byteLimit() const {
    return static_cast<uint32_t>(limit / 8 + (limit % 8 != 0 ? 1 : 0));
}

void BitBuffer::byteLimit(int byteLimit) {
    bitLimit(byteLimit * 8);
}

void BitBuffer::bitLimit(int bitLimit) {
    if (bitIndex > bitLimit || bitLimit > capacity) {
        throw ;
    }
    limit = bitLimit;
}

uint32_t BitBuffer::read(int count) {
    if (bitIndex + count > limit) {
        throw;
    }

    uint32_t res = 0;

    int remaining = count;
    int position = static_cast<int>(bitIndex / 8);
    int firstBitOffset = static_cast<int>(bitIndex % 8);

    while (remaining > 0 && bitIndex <= limit) {
        int bitsToRead = std::min(remaining, 8 - firstBitOffset);
        uint8_t byte = ((ptr[position] << firstBitOffset) & 0xFF) >> (8 - bitsToRead);
        res = (res << bitsToRead) | byte;

        firstBitOffset = 0;
        position++;
        remaining -= bitsToRead;
        bitIndex += bitsToRead;
    }

    return res;
}

