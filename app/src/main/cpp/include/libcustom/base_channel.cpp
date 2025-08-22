//
// Created by zyl on 2022/6/22.
//

#include "base_channel.h"

void *thrun_decode(void *args) {
    //LOGE("base-channel-cpp thrun_decode tag 0");
    auto *base_channel = static_cast<BaseChannel *>(args);
    base_channel->decode();
    //LOGE("base-channel-cpp thrun_decode tag 0-end");
    return nullptr;
}

void *thrun_render(void *args) {
    //LOGE("base-channel-cpp thrun_render tag 0");
    auto *base_channel = static_cast<BaseChannel *>(args);
    base_channel->render();
    //LOGE("base-channel-cpp thrun_render tag 0-end");
    return nullptr;
}

void *thrun_c_complete(void *args) {
    //LOGE("base-channel-cpp thrun_render tag 0");
    auto *base_channel = static_cast<BaseChannel *>(args);
    base_channel->check_completion();
    //LOGE("base-channel-cpp thrun_render tag 0-end");
    return nullptr;
}

BaseChannel::BaseChannel(int stream_index, AVCodecContext *av_codec_context,
                         AVRational av_rational) {

    this->av_codec_ctx = av_codec_context;
    this->stream_index = stream_index;
    this->av_rational = av_rational;

    packets.setReleaseCallback(releasePacket);
    packets.setSyncHandle(dropPackets);

    frames.setReleaseCallback(releaseFrame);
    frames.setSyncHandle(dropFrames);
}

BaseChannel::~BaseChannel() {
    if (av_codec_ctx) {
        avcodec_close(av_codec_ctx);
        av_codec_ctx = nullptr;
    }
}

void BaseChannel::releasePacket(AVPacket **packet) {
    if (packet) {
        av_packet_free(packet);
        *packet = nullptr;
    }
}

void BaseChannel::releaseFrame(AVFrame **frame) {
    if (frame) {//指针的指针能够修改传递进来的指针的指向
        av_frame_free(frame);
        *frame = nullptr;
    }
}

void BaseChannel::syncFrame(queue<AVFrame *> &q) {
    if (!q.empty()) {
        AVFrame *frame = q.front();
        releaseFrame(&frame);
        q.pop();
    }
}

void BaseChannel::stop() {
    //LOGE("base-channel-cpp stop tag 0 index=%d", stream_index);
    is_playing = false;

    //LOGE("base-channel-cpp stop tag 0-1");
    stopWork();

    //LOGE("base-channel-cpp stop tag 1 pid_decode=%ld", pid_decode);
    if (pid_decode != 0) {
        pthread_join(pid_decode, nullptr);
        pid_decode = 0;
        //LOGE("base-channel-cpp stop tag 1-1");
    }
    //LOGE("base-channel-cpp stop tag 2 pid_render=%ld", pid_render);
    if (pid_render != 0) {
        pthread_join(pid_render, nullptr);
        pid_render = 0;
        //LOGE("base-channel-cpp stop tag 2-1");
    }

    //LOGE("base-channel-cpp stop tag 3 pid_check=%ld", pid_check);
    if (pid_check != 0) {
        pthread_join(pid_check, nullptr);
        pid_check = 0;
        //LOGE("base-channel-cpp stop tag 3-1");
    }

    //LOGE("base-channel-cpp stop tag 4");
    cleanQueue();

    //LOGE("base-channel-cpp stop tag 5-end");
}

void BaseChannel::play() {
    //LOGE("base-channel-cpp play tag 0");
    is_playing = true;
    sync_count = 0;
    play_second = 0;
    ren_start_time = 0;
    ren_def_diff = 0;
    total_playback_delay = 0;

    pthread_create(&pid_decode, nullptr, thrun_decode, this);
    pthread_create(&pid_render, nullptr, thrun_render, this);
    pthread_create(&pid_check, nullptr, thrun_c_complete, this);
    //LOGE("base-channel-cpp play tag 1-end");
}

void BaseChannel::decode() {
    //LOGE("base-channel-cpp decode tag 0-0 start is_video=%d", is_video_type);
    avcodec_flush_buffers(av_codec_ctx);
    AVPacket *packet = nullptr;
    bool is_start_decode = true;
    dec_def_diff = 0;

    while (is_playing) {
        //LOGE("base-channel-cpp decode tag 1-0 while start is_video=%d v_diff=%f", is_video_type, v_clock_diff);

        if (!packets.pop(packet)) {
            //LOGE("base-channel-cpp decode tag 1-1 packet pop failed is_video=%d", is_video_type);
            continue;
        }

        if (ren_start_time == 0) {
            ren_start_time = av_gettime() / 1000000.0;
        }

        if (is_start_decode) {
            if (!(packet->flags & AV_PKT_FLAG_KEY)) {
                //LOGE("base-channel-cpp decode tag 1-2 skip non-keyframe");
                releasePacket(&packet);
                continue;
            }
            is_start_decode = false;
            //LOGE("base-channel-cpp decode tag 1-4 got first keyframe");
        }

        this->pts_second = static_cast<double>(packet->pts) * av_q2d(av_rational);
        if (dec_def_diff == 0) {
            dec_def_diff = getSysDiff(this->pts_second);
            //LOGE("base-channel-cpp decode tag 1-3 dec_def_diff=%f is_video=%d", dec_def_diff, is_video_type);
        }

        double diff = dec_def_diff - getSysDiff(this->pts_second);
        //LOGE("base-channel-cpp decode tag 1-4 diff=%f is_video=%d size=%d", diff, is_video_type, packets.size());

        if (diff > 5) {
            //LOGE("base-channel-cpp decode tag 1-5 diff drop packet... is_video=%d", is_video_type);
            dec_def_diff = 0;
            releasePacket(&packet);
            continue;
        }

        this->cur_diff = diff;
        if (diff > 0 && diff < 2) {
            if (v_clock_diff > 0.1) {
                av_usleep(diff * 1000000 * 0.2);
            }
        } else if (diff < -0.1) {
            //LOGE("base-channel-cpp decode tag 1-8 drop frame... is_video=%d", is_video_type);
            releasePacket(&packet);
            if (is_video_type) {
                jcallback->onCallback(THREAD_CHILD, FFMPEG_PACKET_TIMEOUT, FFMPEG_PACKET_STREAM_EXCEPTION);
            }
            continue;
        }

        int64_t start_decode_time = av_gettime_relative();
        int ret = avcodec_send_packet(av_codec_ctx, packet);
        //LOGE("base-channel-cpp decode tag 1-10 send_packet ret=%d is_video=%d", ret, is_video_type);
        releasePacket(&packet);

        if (ret == 0) {
            //LOGE("base-channel-cpp decode tag 1-11 start process frames is_video=%d", is_video_type);
            while (is_playing) {
                AVFrame *frame = av_frame_alloc();
                ret = avcodec_receive_frame(av_codec_ctx, frame);
                //LOGE("base-channel-cpp decode tag 3-5-0------ receive_frame ret=%d is_video=%d", ret, is_video_type);

                if (ret == 0) {
                    if (frame->flags & AV_FRAME_FLAG_CORRUPT) {
                        //LOGE("base-channel-cpp decode tag 3-2 corrupt frame is_video=%d", is_video_type);
                        avcodec_flush_buffers(av_codec_ctx);
                        releaseFrame(&frame);
                        break;
                    }

                    int64_t end_decode_time = av_gettime_relative();
                    double decode_cost_ms = (end_decode_time - start_decode_time) / 1000.0;
                    //LOGE("base-channel-cpp decode tag 3-5-1 is_video=%d decode_cost_ms=%f", is_video_type, decode_cost_ms);

                    frames.push(frame);
                    int size = frames.size();
                    //LOGE("base-channel-cpp decode tag 3-3 is_video=%d frame size=%d", is_video_type, size);
                    if (is_playing && frames.size() > 50) {
                        av_usleep(10);
                    }
                } else {
                    //LOGE("base-channel-cpp decode tag 3-4 receive failed ret=%d is_video=%d", ret, is_video_type);
                    releaseFrame(&frame);
                    break;
                }
            }

            if (ret == AVERROR(EAGAIN)) {
                //LOGE("base-channel-cpp decode tag 1-12 decoder needs more frames is_video=%d", is_video_type);
                continue;
            }
        } else if (ret == AVERROR(EAGAIN)) {
            //LOGE("base-channel-cpp decode tag 1-12 decoder needs more frames is_video=%d", is_video_type);
            continue;
        } else if (ret == AVERROR_EOF) {
            //LOGE("base-channel-cpp decode tag 1-13 eof reached is_video=%d", is_video_type);
            avcodec_send_packet(av_codec_ctx, nullptr);
            break;
        }
    }

    if (packet) {
        //LOGE("base-channel-cpp decode tag 2-0 release last packet is_video=%d", is_video_type);
        releasePacket(&packet);
    }

    cleanQueue();

    if (is_video_type) {
        //LOGE("base-channel-cpp decode tag 2-1 detach callback is_video=%d", is_video_type);
        jcallback->detach();
    }
    //LOGE("base-channel-cpp decode tag 2-2 end is_video=%d", is_video_type);
}

void BaseChannel::dropPackets(queue<AVPacket *> &q) {
    while (!q.empty()) {
        AVPacket *packet = q.front();
        if (packet->flags != AV_PKT_FLAG_KEY) {
            releasePacket(&packet);
            q.pop();
            //LOGE("base-channel-cpp drop packets.......");
        } else {
            break;
        }
    }
}

void BaseChannel::dropFrames(queue<AVFrame *> &q) {
    while (!q.empty()) {
        AVFrame *frame = q.front();
        releaseFrame(&frame);
        /*AVFrame *frame = q.front();
        if (frame->flags != AV_FRAME_FLAG_KEY) {
            releaseFrame(&frame);
            q.pop();
            //LOGE("base-channel-cpp drop frames.......");
        } else {
            break;
        }*/
    }
}

