#include <jni.h>
#include <pthread.h>
#include <unistd.h>
#include <GLES3/gl3.h>
#include <libcustom/ffmpeg.h>

extern "C" {
#include "libavformat/avformat.h"
#include "libavcodec/avcodec.h"
#include "libavcodec/jni.h"
#include "libswscale/swscale.h"
#include "libavutil/imgutils.h"
#include "libavutil/opt.h"
#include "libswresample/swresample.h"
#include "libavfilter/avfilter.h"
}

JavaVM *java_vm = nullptr;
JNIEnv *jni_env = nullptr;
ANativeWindow *window = nullptr;
pthread_mutex_t p_mutex = PTHREAD_MUTEX_INITIALIZER;
FFmpeg *ffmpeg = nullptr;

void printCodes() {
    char vc[40000] = {0};
    char ac[40000] = {0};
    char oc[40000] = {0};

    const AVCodec *c = nullptr;
    void *opaque = nullptr;

    while ((c = av_codec_iterate(&opaque))) {
        const char *name = c->name;
        char type[32] = "/";
        strncat(type, name, strlen(name));

        switch (c->type) {
            case AVMEDIA_TYPE_VIDEO:
                strncat(vc, type, strlen(type));
                break;
            case AVMEDIA_TYPE_AUDIO:
                strncat(ac, type, strlen(type));
                break;
            default:
                strncat(oc, type, strlen(type));
                break;
        }
    }

    LOGI("native-lib codec -----------解码支持-----------");
    LOGI("native-lib codec tag 0 video-code=%s", vc);
    LOGI("native-lib codec tag 1 audio-code=%s", ac);
    LOGI("native-lib codec tag 2 other-type=%s", oc);
}

void renderFrameMediaCodec(AVFrame *frame) {
    auto *buffer = (AVMediaCodecBuffer *) frame->data[3];
    LOGE("native-lib renderFrameMediaCodec");
    av_mediacodec_release_buffer(buffer, 1);
}


void renderFrame(uint8_t *data, int line_size, int w, int h) {
    LOGE("native-lib render_frame tag 0, size=%d w=%d h=%d", line_size, w, h);
    pthread_mutex_lock(&p_mutex);
    if (!window) {
        LOGE("native-lib render_frame tag 0-1");
        pthread_mutex_unlock(&p_mutex);
        return;
    }

    LOGE("native-lib render_frame tag 1");
    ANativeWindow_setBuffersGeometry(window, w, h, WINDOW_FORMAT_RGBA_8888);
    ANativeWindow_Buffer window_buffer;

    LOGE("native-lib render_frame tag 2");
    if (ANativeWindow_lock(window, &window_buffer, nullptr)) {
        LOGE("native-lib render_frame tag 2-1");
        ANativeWindow_release(window);
        window = nullptr;
        pthread_mutex_unlock(&p_mutex);
        return;
    }

    LOGE("native-lib render_frame tag 3");
    auto *dst_data = static_cast<uint8_t *>(window_buffer.bits);
    int dst_line_size = window_buffer.stride * 4;

    LOGE("native-lib render_frame tag 4");
    for (int i = 0; i < window_buffer.height; ++i) { //一行一行拷贝到windowBuffer.bits 中
        memcpy(dst_data + i * dst_line_size, data + i * line_size, dst_line_size);
    }

    LOGE("native-lib render_frame tag 5");
    ANativeWindow_unlockAndPost(window);
    pthread_mutex_unlock(&p_mutex);
    LOGE("native-lib render_frame tag 6-end");
}

jint JNI_OnLoad(JavaVM *vm, void *args) {
    java_vm = vm;
    LOGE("native-lib JNI_OnLoad tag 0 %p", java_vm);
    av_jni_set_java_vm(vm, nullptr);
    LOGE("native-lib JNI_OnLoad tag 1");

    JNIEnv *env;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_4) != JNI_OK) {
        LOGE("native-lib JNI_OnLoad Failed to get the environment");
        return -1;
    }

    jni_env = env;
    LOGE("native-lib JNI_OnLoad tag 2-end");
    return JNI_VERSION_1_4;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_z_player_impl_FFPlayer_native_1ffmpeg_1init(JNIEnv *env, jobject thiz) {
    auto *callback = new Callback(java_vm, env, thiz);
    ffmpeg = new FFmpeg(callback);
    ffmpeg->jni_env = env;
    LOGE("native-lib version=%s", av_version_info());
    printCodes();
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_z_player_impl_FFPlayer_native_1version(JNIEnv *env, jobject instance) {
    char info[10000] = {0};
    LOGE("native-lib version=%s", av_version_info());
    return env->NewStringUTF(info);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_z_player_impl_FFPlayer_native_1log(JNIEnv *env, jobject thiz, jboolean enable) {
    set_native_log(enable);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_z_player_impl_FFPlayer_native_1set_1data_1source(JNIEnv *env, jobject thiz, jstring source) {
    if (ffmpeg == nullptr) {
        LOGE("native-lib setDataSource tag 0,ffmpeg is null...return");
        return;
    }

    LOGE("native-lib setDataSource tag 1");
    char *data_source = const_cast<char *>(env->GetStringUTFChars(source, nullptr));
    LOGE("native-lib setDataSource tag 2 data_source=%s", data_source);
    ffmpeg->data_source = strdup(data_source);
    LOGE("native-lib setDataSource tag 2-1 data_source=%s", ffmpeg->data_source);
    char *temp = strdup(data_source);
    char *path = strtok(temp, ";");

    char *first_path = path;
    LOGE("native-lib setDataSource tag 3 data_source first_path=%s", first_path);
    ffmpeg->unicast_url = strdup(first_path);
    LOGE("native-lib setDataSource tag 3-1 data_source unicast_url=%s", ffmpeg->unicast_url);

    char *last_path = nullptr;
    while (path != nullptr) {
        last_path = path;
        path = strtok(nullptr, ";");
    }
    LOGE("native-lib setDataSource tag 4 data_source last_path=%s", last_path);

    ffmpeg->multicast_url = strdup(last_path);
    LOGE("native-lib setDataSource tag 4-1 data_source multicast_url=%s", ffmpeg->multicast_url);

    free(temp);
    env->ReleaseStringUTFChars(source, data_source);
    LOGE("native-lib setDataSource tag 4-1 end");
}

extern "C"
JNIEXPORT void JNICALL
Java_com_z_player_impl_FFPlayer_native_1set_1surface(JNIEnv *env, jobject thiz, jobject surface) {
    LOGE("native-lib setSurface tag 1 tag surface=%p", surface);

    if (!ffmpeg) {
        LOGE("native-lib setSurface tag 2 ffmpeg is null,return...");
        return;
    }

    pthread_mutex_lock(&p_mutex);
    LOGE("native-lib setSurface tag 2");

    if (surface == nullptr) {
        // 处理传入 null 的情况，重置 Surface 和 ANativeWindow
        LOGE("native-lib setSurface: resetting Surface and ANativeWindow");

        if (ffmpeg->surface != nullptr) {
            env->DeleteGlobalRef(ffmpeg->surface);
            ffmpeg->surface = nullptr;
        }

        if (ffmpeg->window != nullptr) {
            LOGE("native-lib setSurface tag 1-1 release ANativeWindow");
            ANativeWindow_release(ffmpeg->window);
            ffmpeg->window = nullptr;
        }
    } else {
        // 删除旧的全局引用，避免内存泄漏
        if (ffmpeg->surface != nullptr && !env->IsSameObject(surface, ffmpeg->surface)) {
            env->DeleteGlobalRef(ffmpeg->surface);

            // 如果 Surface 发生变化，也需要释放旧的 ANativeWindow
            if (ffmpeg->window != nullptr) {
                LOGE("native-lib setSurface tag 1-1 release ANativeWindow due to surface change");
                ANativeWindow_release(ffmpeg->window);
                ffmpeg->window = nullptr;
            }
        }

        ffmpeg->surface = env->NewGlobalRef(surface);

        if (ffmpeg->window == nullptr) {
            ffmpeg->window = ANativeWindow_fromSurface(env, ffmpeg->surface);
            if (ffmpeg->window == nullptr) {
                LOGE("native-lib setSurface failed to create ANativeWindow from new surface");
                pthread_mutex_unlock(&p_mutex);
                return; // 或者其他适当的错误处理
            }

            // 设置 ANativeWindow 缓冲区的数量
            int bufferCount = ANativeWindow_setBuffersGeometry(ffmpeg->window, 0, 0, WINDOW_FORMAT_RGBA_8888);
            if (bufferCount < 0) {
                LOGE("native-lib setSurface failed to set ANativeWindow buffers geometry");
                ANativeWindow_release(ffmpeg->window);
                ffmpeg->window = nullptr;
                pthread_mutex_unlock(&p_mutex);
                return;
            }
        } else {
            // 检查当前 ANativeWindow 是否有效
            int result = ANativeWindow_getFormat(ffmpeg->window);
            if (result < 0) {
                LOGE("native-lib ANativeWindow appears to be abandoned or invalid.");
                // 释放并重新创建 ANativeWindow
                ANativeWindow_release(ffmpeg->window);
                ffmpeg->window = ANativeWindow_fromSurface(env, ffmpeg->surface);
                if (ffmpeg->window == nullptr) {
                    LOGE("native-lib Failed to recreate ANativeWindow after abandon check.");
                    pthread_mutex_unlock(&p_mutex);
                    return;
                }
            }
        }
    }

    LOGE("native-lib setSurface tag 6");
    pthread_mutex_unlock(&p_mutex);
}


extern "C"
JNIEXPORT void JNICALL
Java_com_z_player_impl_FFPlayer_native_1prepare(JNIEnv *env, jobject thiz) {
    LOGE("native-lib prepare tag 1");
    ffmpeg->prepare();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_z_player_impl_FFPlayer_native_1start(JNIEnv *env, jobject thiz) {
    LOGE("native-lib start tag 0");
    ffmpeg->setRenderFrameCallback(renderFrame);
    ffmpeg->setRenderFrameMediaCodecCallback(renderFrameMediaCodec);
    ffmpeg->start();
    LOGE("native-lib start tag 0-end");
}
extern "C"
JNIEXPORT void JNICALL
Java_com_z_player_impl_FFPlayer_native_1stop(JNIEnv *env, jobject thiz) {
    LOGE("native-lib stop tag 0");
    if (ffmpeg) {
        LOGE("native-lib stop tag 1");
        ffmpeg->stop();
    }
    LOGE("native-lib stop tag 2-end");
}
extern "C"
JNIEXPORT void JNICALL
Java_com_z_player_impl_FFPlayer_native_1release(JNIEnv *env, jobject thiz) {
    LOGE("native-lib release tag 0");

    if (window) {
        LOGE("native-lib release tag 0-1");
        ANativeWindow_release(window);
        window = nullptr;
        LOGE("native-lib release tag 0-2");
    }

    LOGE("native-lib release tag 1");
    if (ffmpeg) {
        LOGE("native-lib release tag 1-0");
        delete ffmpeg;
        ffmpeg = nullptr;
        LOGE("native-lib release tag 1-1");
    }

    LOGE("native-lib release tag 2-end");
}
extern "C"
JNIEXPORT void JNICALL
Java_com_z_player_impl_FFPlayer_native_1seek_1to(JNIEnv *env, jobject thiz, jstring seek, jint type) {
    if (!ffmpeg) {
        LOGE("native-lib seek tag 0-0");
        return; // 如果ffmpeg是null，则直接返回
    }
    LOGE("native-lib seek tag 0-1");
    ffmpeg->set_play_type(static_cast<int>(type));

    if (seek == nullptr) {
        LOGE("native-lib seek tag 0-2");
        ffmpeg->_seek_to("0", static_cast<int>(type));
        return;
    }
    LOGE("native-lib seek tag 0-3");
    const char *c_seek = env->GetStringUTFChars(seek, nullptr);
    if (c_seek == nullptr) {
        LOGE("native-lib seek tag 0-4");
        ffmpeg->_seek_to("0", static_cast<int>(type));
        return;
    }

    LOGE("native-lib seek tag 0-5");
    ffmpeg->_seek_to(c_seek, static_cast<int>(type));
    env->ReleaseStringUTFChars(seek, c_seek);
    LOGE("native-lib seek tag 0-6");
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_z_player_impl_FFPlayer_native_1duration(JNIEnv *env, jobject thiz) {
    LOGE("native-lib duration tag 0");
    double duration = 0;
    if (ffmpeg) {
        duration = ffmpeg->duration();
        LOGE("native-lib duration tag 1, duration=%f", duration);
    }
    LOGE("native-lib duration tag 2-return");
    return duration;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_z_player_impl_FFPlayer_native_1pause(JNIEnv *env, jobject thiz) {
    if (ffmpeg) {
        ffmpeg->pause();
    }
    LOGE("native-lib pause tag 0-end ");
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_z_player_impl_FFPlayer_native_1is_1playing(JNIEnv *env, jobject thiz) {
    bool playing = false;
    if (ffmpeg) {
        playing = ffmpeg->is_playing();
    }
    LOGE("native-lib is_playing tag 0 playing=%d", playing);
    return playing;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_z_player_impl_FFPlayer_native_1position(JNIEnv *env, jobject thiz) {
    double position = 0;
    if (ffmpeg) {
        position = ffmpeg->get_position();
    }
    LOGE("native-lib get_current_position tag 0 p=%f", position);
    return position;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_z_player_impl_FFPlayer_native_1set_1mediacodec(
        JNIEnv *env, jobject thiz, jboolean enable) {
    LOGE("native-lib set_mediacodec_enable tag 0 p=%hhu", enable);
    if (ffmpeg) {
        ffmpeg->mediacodec = enable;
    }
}
extern "C"
JNIEXPORT void JNICALL
Java_com_z_player_impl_FFPlayer_native_1set_1ip_1address(JNIEnv *env, jobject thiz, jstring ip) {
    char *ip_address = const_cast<char *>(env->GetStringUTFChars(ip, nullptr));
    LOGE("native-lib set_ip_address tag 0 ip_address=%s", ip_address);
    ffmpeg->stb_ip = strdup(ip_address);
    env->ReleaseStringUTFChars(ip, ip_address);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_z_player_impl_FFPlayer_native_1set_1serial_1no(JNIEnv *env, jobject thiz, jstring id) {
    char *cid = const_cast<char *>(env->GetStringUTFChars(id, nullptr));
    LOGE("native-lib set_id tag 0 id=%s", cid);
    ffmpeg->stb_id = strdup(cid);
    env->ReleaseStringUTFChars(id, cid);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_z_player_impl_FFPlayer_native_1set_1fcc_1address(JNIEnv *env, jobject thiz, jstring ip,
                                                          jstring port) {
    char *cip = const_cast<char *>(env->GetStringUTFChars(ip, nullptr));
    char *cport = const_cast<char *>(env->GetStringUTFChars(port, nullptr));
    LOGE("native-lib set_fcc_address tag 0 id=%s", cip);
    ffmpeg->fcc_server_ip = strdup(cip);
    ffmpeg->fcc_server_port = strdup(cport);
    env->ReleaseStringUTFChars(ip, cip);
    env->ReleaseStringUTFChars(ip, cport);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_z_player_impl_FFPlayer_native_1set_1mul_1enable(JNIEnv *env, jobject thiz, jboolean enable) {
    ffmpeg->is_mul_enable = enable;
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_z_player_impl_FFPlayer_native_1get_1lag_1count(JNIEnv *env, jobject thiz) {
    return ffmpeg->callback->lag_count;
}
extern "C"
JNIEXPORT jdouble JNICALL
Java_com_z_player_impl_FFPlayer_native_1get_1cur_1diff(JNIEnv *env, jobject thiz) {
    return ffmpeg->get_cur_diff();
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_z_player_impl_FFPlayer_native_1get_1cur_1source(JNIEnv *env, jobject thiz) {
    char *source = ffmpeg->get_cur_data_source();
    return env->NewStringUTF(source);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_z_player_impl_FFPlayer_native_1set_1stream_1mode(JNIEnv *env, jobject thiz, jint stream_mode) {
    ffmpeg->stream_mode = stream_mode;
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_z_player_impl_FFPlayer_native_1check_1stream(JNIEnv *env, jobject thiz, jstring source) {
    char *s = const_cast<char *>(env->GetStringUTFChars(source, nullptr));
    char *s2 = strdup(s);
    env->ReleaseStringUTFChars(source, s);
    return ffmpeg->check_stream(s2);
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_z_player_impl_FFPlayer_get_1fcc_1enable(JNIEnv *env, jobject thiz) {
    return ffmpeg->check_fcc_enable();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_z_player_impl_FFPlayer_native_1reset_1seek(JNIEnv *env, jobject thiz, jlong i) {
    ffmpeg->reset_seek(i);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_z_player_impl_FFPlayer_native_1set_1play_1type(JNIEnv *env, jobject thiz, jint type) {
    ffmpeg->play_type = type;
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_z_player_impl_FFPlayer_native_1get_1play_1type(JNIEnv *env, jobject thiz) {
    return ffmpeg->play_type;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_z_player_impl_FFPlayer_native_1set_1package_1name(JNIEnv *env, jobject thiz, jstring pn) {
    char *cpn = const_cast<char *>(env->GetStringUTFChars(pn, nullptr));
    LOGE("native-lib set_package_name tag 0 pn=%s", cpn);
    ffmpeg->package_name = strdup(cpn);
    env->ReleaseStringUTFChars(pn, cpn);
}