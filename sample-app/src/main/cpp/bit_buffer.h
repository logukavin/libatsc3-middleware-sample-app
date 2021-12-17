#ifndef ANDROID_ATSC_3_0_SAMPLE_APP_A344_AND_PHY_SUPPORT_BIT_BUFFER_H
#define ANDROID_ATSC_3_0_SAMPLE_APP_A344_AND_PHY_SUPPORT_BIT_BUFFER_H

#include <stdint.h>

class BitBuffer {
private:
    uint8_t* ptr;
    int bitIndex;
    int limit;
    int capacity;

public:
    BitBuffer(uint8_t* data, int position, int size);

    int bitRemaining() const;

    int bytePosition() const;
    void bytePosition(int position);
    void bitPosition(int position);

    int byteLimit() const;
    void byteLimit(int byteLimit);
    void bitLimit(int bitLimit);

    uint32_t read(int count);
};

#endif //ANDROID_ATSC_3_0_SAMPLE_APP_A344_AND_PHY_SUPPORT_BIT_BUFFER_H
