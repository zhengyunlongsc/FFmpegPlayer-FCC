//
// Created by zyl on 2022/6/23.
//
#include "video_channel.h"

int get_render_type(bool is_reuse, int fmt) {
    if (is_reuse && (fmt == 0 || fmt == 12)) {
        fmt = AV_PIX_FMT_MEDIACODEC;
    }
    return fmt;
}

VideoChannel::VideoChannel(int stream_index, AVCodecContext *av_codec_context,
                           AVRational av_rational)
        : BaseChannel(stream_index, av_codec_context, av_rational) {
    this->av_codec_ctx = av_codec_context;
    this->av_rational = av_rational;
}

VideoChannel::~VideoChannel() {
    LOGE("video-channel-cpp destroy tag 0");
    if (sws_context) {
        LOGE("video-channel-cpp destroy tag 1");
        sws_freeContext(sws_context);
        sws_context = nullptr;
    }
    LOGE("video-channel-cpp destroy tag 3");
}

void VideoChannel::set_audio_channel(AudioChannel *audio) {
    this->audio_channel = audio;
}

void VideoChannel::set_window(ANativeWindow *w) {
    this->window = w;
}

void VideoChannel::check_completion() {
    while (is_playing && play_type > 2) {
        double duration = (double) av_format_ctx->duration / AV_TIME_BASE;
        if (duration <= 0) {
            LOGE("video-channel-cpp render tag 2-2-0 return");
            return;
        }

        av_usleep(10000);

        double position = pts_second - abs(pts_second_diff) + seek_position;
        LOGE("video-channel-cpp render tag 2-2 position=%f duration=%f", position, duration);

        double p_diff = duration - position;
        if (p_diff <= 2 && duration > 0 && duration < 40000 && duration > position) {
            int p_size = packets.size();
            int f_size = frames.size();
            LOGE("video-channel-cpp render tag 2-3 p_size=%d f_size=%d dur=%f pos=%f", p_size, f_size, duration, position);
            if (p_size == 0 && f_size == 0) {
                LOGE("video-channel-cpp render tag 2-4 delayed_task callback...");
                jcallback->onCompleted(THREAD_CHILD);
                break;
            }
        }
    }

    LOGE("video-channel-cpp render tag 2-end");
}

void VideoChannel::render() {
    LOGE("video-channel.cpp render tag 0-0");
    int src_width = av_codec_ctx->width;  // 这通常是视频的原始宽度
    int src_height = av_codec_ctx->height; // 这通常是视频的原始高度
    ren_def_diff = 0;
    pts_second_diff = 0;

    while (is_playing) {
        AVFrame *frame = nullptr;
        int ret = frames.pop(frame);  // 从队列中取出帧
        LOGE("video-channel.cpp render tag 0-0 ret=%d", ret);
        if (ret == 0 || !frame) {
            releaseFrame(&frame);
            continue;
        }

        this->pts_second = static_cast<double>(frame->pts) * av_q2d(av_rational);
        if (ren_def_diff == 0) {
            ren_def_diff = getSysDiff(this->pts_second);
            LOGE("video-channel-cpp render tag 0-0 ren_def_diff=%f", ren_def_diff);
        }

        if (pts_second_diff == 0) {
            pts_second_diff = 0 - this->pts_second;
            LOGE("video-channel-cpp render tag 0-1-1 pts_second_diff=%f", pts_second_diff);
        }

        v_clock_diff = ren_def_diff - getSysDiff(this->pts_second);
        double v_a_diff = v_clock_diff - audio_channel->a_clock_diff;
        LOGE("video-channel-cpp render tag 0-1 v_diff=%f a_diff=%f v_a_diff=%f size=%d play_second=%d", v_clock_diff, audio_channel->a_clock_diff,
             v_a_diff, frames.size(), play_second);

        if (v_clock_diff > 0) {
            LOGE("video-channel-cpp render tag 0-2-1 sleep to sync: %f", v_clock_diff);
            av_usleep(static_cast<int64_t>(v_clock_diff * 900000));
        } else if (v_clock_diff < -0.33 && frame->pict_type != AV_PICTURE_TYPE_I) {
            LOGE("video-channel-cpp render tag 0-2-2 drop %f", v_clock_diff);
            releaseFrame(&frame);
            continue;
        }

        int dst_width = frame->width; // 假设您希望视频在屏幕上以960像素宽度显示
        int dst_height = frame->height; // 假设您希望视频在屏幕上以540像素高度显示

        int fmt = static_cast<AVPixelFormat>(frame->format);
        LOGE("video-channel.cpp render tag 0-2 frame_fmt=%d mediacodec=%d yuv=%d nv12=%d w=%d,h=%d,cw=%d,ch=%d,is_reuse=%d", fmt,
             AV_PIX_FMT_MEDIACODEC, AV_PIX_FMT_YUV420P, AV_PIX_FMT_NV12, dst_width, dst_height, src_width, src_height, is_reuse);
        fmt = get_render_type(is_reuse, fmt);
        switch (fmt) {
            case AV_PIX_FMT_MEDIACODEC:  // 如果是硬解码格式
                LOGE("video-channel.cpp render tag 0-1 decode mediacodec");
                render_frame_hw_callback(frame);  // 需要实现硬解码后的渲染
                break;
            case AV_PIX_FMT_NV12: // 如果是硬解码格式
                LOGE("video-channel.cpp render tag 0-2 decode nv12");
                /*if (!is_create_opengl) {
                    is_create_opengl = true;
                    create_opengl_context(window);
                }
                upload_nv12_to_textures(frame);
                render_frame();
                break;*/
            case AV_PIX_FMT_YUV420P:  // 如果是硬解码格式
                LOGE("video-channel.cpp render tag 0-2 decode yuv420p");
                if (!sws_context) {
                    sws_context = sws_getContext(src_width, src_height, av_codec_ctx->pix_fmt,  // 源宽高和像素格式
                                                 dst_width, dst_height, dst_format,         // 目标宽高和像素格式
                                                 SWS_BICUBLIN, nullptr, nullptr, nullptr);//SWS_BICUBIC
                    if (!sws_context) {
                        LOGE("Failed to initialize SWS context!");
                        return;
                    }
                }

                ret = av_image_alloc(dst_data, dst_line_size, av_codec_ctx->width, av_codec_ctx->height, dst_format, 1);
                if (ret < 0) {
                    LOGE("Failed to allocate memory for converted image!");
                    return;
                }

                ret = sws_scale(sws_context, frame->data, frame->linesize, 0, av_codec_ctx->height, dst_data, dst_line_size);
                if (ret < 0) {
                    LOGE("sws_scale failed with error code %d", ret);
                    av_freep(&dst_data[0]);
                    return;
                }

                render_frame_sw_callback(dst_data[0], dst_line_size[0], av_codec_ctx->width, av_codec_ctx->height);
                av_freep(&dst_data[0]);
                break;
        }

        releaseFrame(&frame);  // 释放当前帧
    }
}

void VideoChannel::play() {
    BaseChannel::play();
}

void VideoChannel::stop() {
    BaseChannel::stop();
}

void VideoChannel::sync() {
    LOGE("video-channel-cpp sync tag 0-1");
}

