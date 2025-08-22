//
// Created by zyl on 2022/6/23.
//

#ifndef FFMPEGPLAYER_CALLBACK_H
#define FFMPEGPLAYER_CALLBACK_H

#include <jni.h>
#include <sys/types.h>
#include "macro.h"
#include "clog.h"
#include "iosfwd"

class Callback {

public:
    JavaVM *java_vm = 0;
    JNIEnv *jni_env = 0;
    jobject player = 0;

    jmethodID j_on_error_method_id = 0;
    jmethodID j_on_prepare_method_id = 0;
    jmethodID j_on_position_method_id = 0;
    jmethodID j_on_completed_method_id = 0;

    struct timeval st = {0, 0};
    struct timeval cu = {0, 0};
    struct timeval di = {0, 0};
    int lag_count = 0;
    bool is_attached = false;

public:
    Callback(JavaVM *java_vm, JNIEnv *env, jobject instance);

    ~Callback();

    void onCallback(int thread, int code, const char *msg);

    void onPrepare(int thread);

    void onPosition(int thread, double position);

    void onCompleted(int thread);

    void detach();
};

#endif //FFMPEGPLAYER_CALLBACK_H
