//
// Created by zyl on 2022/6/22.
//
#include <cstdlib>
#include "clog.h"

bool g_log_enable = false; // 默认开启日志

/**
 * 被回调的打印方法
 * @param ptr
 * @param level
 * @param fmt
 * @param vl
 */
void printf(void *ptr, int level, const char *fmt, va_list vl) {
    va_list vl2;
    char *line = static_cast<char *>(malloc(128));
    static int print_prefix = 1;
    va_copy(vl2, vl);
    av_log_format_line(ptr, level, fmt, vl2, line, 128, &print_prefix);
    va_end(vl2);
    line[127] = '\0';
    LOGV("%s", line);
    free(line);

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
        av_log_set_callback(printf);
    } else {
        //LOGE("关闭Native日志打印");
        //av_log_set_level(AV_LOG_INFO);
        av_log_set_callback(nullptr);
    }
}
