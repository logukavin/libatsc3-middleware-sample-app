#include <bit_buffer.h>

#include <algorithm>

BitBuffer::BitBuffer(uint8_t *data, uint32_t position, uint32_t size) {
    ptr = data;
    bitIndex = position * 8;
    limit = size * 8;
}

uint32_t BitBuffer::readBits(const uint32_t index, const uint32_t count, uint32_t res) const {
    if (index + count > limit) {
        throw;
    }

    uint32_t pos = static_cast<uint32_t>(index / 8);
    uint8_t startIndex = static_cast<uint8_t>(index - (pos * 8));
    uint32_t endIndex = startIndex + count - 1;

    // If we exceeded the number of bits that can be read
    // from this byte, then move to the next byte and
    // continue reading the bits from that byte
    if (endIndex >= 8) {
        uint8_t byte = ptr[pos];
        uint32_t offset = 8 - count - startIndex;
        if (offset < 0) {
            uint8_t mask = (0xFF >> startIndex);
            byte &= mask;
        } else {
            byte >>= offset;
        }

        uint32_t bits_read = 8 - startIndex;
        uint32_t p = count - bits_read;
        offset = 0;
        while (p < count) {
            res += static_cast<uint32_t>(((byte >> offset) & 0x01) * pow(2, p));
            ++p;
            ++offset;
        }

        return readBits(index + bits_read, count - bits_read, res);
    }

    // Remove everything in front of the starting bit
    uint8_t byte = ptr[pos];
    if (startIndex > 0) {
        uint8_t mask = ~(0xFF << (8 - startIndex));
        byte &= mask;
    }

    byte >>= (8 - count - startIndex);
    res += static_cast<uint32_t>(byte);

    return res;
}

uint32_t BitBuffer::bitRemaining() const {
    return limit - bitIndex;
}

uint32_t BitBuffer::bytePosition() const {
    return static_cast<uint32_t>(bitIndex / 8 + (bitIndex % 8 != 0 ? 1 : 0));
}

void BitBuffer::bytePosition(uint32_t position) {
    bitPosition(position * 8);
}

void BitBuffer::bitPosition(uint32_t position) {
    if (position < limit) {
        bitIndex = position;
    } else {
        throw ;
    }
}

uint32_t BitBuffer::byteLimit() const {
    return static_cast<uint32_t>(limit / 8 + (limit % 8 != 0 ? 1 : 0));
}

void BitBuffer::byteLimit(uint32_t byteLimit) {
    bitLimit(byteLimit * 8);
}

void BitBuffer::bitLimit(uint32_t bitLimit) {
    if (bitIndex > bitLimit) {
        throw ;
    }
    limit = bitLimit;
}

uint32_t BitBuffer::read(uint8_t count) {
    uint32_t value = readBits(bitIndex, count, 0);
    bitIndex += count;
    return value;
}

