//
// Created by zyl on 2022/6/22.
//
#ifndef FFMPEGPLAYER_FFMPEG_H
#define FFMPEGPLAYER_FFMPEG_H

#include "sys/types.h"
#include "unistd.h"
#include "pthread.h"
#include "clog.h"
#include "callback.h"
#include "video_channel.h"

extern "C" {
#include "libavformat/avformat.h"
#include "libavcodec/mediacodec.h"
#include "libavfilter/avfilter.h"
#include "libavformat/avio.h"
}

#define MIN_PORT 10168   // 设置最小端口号为 10168
#define MAX_PORT 65680   // 设置最大端口号为 65680
#define PACKAGE_NAME "com.rzl.live"
#define SHORT_NAME_MPEGTS "mpegts"
#define SHORT_NAME_RTP "rtp"
#define RTP_ADDRESS "rtp://225.0.4.221:7980"

enum PlayerStatus {
    STOP = 0, PREPARE = 1, PLAYING = 2, PAUSE = 3, SEEKING = 4
};

struct InterruptContext {
    int64_t start_time;
    int timeout_ms;
};

class FFmpeg {

private:
    pthread_t pid_prepare = 0;
    pthread_t pid_decode = 0;
    AVIOContext *avio_ctx = nullptr;

public:
    int stream_mode = 0;
    int play_type = 1;
    char *stb_id = nullptr;
    char *stb_ip = nullptr;
    char *fcc_server_ip = nullptr;
    char *fcc_server_port = nullptr;
    char *multicast_url = nullptr;
    char *unicast_url = nullptr;
    char *package_name = nullptr;

    char *data_source = nullptr;
    char *cur_data_source = nullptr;
    char *timestamp_abt = nullptr;
    bool is_finished = true;
    bool is_mul_enable = false;
    double _duration = 0;
    double _cur_position = 0;
    enum AVPixelFormat hw_pix_fmt = AV_PIX_FMT_NONE;

    InterruptContext interrupt_ctx;
    ANativeWindow *window = nullptr;
    int64_t seek_position = 0;
    jobject surface = 0;
    jboolean mediacodec = true;
    AVFormatContext *av_format_ctx = nullptr;
    PlayerStatus player_status = STOP;

    Callback *callback = nullptr;
    AudioChannel *audio_channel = nullptr;
    AudioChannel *audio_channel2 = nullptr;
    VideoChannel *video_channel = nullptr;
    JNIEnv *jni_env = nullptr;

    pthread_mutex_t seek_mutex_t = PTHREAD_MUTEX_INITIALIZER;
    pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;
    pthread_cond_t cond = PTHREAD_COND_INITIALIZER;

public:
    FFmpeg(Callback *callback);

    ~FFmpeg();

    void setRenderFrameCallback(RenderFrameSwCallback callback);

    void setRenderFrameMediaCodecCallback(RenderFrameHwCallback callback);

    int _prepare();

    void _decode();

    void set_play_type(int type);

    void _seek_to(const char *timestamp, int type);

    void prepare();

    char *prepare_url(int *flag);

    void start();

    bool is_playing();

    void pause();

    void stop();

    void release();

    double get_position();

    double duration();

    double get_cur_diff();

    char *get_cur_data_source();

    bool check_stream(char *source);

    bool check_fcc_enable();

    void reset_seek(int64_t position);

    void completed();
};

#endif //FFMPEGPLAYER_FFMPEG_H



