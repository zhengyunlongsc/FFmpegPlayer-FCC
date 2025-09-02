//
// Created by zyl on 2022/6/22.
//
#include <cstdlib>
#include "clog.h"

bool g_log_enable = false; // 默认开启日志

void ffmpeg_log(void *ptr, int level, const char *fmt, va_list vl) {
    /*char line[1024];
    int print_prefix = 1;
    va_list vl2;
    va_copy(vl2, vl);

    // av_log_format_line 可以帮我们安全处理 NULL 参数
    av_log_format_line(ptr, level, fmt, vl2, line, sizeof(line), &print_prefix);
    va_end(vl2);

    // 确保 line 结尾
    line[sizeof(line) - 1] = '\0';

    // 输出到 Android log，不会崩
    switch (level) {
        case AV_LOG_ERROR:
        case AV_LOG_FATAL:
        case AV_LOG_PANIC:
            LOGE("%s", line);
            break;
        case AV_LOG_WARNING:
            LOGW("%s", line);
            break;
        case AV_LOG_INFO:
            LOGI("%s", line);
            break;
        default:
            LOGV("%s", line);
            break;
    }*/
}


/**
 * 是否打印Native
 * @param b
 */
void set_native_log(bool b) {
    g_log_enable = b;
    if (b) {
        //LOGE("开启Native日志打印");
        av_log_set_level(AV_LOG_DEBUG);
        av_log_set_callback(ffmpeg_log);
    } else {
        //LOGE("关闭Native日志打印");
        //av_log_set_level(AV_LOG_INFO);
        av_log_set_callback(nullptr);
    }
}
