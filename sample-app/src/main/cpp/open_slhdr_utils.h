#ifndef ANDROID_ATSC_3_0_SAMPLE_APP_A344_AND_PHY_SUPPORT_OPEN_SLHDR_UTILS_H
#define ANDROID_ATSC_3_0_SAMPLE_APP_A344_AND_PHY_SUPPORT_OPEN_SLHDR_UTILS_H

#include <algorithm>

#define PAYLOAD_MODE_PARAMETER_BASED    0x00
#define PAYLOAD_MODE_TABLE_BASED        0x01

#define PICTURE_PRIMARIES_BT_709     0x00
#define PICTURE_PRIMARIES_BT_2020    0x09

#define COLOUR_SPACE_BT_709     0x00
#define COLOUR_SPACE_BT_2020    0x01
#define COLOUR_SPACE_DCI_P3     0x02
#define COLOUR_SPACE_UNKNOWN    0xff

float calcStandardDeviation(int data[], int size) {
    int i;

    int sum = 0;
    for(i = 0; i < size; ++i) {
        sum += data[i];
    }

    float mean = (float) sum / (float) size;

    float standardDeviation = 0.0;
    for(i = 0; i < 10; ++i) {
        standardDeviation += pow((float) data[i] - mean, 2);
    }

    return sqrt(standardDeviation / 10);
}

uint8_t mapHdrDisplayColourSpace(uint16_t *primaries_x, uint16_t *primaries_y, uint16_t ref_white_x, uint16_t ref_white_y) {
    /**
     * In case the SEI syntax elements retrieved from the bitstream do not exactly match with proposed values of
     * column 2, it is recommended to allocate a value to hdrDisplayColourSpace that is the closest match to the
     * column 2 values.
     */
    int diff[8];

    diff[0] = primaries_x[0] - 15000;
    diff[1] = primaries_y[0] - 30000;
    diff[2] = primaries_x[1] - 7500;
    diff[3] = primaries_y[1] - 3000;
    diff[4] = primaries_x[2] - 32000;
    diff[5] = primaries_y[2] - 16500;
    diff[6] = ref_white_x - 15635;
    diff[7] = ref_white_y - 16450;

    float bt709sd = calcStandardDeviation(diff, 8);

    diff[0] = primaries_x[0] - 8500;
    diff[1] = primaries_y[0] - 39850;
    diff[2] = primaries_x[1] - 6550;
    diff[3] = primaries_y[1] - 2300;
    diff[4] = primaries_x[2] - 35400;
    diff[5] = primaries_y[2] - 14600;
    diff[6] = ref_white_x - 15635;
    diff[7] = ref_white_y - 16450;

    float bt2020sd = calcStandardDeviation(diff, 8);

    diff[0] = primaries_x[0] - 13250;
    diff[1] = primaries_y[0] - 34500;
    diff[2] = primaries_x[1] - 7500;
    diff[3] = primaries_y[1] - 3000;
    diff[4] = primaries_x[2] - 34000;
    diff[5] = primaries_y[2] - 16000;
    diff[6] = ref_white_x - 15635;
    diff[7] = ref_white_y - 16450;

    float dciP3sd = calcStandardDeviation(diff, 8);

    float min = std::min({bt709sd, bt2020sd, dciP3sd});
    if (min == bt709sd) {
        return COLOUR_SPACE_BT_709;
    } else if (min == bt2020sd) {
        return COLOUR_SPACE_BT_2020;
    } else if (min == dciP3sd) {
        return COLOUR_SPACE_DCI_P3;
    }

    return COLOUR_SPACE_UNKNOWN;
}

uint8_t mapHdrPicColourSpace(uint8_t hdrDisplayColourSpace, uint8_t sdrPicColourSpace) {
    if (hdrDisplayColourSpace == COLOUR_SPACE_BT_709) {
        if (sdrPicColourSpace == COLOUR_SPACE_BT_709) {
            return COLOUR_SPACE_BT_709;
        } else if (sdrPicColourSpace == COLOUR_SPACE_BT_2020) {
            return COLOUR_SPACE_BT_2020;
        }
    } else if (hdrDisplayColourSpace == COLOUR_SPACE_BT_2020 || hdrDisplayColourSpace == COLOUR_SPACE_DCI_P3) {
        if (sdrPicColourSpace == COLOUR_SPACE_BT_709 || sdrPicColourSpace == COLOUR_SPACE_BT_2020) {
            return COLOUR_SPACE_BT_2020;
        }
    }

    return COLOUR_SPACE_UNKNOWN;
}

#endif //ANDROID_ATSC_3_0_SAMPLE_APP_A344_AND_PHY_SUPPORT_OPEN_SLHDR_UTILS_H
