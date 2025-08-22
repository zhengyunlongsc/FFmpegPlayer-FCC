//
// Created by zyl on 2022/6/22.
//

#ifndef FFMPEGPLAYER_BASE_CHANNEL_H
#define FFMPEGPLAYER_BASE_CHANNEL_H

#include "jni.h"
#include "safe_queue.h"
#include "macro.h"
#include "clog.h"
#include "callback.h"


extern "C" {
#include "libavformat/avformat.h"
#include <libavcodec/avcodec.h>
#include "libavcodec/mediacodec.h"
#include <libavutil/frame.h>
#include <libavutil/imgutils.h>
#include <libswscale/swscale.h>
#include <libavutil/time.h>
};

#define SYNC_VIDEO_AUDIO_TIME 30

using namespace std;
using namespace safe_queue;

class VideoChannel;

class AudioChannel;

typedef void(*RenderFrameSwCallback)(uint8_t *, int, int, int);

typedef void(*RenderFrameHwCallback)(AVFrame *frame);

class BaseChannel {
private:

public:
    pthread_t pid_decode = 0;
    pthread_t pid_render = 0;
    pthread_t pid_check = 0;

    int play_type = 1;
    int stream_index = -1;
    int sync_count = 0;
    int play_second = 0;
    double ren_start_time = 0;
    double total_playback_delay = 0;
    double cur_diff = 0;
    double pts_second = 0;
    double pts_second_diff = 0;
    double dec_def_diff = 0;
    double ren_def_diff = 0;
    double v_clock_diff = 0;
    double a_clock_diff = 0;
    bool is_playing = false;
    bool is_mediacodec = false;
    bool is_video_type = false;
    bool is_mul_enable = false;
    int64_t seek_position = 0;


    RenderFrameSwCallback render_frame_sw_callback = 0;
    RenderFrameHwCallback render_frame_hw_callback = 0;
    AVRational av_rational = AV_TIME_BASE_Q;
    AVFormatContext *av_format_ctx = nullptr;
    AVCodecContext *av_codec_ctx = nullptr;
    Callback *jcallback = nullptr;

    SafeQueue<AVPacket *> packets;
    SafeQueue<AVFrame *> frames;

public:
    BaseChannel(int stream_index, AVCodecContext *av_codec_context, AVRational av_rational);

    virtual ~BaseChannel();

    static void releasePacket(AVPacket **packer);

    static void releaseFrame(AVFrame **frame);

    static void syncFrame(queue<AVFrame *> &q);

    static void dropPackets(queue<AVPacket *> &q);

    static void dropFrames(queue<AVFrame *> &q);

    void decode();

    virtual void stop() = 0;

    virtual void render() = 0;

    virtual void check_completion() = 0;

    virtual void play() = 0;

    virtual void sync() = 0;

    void setRenderFrameCallback(RenderFrameSwCallback callback) {
        this->render_frame_sw_callback = callback;
    };

    void setRenderFrameMediaCodecCallback(RenderFrameHwCallback callback) {
        this->render_frame_hw_callback = callback;
    };

    double getSysDiff(double second) {
        return av_gettime() / 1000000.0 - second;
    }

    void cleanQueue() {
        frames.clear(0);
        packets.clear(0);
    }

    void stopWork() {
        frames.setWork(false);
        packets.setWork(false);
    }

    void startWork() {
        frames.setWork(true);
        packets.setWork(true);
    }
};


#endif //FFMPEGPLAYER_BASE_CHANNEL_H
