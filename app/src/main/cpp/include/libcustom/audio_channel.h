//
// Created by zyl on 2022/6/22.
//

#ifndef FFMPEGPLAYER_AUDIO_CHANNEL_H
#define FFMPEGPLAYER_AUDIO_CHANNEL_H

#include "base_channel.h"
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>

extern "C" {
#include <libswresample/swresample.h>
};

struct OutBuffer {
    uint8_t *out_buffer = 0;
    size_t size;
};

class BaseChannel;

class AudioChannel : public BaseChannel {
private:
    SLObjectItf engine_object = 0;// 引擎与引擎接口
    SLEngineItf engine_engine = 0;
    SLObjectItf output_mix_object = 0;//混音器
    SLObjectItf player_object = 0;
    SLPlayItf player_play = 0;//播放器
    SLAndroidSimpleBufferQueueItf buffer_queue = 0;
    SLPlaybackRateItf playback_rate = 0;


public:
    int out_channels;
    int out_sample_size;
    int out_sample_rate;
    double duration = 0;
    uint8_t *out_buffer = nullptr;
    SwrContext *swr_context = nullptr;//重采样
    VideoChannel *video_channel = nullptr;

public:
    AudioChannel(int stream_index, AVCodecContext *av_codec_context, AVRational av_rational, double duration);

    ~AudioChannel();

    void play();

    void render();

    void check_completion();

    void stop();

    void sync();

    void init_open_sl();

    void shutdown_open_sl();

    void setVideoChannel(VideoChannel *video);

    size_t get_pcm();
};

#endif //FFMPEGPLAYER_AUDIO_CHANNEL_H
