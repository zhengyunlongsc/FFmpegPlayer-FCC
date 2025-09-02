//
// Created by zyl on 2022/6/22.
//
#ifndef FFMPEGPLAYER_CLOG_H
#define FFMPEGPLAYER_CLOG_H

#include "android/native_window.h"
#include "android/native_window_jni.h"
#include "android/log.h"

extern "C" {
#include "libavutil/log.h"
}

#define LOGV_TAG "JniLogV"
#define LOGD_TAG "JniLogD"
#define LOGI_TAG "JniLogI"
#define LOGE_TAG "JniLogE"

// 声明一个外部变量，用于存储日志开关状态
extern bool g_log_enable;

// 定义日志函数
inline void logv(const char* tag, const char* format, ...) {
    if (g_log_enable) {
        va_list args;
        va_start(args, format);
        __android_log_vprint(ANDROID_LOG_VERBOSE, tag, format, args);
        va_end(args);
    }
}

inline void logi(const char* tag, const char* format, ...) {
    if (g_log_enable) {
        va_list args;
        va_start(args, format);
        __android_log_vprint(ANDROID_LOG_INFO, tag, format, args);
        va_end(args);
    }
}

inline void logd(const char* tag, const char* format, ...) {
    if (g_log_enable) {
        va_list args;
        va_start(args, format);
        __android_log_vprint(ANDROID_LOG_DEBUG, tag, format, args);
        va_end(args);
    }
}

inline void logw(const char* tag, const char* format, ...) {
    if (g_log_enable) {
        va_list args;
        va_start(args, format);
        __android_log_vprint(ANDROID_LOG_WARN, tag, format, args);
        va_end(args);
    }
}

inline void loge(const char* tag, const char* format, ...) {
    if (g_log_enable) {
        va_list args;
        va_start(args, format);
        __android_log_vprint(ANDROID_LOG_ERROR, tag, format, args);
        va_end(args);
    }
}

// 定义宏简化调用
#define LOGV(...) logv(LOGV_TAG, __VA_ARGS__)
#define LOGI(...) logi(LOGI_TAG, __VA_ARGS__)
#define LOGD(...) logd(LOGD_TAG, __VA_ARGS__)
#define LOGW(...) logw(LOGE_TAG, __VA_ARGS__)
#define LOGE(...) loge(LOGE_TAG, __VA_ARGS__)

void set_native_log(bool b);

#endif //FFMPEGPLAYER_CLOG_H

