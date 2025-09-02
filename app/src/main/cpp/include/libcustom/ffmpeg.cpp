//
// Created by zyl on 2022/6/23.
//

#include "ffmpeg.h"

enum AVPixelFormat get_hw_format(AVCodecContext *ctx, const enum AVPixelFormat *pix_fmts) {
    while (*pix_fmts != -1) {
        if (*pix_fmts == AV_PIX_FMT_NV12 || *pix_fmts == AV_PIX_FMT_MEDIACODEC) {
            return *pix_fmts;
        }
        pix_fmts++;
    }
    return AV_PIX_FMT_NONE;
}

int hw_decoder_init(AVCodecContext *ctx, const enum AVHWDeviceType type) {
    AVBufferRef *hw_device_ctx = nullptr;
    int ret = av_hwdevice_ctx_create(&hw_device_ctx, type, nullptr, nullptr, 0);
    if (ret < 0) {
        LOGE("ffmpeg-cpp Failed to create specified HW device");
        return ret;
    }

    LOGE("ffmpeg-cpp Success to create specified HW device");
    ctx->hw_device_ctx = av_buffer_ref(hw_device_ctx);
    //ctx->hw_device_ctx = av_hwdevice_ctx_alloc(AV_HWDEVICE_TYPE_MEDIACODEC);
    return ret;
}

int interrupt_timeout(void *ctx) {
    FFmpeg *f = (FFmpeg *) ctx;
    LOGE("ffmpeg-cpp timeout player status=%d", f->player_status);
    if (f->player_status == STOP) {
        LOGE("ffmpeg-cpp _prepare timeout player stop...");
        return 1;
    }

    if (f->player_status == PLAYING) {
        return 0;
    }

    InterruptContext ic = f->interrupt_ctx;
    int64_t elapsed_time = av_gettime() / 1000 - ic.start_time;
    LOGE("ffmpeg-cpp _prepare timeout=%lld timeout_ms=%d", elapsed_time, ic.timeout_ms);

    bool t = elapsed_time > ic.timeout_ms;
    LOGE("ffmpeg-cpp _prepare timeout tag 0-2 t=%d", t);
    return t; //返回 1:表示停止，0:表示继续
}

AVInputFormat *get_av_input_format(const char *url, int type) {
    LOGE("ffmpeg-cpp _prepare get_av_input_format tag 0-0 url=%s type=%d", url, type);
    AVInputFormat *input_format = nullptr;
    const char *short_name = SHORT_NAME_MPEGTS;

    LOGE("ffmpeg-cpp _prepare get_av_input_format tag 0-1");
    if (strstr(RTP_ADDRESS, url) != nullptr) {
        short_name = SHORT_NAME_RTP;
    }

    if (type == 1) {
        LOGE("ffmpeg-cpp _prepare get_av_input_format tag 0-2");
        input_format = const_cast<AVInputFormat *>(av_find_input_format(short_name));
    }
    LOGE("ffmpeg-cpp _prepare get_av_input_format tag 0-3 name=%s", short_name);
    return input_format;
}

int64_t convert_timestamp_to_int64(double timestamp) {
    // 提取秒部分和毫秒部分
    time_t seconds = (time_t) timestamp;  // 整数部分，表示秒
    int64_t milliseconds = (int64_t) ((timestamp - seconds) * 1000);  // 小数部分，表示毫秒

    struct tm *time_info;

    // 使用 gmtime() 将时间戳转换为 UTC 时间
    time_info = gmtime(&seconds);

    // 组装成格式：YYYYMMDDHHMMSS
    int year = time_info->tm_year + 1900;
    int month = time_info->tm_mon + 1;
    int day = time_info->tm_mday;
    int hour = time_info->tm_hour;
    int minute = time_info->tm_min;
    int second = time_info->tm_sec;

    // 将年、月、日、时、分、秒合并为一个 int64 型数值
    // 假设格式：YYYYMMDDHHMMSS
    int64_t result = (int64_t) year * 10000000000LL + (int64_t) month * 100000000LL + (int64_t) day * 1000000LL
                     + (int64_t) hour * 10000LL + (int64_t) minute * 100LL + second;

    // 处理毫秒部分，可以选择将毫秒加到末尾，或者根据需求调整
    //result = result * 1000 + milliseconds;  // 将毫秒部分加入到结果中

    return result;
}

unsigned short get_random_port() {
    // 获取当前时间戳（秒级）
    time_t current_time = time(NULL);

    // 获取一个随机数，确保随机种子是不同的
    srand((unsigned int) (current_time + rand()));  // 使用当前时间 + 随机数作为随机数种子

    // 生成一个 0 到 999 的随机数
    unsigned short random_part = rand() % 1000;

    // 使用时间戳和随机数的结合，确保端口在有效范围内
    unsigned short port = (unsigned short) (
            (current_time + random_part) % (MAX_PORT - MIN_PORT + 1) + MIN_PORT);

    return port;
}

char *extract_rtsp_base_url(const char *url) {
    const char *protocol_end = strstr(url, "://");

    if (protocol_end != NULL) {
        size_t protocol_len = protocol_end - url;
        const char *path_start = strchr(protocol_end + 3, '/');  // 跳过 "://"
        size_t base_url_size = protocol_len + 3 + 256; // 协议部分 + "://" + 域名和端口部分
        char *base_url = (char *) malloc(base_url_size);
        if (!base_url) {
            return NULL;  // 内存分配失败
        }
        strncpy(base_url, url, protocol_len);
        base_url[protocol_len] = '\0';  // 添加结束符
        strcat(base_url, "://");  // 拼接 "://"
        if (path_start != NULL) {
            size_t len = path_start - (protocol_end + 3);  // 计算域名和端口的长度
            strncat(base_url, protocol_end + 3, len);  // 拼接域名和端口
        } else {
            strcat(base_url, protocol_end + 3);
        }
        return base_url;  // 返回动态分配的 base_url
    } else {
        return NULL;  // 无效的 URL
    }
}

int is_video_packet(AVPacket *packet, AVFormatContext *fmt_ctx) {
    if (packet->stream_index >= 0 && packet->stream_index < fmt_ctx->nb_streams) {
        const AVStream *stream = fmt_ctx->streams[packet->stream_index];
        return stream->codecpar->codec_type == AVMEDIA_TYPE_VIDEO;
    }
    return 0;
}

AVDictionary *init_options() {
    AVDictionary *options = nullptr;
    // RTSP 相关
    av_dict_set(&options, "rtsp_transport", "tcp", 0);         // TCP 模式
    av_dict_set(&options, "rtsp_flags", "prefer_tcp", 0);      // 优先 TCP
    av_dict_set(&options, "timeout", "5000000", 0);            // 超时 5 秒
    av_dict_set(&options, "reconnect", "1", 0);                // 自动重连
    av_dict_set(&options, "reconnect_streamed", "1", 0);       // 流中断也重连
    av_dict_set(&options, "reconnect_delay_max", "30", 0);     // 最大延迟 30s

    // 缓冲区相关
    av_dict_set(&options, "buffer_size", "1048576", 0);        // 缓冲区大小 1MB
    av_dict_set(&options, "probesize", "1024000", 0);          // 探测大小 1MB
    av_dict_set(&options, "max_delay", "50000", 0);            // 最大延迟 50ms
    av_dict_set(&options, "flags", "+low_delay", 0);
    av_dict_set(&options, "infbuf", "1", 0);                   // 允许无限缓冲

    // 硬件加速（请根据平台修改）
    av_dict_set(&options, "hwaccel", "mediacodec", 0);         // Android
    av_dict_set(&options, "hwaccel_output_format", "mediacodec", 0); // 输出格式为 MediaCodec

    // 解码性能优化
    av_dict_set(&options, "tune", "zerolatency", 0);           // 零延迟模式
    av_dict_set(&options, "skip_loop_filter", "1", 0);         // 忽略滤波器
    av_dict_set(&options, "skip_frame", "nokey", 0);           // 只解 I 帧
    av_dict_set(&options, "framedrop", "1", 0);                // 丢帧优化
    av_dict_set(&options, "vsync", "0", 0);                    // 禁用 vsync
    av_dict_set(&options, "threads", "auto", 0);               // 自动线程数

    // DNS & User-Agent
    av_dict_set(&options, "dns_cache_clear", "2", 0);
    av_dict_set(&options, "dns_cache_timeout", "60", 0);
    av_dict_set(&options, "user_agent", "Iptv-Agent/1.0", 0);

    return options;
}

void *thrun_prepare(void *args) {
    LOGE("ffmpeg-cpp thrun_prepare tag 1");
    auto *ffmpeg = static_cast<FFmpeg *>(args);
    pthread_mutex_lock(&ffmpeg->mutex);
    ffmpeg->_prepare();
    ffmpeg->callback->detach();
    LOGE("ffmpeg-cpp thrun_prepare tag 2-end...");
    pthread_mutex_unlock(&ffmpeg->mutex);
    return nullptr;
}

void *thrun_decode_packet(void *args) {
    LOGE("ffmpeg-cpp thrun_decode_packet tag 1");
    auto *ffmpeg = static_cast<FFmpeg *>(args);
    ffmpeg->_decode();
    LOGE("ffmpeg-cpp thrun_decode_packet tag 2-end");
    return nullptr;
}

FFmpeg::FFmpeg(Callback *callback) {
    this->callback = callback;
    pthread_mutex_init(&seek_mutex_t, nullptr);
}

void FFmpeg::setRenderFrameCallback(RenderFrameSwCallback callback) {
    LOGE("ffmpeg-cpp setRenderFrameCallback tag 0");
    if (video_channel) {
        video_channel->setRenderFrameCallback(callback);
    }
    LOGE("ffmpeg-cpp setRenderFrameCallback tag 0-end");
}

void FFmpeg::setRenderFrameMediaCodecCallback(RenderFrameHwCallback callback) {
    LOGE("ffmpeg-cpp setRenderFrameMediaCodecCallback tag 0");
    if (video_channel) {
        video_channel->setRenderFrameMediaCodecCallback(callback);
    }
    LOGE("ffmpeg-cpp setRenderFrameMediaCodecCallback tag 0-end");
}

char *FFmpeg::prepare_url(int *flag) {
    LOGE("ffmpeg-cpp _prepare tag 2-0 flag=%d", flag);
    switch (*flag) {
        case 0://单播
            interrupt_ctx = {av_gettime() / 1000, 6000};
            break;
        case 1://组播
            interrupt_ctx = {av_gettime() / 1000, 6000};
            break;
        case 2://fcc
            interrupt_ctx = {av_gettime() / 1000, 6000};
            break;
    }

    char *url = (*flag == 0) ? data_source : multicast_url;
    return url;
}

int FFmpeg::_prepare() {
    player_status = PREPARE;
    is_finished = false;

    if (!this->surface) {
        LOGE("ffmpeg-cpp _prepare tag 0 surface is null..., return");
        return 0;
    }

    LOGE("ffmpeg-cpp _prepare tag 1 start...");
    int64_t start_time = av_gettime();
    int ret = avformat_network_init();
    LOGE("ffmpeg-cpp _prepare tag 1 network=%s stb_id=%s", av_err2str(ret), stb_ip);

    // 如果 flag != 2，则表示是封装流，使用 avformat_open_input 打开流
    av_format_ctx = avformat_alloc_context();
    if (!av_format_ctx) {
        LOGE("ffmpeg-cpp _prepare tag 1-1 format context fail!");
        is_finished = true;
        return 0;
    }

    LOGE("ffmpeg-cpp _prepare tag 1-0 stream_mode=%d", stream_mode);
    int model = 2;//0:单播;/1:组播;2:fcc
    char *url = prepare_url(&model);
    if (!url) {
        LOGE("ffmpeg-cpp _prepare tag 2-0-0 url is null...");
        return 0; // 或者其他错误处理逻辑
    }

    LOGE("ffmpeg-cpp _prepare tag 2-0-1 model=%d, url=%s", model, url);
    this->cur_data_source = strdup(url);
    av_format_ctx->seek_timestamp_abt = timestamp_abt ? strdup(timestamp_abt) : nullptr;
    SAFE_FREE_STRING(timestamp_abt);

    if (av_format_ctx->seek_timestamp_abt && play_type == 2) {
        char *temp = extract_rtsp_base_url(url);//rtsp://123.147.112.17:8089
        LOGE("ffmpeg-cpp _prepare tag 2-0-2 temp url=%s", temp);
        SAFE_DELETE_OBJECT(video_channel);
    }

    //av_format_ctx->interrupt_callback.callback = interrupt_timeout;
    //av_format_ctx->interrupt_callback.opaque = this;
    LOGE("ffmpeg-cpp _prepare tag 2-0 seek_timestamp_abt=%s", av_format_ctx->seek_timestamp_abt);

    AVDictionary *options = init_options();
    av_dict_set(&options, "fcc_server_ip", fcc_server_ip, 0);
    av_dict_set(&options, "fcc_server_port", fcc_server_port, 0);
    av_dict_set(&options, "stb_id", stb_id, 0);
    av_dict_set(&options, "stb_ip", stb_ip, 0);

    LOGE("ffmpeg-cpp _prepare tag 2-1-0 open source fcc_server_ip=%s", fcc_server_ip);
    LOGE("ffmpeg-cpp _prepare tag 2-1-0 open source fcc_server_port=%s", fcc_server_port);
    LOGE("ffmpeg-cpp _prepare tag 2-1-0 open source stb_id=%s,stb_ip=%s", stb_id, stb_ip);
    LOGE("ffmpeg-cpp _prepare tag 2-1-0 open source mul enable=%d", is_mul_enable);

    AVInputFormat *input_format = is_mul_enable ? nullptr : get_av_input_format(url, play_type);

    ret = avformat_open_input(&av_format_ctx, url, input_format, &options);
    LOGE("ffmpeg-cpp _prepare tag 2-1 open source ret=%d url=%s", ret, url);

    int r_code = (model == 2 && ret >= 0) ? FFMPEG_FCC_RESPONSE_SUCCESS : FFMPEG_FCC_RESPONSE_FAIl;
    const char *text = (model == 2 && ret >= 0) ? "FCC请求成功" : "FCC请求失败";
    this->callback->onCallback(THREAD_CHILD, r_code, text);

    if (model == 2 && ret == -1000) {
        _prepare();
        is_finished = true;
        return 0;
    }

    int rep = 0;//重试次数
    while (ret < 0) {
        LOGE("ffmpeg-cpp _prepare tag 2-1-1 open source ret=%d", ret);
        const char *msg = av_err2str(ret);
        if (rep > 0) {
            LOGE("ffmpeg-cpp _prepare tag 2-1-2 open source ret=%d msg=%s", ret, msg);
            if (strstr(msg, "Immediate exit requested") != nullptr) {
                LOGE("ffmpeg-cpp _prepare tag 2-1-3 return...");
                is_finished = true;
                this->callback->onCallback(THREAD_CHILD, FFMPEG_CAN_NOT_OPEN_URL, msg);
                return 0;
            }

            rep--;
            av_usleep(100000);
            ret = avformat_open_input(&av_format_ctx, url, nullptr, &options);
            LOGE("ffmpeg-cpp _prepare tag 2-1-4 open source ret=%d", ret);
            continue;
        }

        is_finished = true;
        LOGE("ffmpeg-cpp _prepare tag 3-1 FFMPEG_CAN_NOT_OPEN_URL msg=%s", msg);
        this->callback->onCallback(THREAD_CHILD, FFMPEG_CAN_NOT_OPEN_URL, msg);
        return 0;
    }

    LOGE("ffmpeg-cpp _prepare tag 4-0 start_time=%lld", start_time);
    ret = avformat_find_stream_info(av_format_ctx, nullptr);

    //av_dump_format(av_format_ctx, 0, url, 0);
    av_dict_free(&options);

    int64_t time_diff_ms = (av_gettime() - start_time) / 1000;
    LOGE("ffmpeg-cpp _prepare tag 4-1-2 time=%lld", time_diff_ms);

    if (player_status == STOP) {
        LOGE("ffmpeg-cpp _prepare tag 4-1-3 time=%lld", time_diff_ms);
        is_finished = true;
        return 0;
    }

    if (ret < 0) {
        LOGE("ffmpeg-cpp _prepare tag 4-1-2-0 FFMPEG_CAN_NOT_FIND_STREAMS");
        this->callback->onCallback(THREAD_CHILD, FFMPEG_CAN_NOT_FIND_STREAMS, av_err2str(ret));
        is_finished = true;
        return 0;
    }

    if (seek_position > 0 && (play_type == 4 || play_type == 3)) {
        LOGE("ffmpeg-cpp _prepare tag 4-1 Seek position=%lld", seek_position);
        int64_t timestamp = seek_position * AV_TIME_BASE; // 例如，30秒
        ret = av_seek_frame(av_format_ctx, -1, timestamp, AVSEEK_FLAG_BACKWARD);
        if (ret < 0) {
            LOGE("ffmpeg-cpp _prepare tag 4-2 Seek failed: %s", av_err2str(ret));
            this->callback->onCallback(THREAD_CHILD, FFMPEG_SEEK_FAILED_CODE, av_err2str(ret));
            is_finished = true;
            return 0;
        }

        LOGE("ffmpeg-cpp _prepare tag 4-2 Seek successful to %lld", seek_position);
    }

    this->_duration = (double) av_format_ctx->duration / AV_TIME_BASE;
    LOGE("ffmpeg-cpp _prepare tag 5 duration=%f,nb_streams=%d", _duration, av_format_ctx->nb_streams);

    for (int i = 0; i < av_format_ctx->nb_streams; i++) {
        AVStream *av_stream = av_format_ctx->streams[i];   //媒体流
        AVCodecParameters *parameters = av_stream->codecpar;  //编码解码的参数

        const char *code_name = avcodec_get_name(parameters->codec_id);
        LOGE("ffmpeg-cpp _prepare tag 5-0--for stream_index=%d,code_name=%s,nb_streams=%d", i, code_name, av_format_ctx->nb_streams);

        const AVCodec *codec = nullptr;
        if ((parameters->codec_id == AV_CODEC_ID_H264 || parameters->codec_id == AV_CODEC_ID_HEVC) && mediacodec) {
            if (parameters->codec_id == AV_CODEC_ID_H264) {
                LOGE("ffmpeg-cpp _prepare tag 5-1 find decoder by h264_mediacodec");
                codec = avcodec_find_decoder_by_name("h264_mediacodec");
            } else if (parameters->codec_id == AV_CODEC_ID_HEVC) { // 对于HEVC的情况
                LOGE("ffmpeg-cpp _prepare tag 5-1 find decoder by hevc_mediacodec");
                codec = avcodec_find_decoder_by_name("hevc_mediacodec"); // 确保有对应的硬件解码器名称
            }

            LOGE("ffmpeg-cpp _prepare tag 5-1-2 find decoder config hw codec");
            enum AVHWDeviceType type = av_hwdevice_find_type_by_name("mediacodec");
            for (int j = 0;; j++) {// 配置硬解码
                const AVCodecHWConfig *config = avcodec_get_hw_config(codec, j);
                if (!config) {
                    LOGE("ffmpeg-cpp _prepare Decoder %s does not support device type %s.\n", codec->name, av_hwdevice_get_type_name(type));
                    break;
                } else if (config->methods & AV_CODEC_HW_CONFIG_METHOD_HW_DEVICE_CTX &&
                           config->device_type == type) {
                    hw_pix_fmt = config->pix_fmt;
                    const char *fmt = av_get_pix_fmt_name(static_cast<AVPixelFormat>(hw_pix_fmt));
                    LOGE("ffmpeg-cpp _prepare tag 5-1-3 find decoder config hw codec success config_fmt=%s", fmt);
                    break;
                }
            }

        } else if (parameters->codec_id == AV_CODEC_ID_MPEG4) {
            LOGE("ffmpeg-cpp _prepare tag 5-2 find decoder by mpeg4_mediacodec");
            codec = avcodec_find_decoder_by_name("mpeg4_mediacodec");
        } else {
            LOGE("ffmpeg-cpp _prepare tag 5-3-0 find other decoder");
            codec = avcodec_find_decoder(parameters->codec_id);//解码器
        }

        if (!codec) {
            LOGE("ffmpeg-cpp _prepare tag 5-3-1 FFMPEG_FIND_DECODER_FAIL");
            this->callback->onCallback(THREAD_CHILD, FFMPEG_FIND_DECODER_FAIL, av_err2str(ret));
            is_finished = true;
            return 0;
        }

        LOGE("ffmpeg-cpp _prepare tag 5-4 decoder name=%s pw=%d,ph=%d", codec->name, parameters->width, parameters->height);
        bool is_reuse = false;
        AVCodecContext *av_codec_ctx = nullptr;
        if (video_channel && mediacodec && parameters->codec_type == AVMEDIA_TYPE_VIDEO
            && (parameters->codec_id == AV_CODEC_ID_H264 || parameters->codec_id == AV_CODEC_ID_HEVC)) {
            LOGE("ffmpeg-cpp _prepare tag 5-4-0 reuse av_codec_ctx");
            av_codec_ctx = video_channel->av_codec_ctx;
            av_codec_ctx->codec = codec;
            is_reuse = true;
            LOGE("ffmpeg-cpp _prepare tag 5-4-0-0 reuse av_codec_ctx");
        } else {
            av_codec_ctx = avcodec_alloc_context3(codec);
        }

        if (!av_codec_ctx) {
            LOGE("ffmpeg-cpp _prepare tag 5-4-1 FFMPEG_ALLOC_CODEC_CONTEXT_FAIL");
            this->callback->onCallback(THREAD_CHILD, FFMPEG_ALLOC_CODEC_CONTEXT_FAIL, av_err2str(ret));
            is_finished = true;
            return 0;
        }

        if (avcodec_is_open(av_codec_ctx)) {
            LOGE("ffmpeg-cpp _prepare tag 5-5-0");
            avcodec_flush_buffers(av_codec_ctx);
        }

        if (mediacodec) {
            av_codec_ctx->lowres = 1;
        }

        ret = avcodec_parameters_to_context(av_codec_ctx, parameters);
        if (ret < 0) {
            LOGE("ffmpeg-cpp _prepare tag 5-5-1 FFMPEG_CODEC_CONTEXT_PARAMETERS_FAIL");
            this->callback->onCallback(THREAD_CHILD, FFMPEG_CODEC_CONTEXT_PARAMETERS_FAIL, av_err2str(ret));
            is_finished = true;
            return 0;
        }

        LOGE("ffmpeg-cpp _prepare tag 6 parameters to codec_ctx codec thread count=%d", av_codec_ctx->thread_count);
        av_codec_ctx->flags |= AV_CODEC_FLAG_LOW_DELAY;
        av_codec_ctx->flags2 |= AV_CODEC_FLAG2_FAST;
        av_codec_ctx->thread_type = FF_THREAD_SLICE;

        if (parameters->codec_type == AVMEDIA_TYPE_VIDEO && hw_pix_fmt != AV_PIX_FMT_NONE) {
            int t_count = av_codec_ctx->thread_count;
            LOGE("ffmpeg-cpp _prepare tag 6-1 hw_decoder_init count=%d aw=%d,ah=%d", t_count, av_codec_ctx->width, av_codec_ctx->height);
            av_codec_ctx->get_format = get_hw_format;
            ret = hw_decoder_init(av_codec_ctx, AV_HWDEVICE_TYPE_MEDIACODEC);
            if (ret < 0) {
                LOGE("ffmpeg-cpp _prepare tag 6-2 hw_decoder_init Fail !!!");
                this->callback->onCallback(THREAD_CHILD, FFMPEG_OPEN_DECODER_FAIL, av_err2str(ret));
                is_finished = true;
                return 0;
            }

            LOGE("ffmpeg-cpp _prepare tag 6-2 av_media_code_context init");
            AVMediaCodecContext *av_media_code_context = av_mediacodec_alloc_context();
            LOGE("ffmpeg-cpp _prepare tag 6-3 av_mediacodec_default_init bind surface");
            av_mediacodec_default_init(av_codec_ctx, av_media_code_context, surface);
        }

        ret = avcodec_is_open(av_codec_ctx);
        LOGE("ffmpeg-cpp _prepare tag 6-4 avcodec_is_open ret=%d", ret);
        if (!ret) {
            ret = avcodec_open2(av_codec_ctx, codec, nullptr);
        }

        if (ret < 0) {
            char error_buf[128];
            av_strerror(ret, error_buf, sizeof(error_buf));
            LOGE("ffmpeg-cpp _prepare tag 6-4-1 FFMPEG_OPEN_DECODER_FAIL ret=%d, %s", ret, error_buf);
            this->callback->onCallback(THREAD_CHILD, FFMPEG_OPEN_DECODER_FAIL, av_err2str(ret));
            is_finished = true;
            return 0;
        }

        LOGE("ffmpeg-cpp _prepare tag 7 open codec ret=%d", ret);
        AVRational time_base = av_stream->time_base;
        LOGE("ffmpeg-cpp _prepare tag 8 av_stream time_base num=%d den=%d", time_base.num, time_base.den);

        if (parameters->codec_type == AVMEDIA_TYPE_VIDEO) {
            double fps = av_q2d(av_stream->avg_frame_rate);
            LOGE("ffmpeg-cpp _prepare tag 9-1 codec type video fps=%f", fps);
            if (!video_channel || video_channel == nullptr) {
                video_channel = new VideoChannel(i, av_codec_ctx, time_base);
            }
            video_channel->av_format_ctx = av_format_ctx;
            video_channel->seek_position = seek_position;
            video_channel->stream_index = i;
            video_channel->is_reuse = is_reuse;
            video_channel->av_codec_ctx = av_codec_ctx;
            video_channel->av_rational = time_base;
            video_channel->is_video_type = true;
            video_channel->is_mediacodec = mediacodec;
            video_channel->play_type = play_type;
            video_channel->jcallback = this->callback;
            video_channel->is_mul_enable = is_mul_enable;
            video_channel->set_window(window);
        } else if (parameters->codec_type == AVMEDIA_TYPE_AUDIO) {
            LOGE("ffmpeg-cpp _prepare tag 9-2 codec type audio index=%d", i);
            AudioChannel *channel = new AudioChannel(i, av_codec_ctx, time_base, _duration);
            channel->av_format_ctx = av_format_ctx;
            channel->stream_index = i;
            channel->av_codec_ctx = av_codec_ctx;
            channel->av_rational = time_base;
            channel->seek_position = seek_position;
            channel->duration = _duration;
            channel->is_mediacodec = mediacodec;
            channel->play_type = play_type;
            channel->jcallback = this->callback;
            channel->is_mul_enable = is_mul_enable;

            if (!audio_channel || audio_channel == nullptr) {
                audio_channel = channel;
            } else {
                audio_channel2 = channel;
            }
        }
    }

    LOGE("ffmpeg-cpp _prepare tag 10");
    if (!audio_channel && !video_channel) {
        LOGE("ffmpeg-cpp av find fail !!!");
        const char *msg = "未找到音视频流";
        this->callback->onCallback(THREAD_CHILD, FFMPEG_NOMEDIA, msg);
        is_finished = true;
        return 0;
    }


    time_diff_ms = (av_gettime() - start_time) / 1000;
    LOGE("ffmpeg-cpp _prepare tag 4-1-3 _prepare total time=%lld", time_diff_ms);
    LOGE("ffmpeg-cpp _prepare tag 11-end prepare finish!");
    is_finished = true;
    this->callback->onPrepare(THREAD_CHILD); //准备完了 通知java 你随时可以开始播放
    return 1;
}

void FFmpeg::_decode() {
    LOGE("ffmpeg-cpp _decode tag 0-0");
    if (video_channel && audio_channel) {
        LOGE("ffmpeg-cpp _decode tag 0-1");
        int v_index = video_channel->stream_index;
        int a_index = audio_channel->stream_index;
        int a_index2 = audio_channel2 == nullptr ? -1 : audio_channel2->stream_index;

        AVRational v_rational = av_format_ctx->streams[v_index]->time_base;
        AVRational a_rational = av_format_ctx->streams[a_index]->time_base;

        player_status = PLAYING;
        AVPacket *packet = nullptr;
        int ret = 0;
        double second = 0;
        LOGE("ffmpeg-cpp _decode tag 0-2");
        while (player_status == PLAYING) {
            packet = av_packet_alloc();
            LOGE("ffmpeg-cpp _decode tag 0-3");
            ret = av_read_frame(av_format_ctx, packet);
            LOGE("ffmpeg-cpp _decode tag 0-4 ret=%d", ret);
            if (ret == 0) {
                int index = packet->stream_index;
                LOGE("ffmpeg-cpp _decode tag 0-1 stream_index=%d, is_video=%d", index, (index == v_index));

                if (index == v_index) {
                    second = static_cast<double>(packet->pts) * av_q2d(v_rational);
                    //LOGV("ffmpeg-cpp _decode tag 0-1 second=%f get_position=%lld", second, video_channel->seek_position);
                    if (second >= video_channel->seek_position) {
                        int size = video_channel->packets.size();
                        LOGE("ffmpeg-cpp _decode tag 0-2 video size=%d", size);
                        if (size > 50 && player_status == PLAYING) {
                            av_usleep(10);
                        }
                        video_channel->packets.push(packet);
                    }
                } else if (index == a_index) {
                    second = static_cast<double>(packet->pts) * av_q2d(a_rational);
                    if (second >= audio_channel->seek_position) {
                        int size = audio_channel->packets.size();
                        LOGE("ffmpeg-cpp _decode tag 0-2 audio size=%d", size);
                        if (size > 50 && player_status == PLAYING) {
                            av_usleep(10);
                        }
                        audio_channel->packets.push(packet);
                    }
                } else if (index == a_index2) {
                    second = static_cast<double>(packet->pts) * av_q2d(a_rational);
                    if (second >= audio_channel2->seek_position) {
                        int size = audio_channel2->packets.size();
                        LOGE("ffmpeg-cpp _decode tag 0-2 audio2 size=%d", size);
                        if (size > 50 && player_status == PLAYING) {
                            av_usleep(10);
                        }
                        audio_channel2->packets.push(packet);
                    }
                }
            } else if (ret == AVERROR(EAGAIN)) {
                av_packet_unref(packet);
                continue;
            } else if (ret == AVERROR_EOF) {
                av_packet_free(&packet);
                LOGE("ffmpeg-cpp _decode tag 0-eof, read eof...");
                break;
            }
        }

        video_channel->packets.clear(0);
        audio_channel->packets.clear(0);

        if (audio_channel2) {
            audio_channel2->packets.clear(0);
            audio_channel2->dec_def_diff = 0;
        }

        video_channel->dec_def_diff = 0;
        audio_channel->dec_def_diff = 0;

        LOGE("ffmpeg-cpp _decode tag 0-end");
    }
}

void FFmpeg::prepare() {
    LOGE("ffmpeg-cpp prepare tag 0-0 is_finished=%d", is_finished);
    this->player_status = STOP;

    if (!is_finished) {
        av_usleep(130000);
    }

    if (is_finished) {
        is_finished = false;
        LOGE("ffmpeg-cpp prepare tag 1 start release");
        release();
        LOGE("ffmpeg-cpp prepare tag 2 end release");
        pthread_create(&pid_prepare, nullptr, thrun_prepare, this);
        LOGE("ffmpeg-cpp prepare tag 3-end");
    }
}

void FFmpeg::start() {
    LOGE("ffmpeg-cpp start tag 0 start audio1 work player_status=%d", player_status);
    if (player_status == PAUSE) {
        prepare();
        return;
    }

    if (audio_channel) {
        LOGE("ffmpeg-cpp start tag 0-0 start audio1 work");
        audio_channel->setVideoChannel(video_channel);
        audio_channel->startWork();
        audio_channel->play();
        LOGE("ffmpeg-cpp start tag 0-1 start audio1 work");
    }
    LOGE("ffmpeg-cpp start tag 1 start audio2 work");
    if (audio_channel2) {
        LOGE("ffmpeg-cpp start tag 1-0 start audio2 work");
        audio_channel2->setVideoChannel(video_channel);
        audio_channel2->startWork();
        audio_channel2->play();
        LOGE("ffmpeg-cpp start tag 1-2 start audio2 work");
    }

    LOGE("ffmpeg-cpp start tag 2 start video work");
    if (video_channel) {
        LOGE("ffmpeg-cpp start tag 2-0 start video work");
        video_channel->set_audio_channel(audio_channel);
        video_channel->startWork();
        video_channel->play();
        LOGE("ffmpeg-cpp start tag 2-1 start video work");
    }

    LOGE("ffmpeg-cpp start tag 3 start decode packet");
    pthread_create(&pid_decode, nullptr, thrun_decode_packet, this);
}

void FFmpeg::pause() {
    LOGE("ffmpeg-cpp pause tag 0");
    if (player_status == PLAYING) {
        _cur_position = get_position();
        stop();

        seek_position = (int64_t) _cur_position;
        player_status = PAUSE;
        LOGE("ffmpeg-cpp _prepare tag 1 pause position=%f seek_position=%lld", _cur_position, seek_position);
    }
    LOGE("ffmpeg-cpp pause tag 0-end");
}

void FFmpeg::stop() {
    LOGE("ffmpeg-cpp stop tag 0");
    release();
    LOGE("ffmpeg-cpp stop tag 0-end");
}

void FFmpeg::release() {
    pthread_mutex_lock(&mutex);
    LOGE("ffmpeg-cpp release tag 0-1-----------start-----------");
    player_status = STOP;
    is_finished = false;

    if (video_channel) {
        video_channel->is_playing = false;
        video_channel->packets.clear(0);
        video_channel->packets.push(nullptr);

        video_channel->frames.clear(0);
        video_channel->frames.push(nullptr);
    }

    if (audio_channel) {
        audio_channel->is_playing = false;
        audio_channel->packets.clear(0);
        audio_channel->packets.push(nullptr);

        audio_channel->frames.clear(0);
        audio_channel->frames.push(nullptr);
    }

    if (audio_channel2) {
        audio_channel2->is_playing = false;
        audio_channel2->packets.clear(0);
        audio_channel2->packets.push(nullptr);

        audio_channel2->frames.clear(0);
        audio_channel2->frames.push(nullptr);
    }

    int64_t start_time = av_gettime();
    int64_t time_diff_ms = (av_gettime() - start_time) / 1000;

    LOGE("ffmpeg-cpp release tag 0-1 rtcp time=%lld", time_diff_ms);
    if (pid_prepare != 0) {
        int ret = pthread_join(pid_prepare, nullptr);
        pid_prepare = 0;
        LOGE("ffmpeg-cpp release tag 0-1-1");
    }

    LOGE("ffmpeg-cpp release tag 0-2 pid_decode=%d", pid_decode);
    if (pid_decode != 0) {
        int ret = pthread_join(pid_decode, nullptr);
        pid_decode = 0;
        LOGE("ffmpeg-cpp release tag 0-2-1");
    }

    LOGE("ffmpeg-cpp release tag 0-3");
    if (video_channel) {
        LOGE("ffmpeg-cpp release tag 0-3-1");
        video_channel->stop();
    }

    LOGE("ffmpeg-cpp release tag 0-4");
    if (audio_channel) {
        LOGE("ffmpeg-cpp release tag 0-4-1");
        audio_channel->stop();
    }

    LOGE("ffmpeg-cpp release tag 0-5");
    if (video_channel) {
        av_mediacodec_default_free(video_channel->av_codec_ctx);
        LOGE("ffmpeg-cpp release tag 0-5-1");
    }

    LOGE("ffmpeg-cpp release tag 0-6");
    if (av_format_ctx) {
        LOGE("ffmpeg-cpp release tag 0-6-1");
        avformat_close_input(&av_format_ctx);
        avformat_free_context(av_format_ctx);
        av_format_ctx = nullptr;
        LOGE("ffmpeg-cpp release tag 0-6-2");
    }

    if (avio_ctx) {
        avio_context_free(&avio_ctx); // 释放 AVIOContext
    }

    LOGE("ffmpeg-cpp release tag 0-7");
    time_diff_ms = (av_gettime() - start_time) / 1000;

    LOGE("ffmpeg-cpp release tag 0-7-0 pn=%s", package_name);
    if (package_name && strcmp(PACKAGE_NAME, package_name) != 0) {
        SAFE_DELETE_OBJECT(video_channel);
        LOGE("ffmpeg-cpp release tag 0-7-1 release video_channel");
    }

    SAFE_DELETE_OBJECT(audio_channel);
    LOGE("ffmpeg-cpp release tag 0-7-2 release audio_channel");
    SAFE_DELETE_OBJECT(audio_channel2);
    LOGE("ffmpeg-cpp release tag 0-7-3 release audio_channel2");
    LOGE("ffmpeg-cpp release tag 0-8 total time=%lld", time_diff_ms);

    is_finished = true;
    pthread_mutex_unlock(&mutex);
    LOGE("ffmpeg-cpp release tag 0-9 end");
}

double FFmpeg::duration() {
    LOGE("ffmpeg-cpp duration tag 0 _duration=%f", this->_duration);
    return abs(this->_duration);
}

double FFmpeg::get_position() {
    int position = 0;
    LOGE("ffmpeg-cpp get_position tag 0-0 play type=%d", play_type);
    switch (play_type) {
        case 1:
            break;
        case 2:
            position = seek_position;
            break;
        case 3:
            if (video_channel) {
                double second = video_channel->pts_second;
                double diff = video_channel->pts_second_diff + seek_position;
                position = second - abs(diff);
                if (player_status != PLAYING) {
                    LOGE("ffmpeg-cpp get_position tag 3-0 second=%f diff=%f position=%d seek_position=%lld", second, diff, position, seek_position);
                    position = seek_position;
                }
                LOGE("ffmpeg-cpp get_position tag 3-1 second=%f diff=%f position=%d", second, diff, position);
            }
            break;
        case 4:
            if (video_channel) {
                double second = video_channel->pts_second;
                LOGE("ffmpeg-cpp get_position tag 4 second=%f", second);
                position = second;
                LOGE("ffmpeg-cpp get_position tag 4-1 second=%f position=%d", second, position);
            }
            break;
    }

    LOGE("ffmpeg-cpp get_position tag 3 pos=%d,seek_position=%lld", position, seek_position);
    return position;
}

bool FFmpeg::is_playing() {
    LOGE("ffmpeg-cpp is_playing tag 0 playing=%d", player_status);
    return player_status;
}

void FFmpeg::reset_seek(int64_t position) {
    seek_position = position;
}

void FFmpeg::set_play_type(int type) {
    if (type >= 1 && type <= 4) {
        play_type = type;
    }
}

void FFmpeg::_seek_to(const char *timestamp, int type) {
    LOGE("ffmpeg-cpp _seek_to tag 0 timestamp=%s type=%d", timestamp, type);
    player_status = SEEKING;
    play_type = type;

    LOGE("ffmpeg-cpp _seek_to tag 1");
    if (pid_decode != 0) {
        pthread_join(pid_decode, nullptr);
        pid_decode = 0;
    }
    LOGE("ffmpeg-cpp _seek_to tag 2");

    switch (type) {
        case 1://直播
            SAFE_FREE_STRING(timestamp_abt);
            LOGE("ffmpeg-cpp _seek_to tag 1 timestamp_abt=%s type=%d", timestamp_abt, type);
            break;
        case 2://时移
            if (timestamp) {
                seek_position = atoi(timestamp);
                LOGE("ffmpeg-cpp _seek_to tag 2-0 timestamp=%d", seek_position);
                time_t now = time(NULL);
                struct tm *local_time = localtime(&now);
                struct tm specific_time = *local_time;

                specific_time.tm_sec += seek_position;

                time_t time = mktime(&specific_time);
                struct tm *s_time = gmtime(&time);

                char buffer[25];
                strftime(buffer, sizeof(buffer), "%Y%m%dT%H%M%SZ", s_time);
                LOGE("ffmpeg-cpp _seek_to tag 2-0 buffer=%s type=%d", buffer, type);
                timestamp_abt = strdup(buffer);
            }

            LOGE("ffmpeg-cpp _seek_to tag 2-end timestamp_abt=%s type=%d", timestamp_abt, type);
            break;
        case 3://回看
            //timestamp_abt = strdup(timestamp);
            seek_position = atoi(timestamp);
            LOGE("ffmpeg-cpp _seek_to tag 3 seek_position=%lld type=%d", seek_position, type);
            break;
        case 4://点播
            seek_position = atoi(timestamp);
            LOGE("ffmpeg-cpp _seek_to tag 4 seek_position=%lld type=%d", seek_position, type);
            break;
    }
}

double FFmpeg::get_cur_diff() {
    if (video_channel && is_playing()) {
        return video_channel->cur_diff;
    }
    return 0;
}

FFmpeg::~FFmpeg() {
    LOGE("ffmpeg-cpp destroy tag 0");
    pthread_mutex_destroy(&seek_mutex_t);
    pthread_mutex_destroy(&mutex);
    pthread_cond_destroy(&cond);

    LOGE("ffmpeg-cpp destroy tag 1");
    //release();
    LOGE("ffmpeg-cpp destroy tag 2");
    SAFE_FREE_STRING(stb_id);
    LOGE("ffmpeg-cpp destroy tag 2-1");
    SAFE_FREE_STRING(stb_ip);
    LOGE("ffmpeg-cpp destroy tag 2-2");
    SAFE_FREE_STRING(fcc_server_ip);
    LOGE("ffmpeg-cpp destroy tag 2-3");
    SAFE_FREE_STRING(multicast_url);
    LOGE("ffmpeg-cpp destroy tag 2-4");
    SAFE_FREE_STRING(unicast_url);
    LOGE("ffmpeg-cpp destroy tag 2-5");
    SAFE_FREE_STRING(data_source);
    LOGE("ffmpeg-cpp destroy tag 2-6");
    SAFE_FREE_STRING(cur_data_source);

    LOGE("ffmpeg-cpp destroy tag 3");

    SAFE_DELETE_OBJECT(video_channel);
    SAFE_DELETE_OBJECT(audio_channel);
    SAFE_DELETE_OBJECT(audio_channel2);
    LOGE("ffmpeg-cpp destroy tag 4");

    if (callback) {
        SAFE_DELETE_OBJECT(this->callback);
    }

    LOGE("ffmpeg-cpp destroy tag 5");
    if (jni_env) {
        jni_env->DeleteGlobalRef(surface);
        surface = nullptr;
    }
    LOGE("ffmpeg-cpp destroy tag 6-end");
}

char *FFmpeg::get_cur_data_source() {
    return this->cur_data_source;
}

bool FFmpeg::check_stream(char *source) {
    LOGE("ffmpeg-cpp check_stream tag 0-0 source=%s", source);
    AVFormatContext *fmt_ctx = avformat_alloc_context();
    if (!fmt_ctx) {
        LOGE("ffmpeg-cpp check_stream tag 0-0-1 Failed to allocate AVFormatContext");
        SAFE_FREE_STRING(source);
        return false;
    }

    interrupt_ctx = {av_gettime() / 1000, 2000};
    fmt_ctx->interrupt_callback.callback = interrupt_timeout;
    fmt_ctx->interrupt_callback.opaque = this;

    int ret = avformat_open_input(&fmt_ctx, source, nullptr, nullptr);
    bool result = (ret >= 0);

    LOGE("ffmpeg-cpp check_stream tag 0-1 enable=%d", result);
    avformat_close_input(&fmt_ctx);
    SAFE_FREE_STRING(source);
    return result;
}

bool FFmpeg::check_fcc_enable() {
    LOGE("ffmpeg-cpp check_fcc_enable tag 0-0 fcc=%d", 0);
    return 0;
}

void FFmpeg::completed() {
    double dur = duration();
    double cur = get_position();
    LOGE("ffmpeg-cpp completed tag 0-1 dur=%f cur=%f", dur, cur);
}



