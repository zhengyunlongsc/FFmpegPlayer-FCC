//
// Created by zyl on 2022/6/23.
//
#include "callback.h"

jstring getJString(JNIEnv *env, const char *c) {
    jstring str = env->NewStringUTF(c);
    return str;
}

Callback::Callback(JavaVM *java_vm, JNIEnv *env, jobject instance) {
    this->java_vm = java_vm;
    this->jni_env = env;
    this->player = env->NewGlobalRef(instance);

    jclass clazz = env->GetObjectClass(player);
    j_on_error_method_id = env->GetMethodID(clazz, "onCallback", "(ILjava/lang/String;)V");
    j_on_prepare_method_id = env->GetMethodID(clazz, "onPrepare", "()V");
    j_on_position_method_id = env->GetMethodID(clazz, "onProgress", "(D)V");
    j_on_completed_method_id = env->GetMethodID(clazz, "onCompleted", "()V");

    LOGE("Callback tag 1-1");
}

Callback::~Callback() {
    LOGE("Callback 2");
    jni_env->DeleteGlobalRef(player);
    LOGE("Callback 2-1");
}

void Callback::detach() {
    LOGE("Callback detach 2-0");
    if (is_attached) {
        LOGE("Callback detach 2-1");
        java_vm->DetachCurrentThread();
        is_attached = false;
    }
    LOGE("Callback detach 2-1");
}

void Callback::onCallback(int thread, int code, const char *msg) {
    gettimeofday(&cu, nullptr);
    timersub(&cu, &st, &di);
    LOGE("Callback onCallback tag 3-0 st=%ld,cu=%ld,di=%ld", st.tv_sec, cu.tv_sec, di.tv_sec);
    if (di.tv_sec < 1) {
        return;  // 防抖机制：1秒内只调用一次
    }

    if (this->lag_count > 1000000) {
        this->lag_count = 0;
    }
    this->lag_count++;

    gettimeofday(&st, nullptr);

    LOGE("Callback onCallback tag 3");
    if (thread == THREAD_MAIN) {
        LOGE("Callback onCallback tag 3-1");
        jni_env->CallVoidMethod(player, j_on_error_method_id, code, getJString(jni_env, msg));
    } else {
        LOGE("Callback onCallback tag 3-2");
        JNIEnv *env = nullptr;
        jint ret = java_vm->AttachCurrentThread(&env, nullptr);
        if (ret != JNI_OK) {
            LOGE("Callback onCallback tag 3-2-1");
            return;
        }
        is_attached= true;
        LOGE("Callback onCallback tag 3-3");
        env->CallVoidMethod(player, j_on_error_method_id, code, getJString(env, msg));
        LOGE("Callback onCallback tag 3-4");
    }
    LOGE("Callback onCallback tag 3-5");
}

void Callback::onPrepare(int thread) {
    LOGE("Callback onPrepare tag 4");
    if (thread == THREAD_MAIN) {
        LOGE("Callback onPrepare tag 4-1");
        this->jni_env->CallVoidMethod(player, j_on_prepare_method_id);
    } else {
        LOGE("Callback onPrepare tag 4-2");
        JNIEnv *jniEnv;
        jint ret = java_vm->AttachCurrentThread(&jniEnv, nullptr);
        LOGE("Callback onPrepare tag 4-2-0");
        if (ret != JNI_OK) {
            LOGE("Callback onPrepare tag 4-2-1");
            return;
        }
        is_attached= true;
        LOGE("Callback onPrepare tag 4-3");
        jniEnv->CallVoidMethod(player, j_on_prepare_method_id);
        java_vm->DetachCurrentThread();
        LOGE("Callback onPrepare tag 4-4");
    }

    LOGE("Callback tag 4-5");
}

/**
 * 进度
 * @param thread
 * @param position
 */
void Callback::onPosition(int thread, double position) {
    LOGE("Callback onPosition tag 5, position=%f", position);
    if (thread == THREAD_MAIN) {
        LOGE("Callback onPosition tag 5-1");
        jni_env->CallVoidMethod(player, j_on_position_method_id, position);
    } else {
        LOGE("Callback onPosition tag 5-2");
        JNIEnv *jniEnv;
        jint ret = java_vm->AttachCurrentThread(&jniEnv, nullptr);
        LOGE("Callback onPosition tag 5-2-0");
        if (ret != JNI_OK) {
            LOGE("Callback onPosition tag 5-2-1");
            return;
        }
        is_attached= true;
        LOGE("Callback onPosition tag 5-3");
        jniEnv->CallVoidMethod(player, j_on_position_method_id, position);
        java_vm->DetachCurrentThread();
        LOGE("Callback onPosition tag 5-4");
    }
    LOGE("Callback onPosition tag 5-5");
}

void Callback::onCompleted(int thread) {
    LOGE("Callback onCompleted tag 0");
    if (thread == THREAD_MAIN) {
        LOGE("Callback onCompleted tag 0-1");
        jni_env->CallVoidMethod(player, j_on_completed_method_id);
    } else {
        LOGE("Callback onCompleted tag 1-2");
        JNIEnv *jniEnv;
        jint ret = java_vm->AttachCurrentThread(&jniEnv, nullptr);
        LOGE("Callback onCompleted tag 1-2-0");
        if (ret != JNI_OK) {
            LOGE("Callback onCompleted tag 1-2-1");
            return;
        }
        is_attached= true;
        LOGE("Callback onCompleted tag 1-3");
        jniEnv->CallVoidMethod(player, j_on_completed_method_id);
        java_vm->DetachCurrentThread();
        LOGE("Callback onCompleted tag 1-4");
    }
    LOGE("Callback onCompleted tag 2-0-------------\n");
}


