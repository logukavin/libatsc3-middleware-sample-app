#ifndef ANDROID_ATSC_3_0_SAMPLE_APP_A344_AND_PHY_SUPPORT_BIT_BUFFER_H
#define ANDROID_ATSC_3_0_SAMPLE_APP_A344_AND_PHY_SUPPORT_BIT_BUFFER_H

#include <stdint.h>

class BitBuffer {
private:
    uint8_t* ptr;
    uint32_t bitIndex;
    uint32_t limit;

    uint32_t readBits(const uint32_t index, const uint32_t count, uint32_t res) const;

public:
    BitBuffer(uint8_t* data, uint32_t position, uint32_t size);

    uint32_t bitRemaining() const;

    uint32_t bytePosition() const;
    void bytePosition(uint32_t position);
    void bitPosition(uint32_t position);

    uint32_t byteLimit() const;
    void byteLimit(uint32_t byteLimit);
    void bitLimit(uint32_t bitLimit);

    uint32_t read(uint8_t count);
};

#endif //ANDROID_ATSC_3_0_SAMPLE_APP_A344_AND_PHY_SUPPORT_BIT_BUFFER_H
