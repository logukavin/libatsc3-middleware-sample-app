#include <jni.h>
#include <string>

extern "C"
JNIEXPORT jstring JNICALL
Java_com_nextgenbroadcast_mobile_middleware_Auth0_username(JNIEnv *env, jobject thiz) {
    std::string str = "middleware-service@sinclairplatform.com";
    return env->NewStringUTF(str.c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_nextgenbroadcast_mobile_middleware_Auth0_password(JNIEnv *env, jobject thiz) {
    std::string str = "xD7R2Acu8E";
    return env->NewStringUTF(str.c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_nextgenbroadcast_mobile_middleware_Auth0_clientId(JNIEnv *env, jobject thiz) {
    std::string str = "vW7uQCHG3cF17W1D1uxBKFaP3jQHVf4i";
    return env->NewStringUTF(str.c_str());
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_nextgenbroadcast_mobile_middleware_Auth0_clientSecret(JNIEnv *env, jobject thiz) {
    std::string str = "rZYm-grsEEaQGSbJSgZIaLIrjAFs_oLVt5u0Wl1zfvVFKlA0VYrt8qwGm4AFKdsH";
    return env->NewStringUTF(str.c_str());
}