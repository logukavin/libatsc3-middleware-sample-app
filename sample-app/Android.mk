LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := libopenslhdr

LOCAL_SRC_FILES += \
    $(LOCAL_PATH)/src/main/cpp/open_slhdr.cpp \
    $(LOCAL_PATH)/src/main/cpp/bit_buffer.cpp

LOCAL_C_INCLUDES += \
    $(LOCAL_PATH)/src/main/cpp

# for android logs
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog

include $(BUILD_SHARED_LIBRARY)

