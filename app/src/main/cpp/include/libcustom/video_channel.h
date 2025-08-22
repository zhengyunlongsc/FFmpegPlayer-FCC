//
// Created by zyl on 2022/6/22.
//

#ifndef FFMPEGPLAYER_VIDEO_CHANNEL_H
#define FFMPEGPLAYER_VIDEO_CHANNEL_H

#include "base_channel.h"
#include "audio_channel.h"

#include <android/native_window.h>

class BaseChannel;

class VideoChannel : public BaseChannel {

public:
    int dst_line_size[4] = {0};
    bool is_reuse = false;

    ANativeWindow *window = nullptr;
    SwsContext *sws_context = nullptr;
    AudioChannel *audio_channel = nullptr;
    AVPixelFormat dst_format = AV_PIX_FMT_RGBA;
    uint8_t *dst_data[4] = {0};

    // EGL 相关变量

public:
    VideoChannel(int i, AVCodecContext *av_codec_context, AVRational av_rational);

    ~VideoChannel();

    void render();

    void check_completion();

    void play();

    void stop();

    void sync();

    void set_audio_channel(AudioChannel *audio);

    void set_window(ANativeWindow *pWindow);
};

#endif //FFMPEGPLAYER_VIDEO_CHANNEL_H
