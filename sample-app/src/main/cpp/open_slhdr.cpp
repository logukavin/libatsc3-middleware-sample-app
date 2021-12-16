#include <jni.h>
#include <android/log.h>

#include <open_slhdr_utils.h>
#include <bit_buffer.h>

#define __LOG_DEBUG(...) __android_log_print(ANDROID_LOG_DEBUG, "OpenSlhdr", __VA_ARGS__);

#define __SEI_PAYLOAD_TYPE_USER_DATA_REGISTER   0x04
#define __SEI_SL_HDR_1_ITU_T_T35_COUNTRY_CODE   0xB5
#define __SEI_SL_HDR_1_TERMINAL_PROVIDER_CODE   0x003A
#define __SEI_SL_HDR_1_SPEC_MAJOR_VERSION_IDC   0x00 //note, lower nibble
#define __SEI_SL_HDR_1_SPEC_HEVC_VERSION_IDC   0x00
#define __SEI_SL_HDR_1_SPEC_AVC_VERSION_IDC   0x01

void parseSlHdrMetadataFromSEI(BitBuffer *buff) {
    uint8_t sl_hdr_mode_value_minus1= buff->read(4);
    uint8_t sl_hdr_spec_major_version_idc = buff->read(4);
    uint8_t sl_hdr_spec_minor_version_idc = buff->read(7);
    uint8_t sl_hdr_cancel_flag = buff->read(1);

    //TODO: check sl_hdr_spec_major_version_idc & sl_hdr_spec_minor_version_idc

    //TODO: if (sl_hdr_cancel_flag) return;

    uint8_t sl_hdr_persistence_flag = buff->read(1);
    uint8_t original_picture_info_present_flag = buff->read(1);
    uint8_t target_picture_info_present_flag = buff->read(1);
    uint8_t src_mdcv_info_present_flag = buff->read(1);
    uint8_t sl_hdr_extension_present_flag = buff->read(1);
    uint8_t sl_hdr_payload_mode = buff->read(3);

    if (original_picture_info_present_flag) {
        uint8_t original_picture_primaries = buff->read(8);
        uint16_t original_picture_max_luminance = buff->read(16);
        uint16_t original_picture_min_luminance = buff->read(16);
    }

    uint8_t sdrPicColourSpace = COLOUR_SPACE_UNKNOWN;
    if (target_picture_info_present_flag) {
        uint8_t target_picture_primaries = buff->read(8);
        uint16_t target_picture_max_luminance = buff->read(16);
        uint16_t target_picture_min_luminance = buff->read(16);

        if (target_picture_primaries == PICTURE_PRIMARIES_BT_709) {
            sdrPicColourSpace = COLOUR_SPACE_BT_709;
        } else if (target_picture_primaries == PICTURE_PRIMARIES_BT_2020) {
            sdrPicColourSpace = COLOUR_SPACE_BT_2020;
        }
    }

    //hdrDisplayMaxLuminance = Min( 50 × (( max_display_mastering_luminance × 0,000 1 + 25 ) / 50 ); 10 000 )
    //hdrDisplayMinLuminance = Min( min_display_mastering_luminance × 0,000 1 ; 10 000 )
    uint8_t hdrDisplayColourSpace = COLOUR_SPACE_UNKNOWN;
    if (src_mdcv_info_present_flag) {
        uint16_t src_mdcv_primaries_x[3];
        uint16_t src_mdcv_primaries_y[3];
        for (int i = 0; i < 3; i++) {
            src_mdcv_primaries_x[i] = buff->read(16);
            src_mdcv_primaries_y[i] = buff->read(16);
        }
        uint16_t src_mdcv_ref_white_x = buff->read(16);
        uint16_t src_mdcv_ref_white_y = buff->read(16);
        uint16_t src_mdcv_max_mastering_luminance = buff->read(16);
        uint16_t src_mdcv_min_mastering_luminance = buff->read(16);

        hdrDisplayColourSpace = mapHdrDisplayColourSpace(src_mdcv_primaries_x, src_mdcv_primaries_y, src_mdcv_ref_white_x, src_mdcv_ref_white_y);
    } else {
        //TODO: get display params
        //hdrDisplayColourSpace = mapHdrDisplayColourSpace(display_primaries_x, display_primaries_y, white_point_x, white_point_y);
        hdrDisplayColourSpace = COLOUR_SPACE_BT_2020;
    }

    uint16_t matrix_coefficient_value[4];
    for(int i = 0; i < 4; i++) {
        matrix_coefficient_value[i] = buff->read(16);
    }

    uint16_t chroma_to_luma_injection[2];
    for(int i = 0; i < 2; i++) {
        chroma_to_luma_injection[i] = buff->read(16);
    }

    uint8_t k_coefficient_value[3];
    for(int i = 0; i < 3; i++) {
        k_coefficient_value[i] = buff->read(8);
    }

    if (sl_hdr_payload_mode == PAYLOAD_MODE_PARAMETER_BASED) {
        uint8_t tone_mapping_input_signal_black_level_offset = buff->read(8);
        uint8_t tone_mapping_input_signal_white_level_offset = buff->read(8);
        uint8_t shadow_gain_control = buff->read(8);
        uint8_t highlight_gain_control = buff->read(8);
        uint8_t mid_tone_width_adjustment_factor = buff->read(8);
        uint8_t tone_mapping_output_fine_tuning_num_val = buff->read(4);
        uint8_t saturation_gain_num_val = buff->read(4);

        uint8_t tone_mapping_output_fine_tuning_x[tone_mapping_output_fine_tuning_num_val];
        uint8_t tone_mapping_output_fine_tuning_y[tone_mapping_output_fine_tuning_num_val];
        for (int i = 0; i < tone_mapping_output_fine_tuning_num_val; i++) {
            tone_mapping_output_fine_tuning_x[i] = buff->read(8);
            tone_mapping_output_fine_tuning_y[i] = buff->read(8);
        }

        uint8_t saturation_gain_x[saturation_gain_num_val];
        uint8_t saturation_gain_y[saturation_gain_num_val];
        for (int i = 0; i < saturation_gain_num_val; i++) {
            saturation_gain_x[i] = buff->read(8);
            saturation_gain_y[i] = buff->read(8);
        }

        __LOG_DEBUG("");
    }
    /* We should not support it
    else if (sl_hdr_payload_mode == PAYLOAD_MODE_TABLE_BASED) {
        uint8_t lm_uniform_sampling_flag = buff->read(1);
        uint8_t luminance_mapping_num_val = buff->read(7);
        uint16_t luminance_mapping_x[luminance_mapping_num_val];
        uint16_t luminance_mapping_y[luminance_mapping_num_val];
        for (int i = 0; i < luminance_mapping_num_val; i++) {
            if (!lm_uniform_sampling_flag) {
                luminance_mapping_x[i] = buff->read(16);
            }
            luminance_mapping_y[i] = buff->read(16);
        }
        uint8_t cc_uniform_sampling_flag = buff->read(1);
        uint8_t colour_correction_num_val = buff->read(7);
        uint16_t colour_correction_x[colour_correction_num_val];
        uint16_t colour_correction_y[colour_correction_num_val];
        for (int i = 0; i < colour_correction_num_val; i++) {
            if (!cc_uniform_sampling_flag) {
                colour_correction_x[i] = buff->read(16);
            }
            colour_correction_y[i] = buff->read(16);
        }
    }*/

    uint8_t hdrPicColourSpace;
    if (target_picture_info_present_flag) {
        hdrPicColourSpace = mapHdrPicColourSpace(hdrDisplayColourSpace, sdrPicColourSpace);
    } else {
        hdrPicColourSpace = sdrPicColourSpace = hdrDisplayColourSpace;
    }
    uint8_t GamutMappingEnabledFlag = (sdrPicColourSpace < hdrPicColourSpace) ? 1 : 0;

    if (GamutMappingEnabledFlag) {
        uint8_t gamut_mapping_mode = buff->read(8);
        if (gamut_mapping_mode == 1) {
            //TODO: gamut_mapping_params()
        }
    }

    if (sl_hdr_extension_present_flag) {
        uint8_t sl_hdr_extension_6bits = buff->read(6);
        uint16_t sl_hdr_extension_length = buff->read(10);
        uint8_t sl_hdr_extension_data_byte[sl_hdr_extension_length];
        for(int i = 0; i < sl_hdr_extension_length; i++ ) {
            sl_hdr_extension_data_byte[i] = buff->read(8);
        }
    }

    __LOG_DEBUG("SL-HDR metadata, size: %d | sl_hdr_mode_value_minus1: %d, sl_hdr_spec_major_version_idc: %d, sl_hdr_spec_minor_version_idc: %d, sl_hdr_cancel_flag: %d",
                buff->byteLimit(), sl_hdr_mode_value_minus1, sl_hdr_spec_major_version_idc, sl_hdr_spec_minor_version_idc, sl_hdr_cancel_flag);
}

uint8_t scanForSlHdrSeiMessage(BitBuffer *buff) {
    if(buff) {
        //we need at least 6 bytes to read..
        for (int p = buff->bytePosition(); p < buff->byteLimit() - 6; p++) {
            buff->bytePosition(p);

            if (buff->read(8) == __SEI_PAYLOAD_TYPE_USER_DATA_REGISTER) {
                uint8_t payloadSize = buff->read(8);

                uint8_t itu_t_t35_country_code = buff->read(8);
                uint16_t terminal_provider_code = buff->read(16);
                uint8_t terminal_provider_oriented_code_message_idc = buff->read(8);
                if (itu_t_t35_country_code == __SEI_SL_HDR_1_ITU_T_T35_COUNTRY_CODE
                    && terminal_provider_code == __SEI_SL_HDR_1_TERMINAL_PROVIDER_CODE
                    && (terminal_provider_oriented_code_message_idc == __SEI_SL_HDR_1_SPEC_HEVC_VERSION_IDC
                        || terminal_provider_oriented_code_message_idc == __SEI_SL_HDR_1_SPEC_AVC_VERSION_IDC)) {

                    buff->byteLimit(buff->bytePosition() + payloadSize - 32);

//                    __LOG_DEBUG("Detect SL-HDR metadata: %02x%02x%02x%02x%02x - %02x%02x%02x%02x%02x",
//                                buff->read8(), buff->read8(), buff->read8(), buff->read8(), buff->read8(),
//                                data[p + 6], data[p + 7], data[p + 8], data[p + 9], data[p + 10]);
//                    __LOG_DEBUG("Detect SL-HDR metadata: %02x - %02x%02x%02x%02x%02x - %02x%02x%02x%02x%02x",
//                                data[p], data[p + 1], data[p + 2], data[p + 3], data[p + 4], data[p + 5],
//                                data[p + 6], data[p + 7], data[p + 8], data[p + 9], data[p + 10]);

                    //b5003a00-0003003001 maybe there is some prefix in payload?

                    return 1;
                }
            }
        }
    }

    return 0;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_nextgenbroadcast_mobile_middleware_sample_openSlhdr_OpenSlhdr_decodeMetadata(JNIEnv *env, jobject thiz, jobject data, jint position, jint size) {
    uint8_t *fragmentBufferPtr = reinterpret_cast<uint8_t *>(env->GetDirectBufferAddress(data));
    uint32_t fragmentBufferSize = (uint32_t) (env->GetDirectBufferCapacity(data));

    if (fragmentBufferPtr == NULL) {
        __LOG_DEBUG("Failed to get buffer Ptr. Buffer size: %d, position: %d", size, position);
        return -1;
    }

    BitBuffer *buff = new BitBuffer(fragmentBufferPtr, (uint32_t) position, std::min((uint32_t) size, fragmentBufferSize));
    uint8_t res = scanForSlHdrSeiMessage(buff);
    if (res) {
        parseSlHdrMetadataFromSEI(buff);
    }
    delete buff;

    //__android_log_write(ANDROID_LOG_DEBUG, "!!!", "OpenSlhdr");
    //__LOG_DEBUG("data size %d at %d from %d - is SL-HDR: %d", size, position, fragmentBufferSize, slhdrInfo /*fragmentBufferPtr*/);

    return 0;
}
