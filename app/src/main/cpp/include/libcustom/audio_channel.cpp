#include "audio_channel.h"
#include "video_channel.h"

void bufferQueueCallback(SLAndroidSimpleBufferQueueItf bq, void *context) {
    auto *audio_channel = static_cast<AudioChannel *>(context);
    size_t size = audio_channel->get_pcm();
    if (size) {
        (*bq)->Enqueue(bq, audio_channel->out_buffer, size);
    }
}

AudioChannel::AudioChannel(int stream_index, AVCodecContext *av_codec_context, AVRational av_rational, double duration)
        : BaseChannel(stream_index, av_codec_context, av_rational) {
    this->av_codec_ctx = av_codec_context;
    this->duration = duration;
    this->swr_context = swr_alloc();

    out_channels = av_get_channel_layout_nb_channels(AV_CH_LAYOUT_STEREO);
    out_sample_size = av_get_bytes_per_sample(AV_SAMPLE_FMT_S16);
    out_sample_rate = 44100;//44100个16位 44100 * 2,44100*(双声道)*(16位)

    //LOGE("audio-channel-cpp tag 0-0 channels=%d,sample_size=%d,sample_rate=%d", out_channels, out_sample_size, out_sample_rate);

    size_t size = out_sample_rate * out_channels * out_sample_size;
    //LOGE("audio-channel-cpp tag 0-1 size=%d", size);
    out_buffer = static_cast<uint8_t *>(av_malloc(size));
    memset(out_buffer, 0, size);

    uint64_t in_ch_layout = av_codec_context->channel_layout;
    AVSampleFormat in_sample_fmt = av_codec_context->sample_fmt;
    int in_sample_rate = av_codec_context->sample_rate;

    //LOGE("audio-channel-cpp tag 0-2 channels=%llu,ch_layout=%d,sample_rate=%d", in_ch_layout, in_sample_fmt, in_sample_rate);

    swr_alloc_set_opts(swr_context, AV_CH_LAYOUT_STEREO, AV_SAMPLE_FMT_S16, out_sample_rate,
                       in_ch_layout, in_sample_fmt, in_sample_rate, 0, nullptr);
    int ret = swr_init(swr_context);    //初始化
    //LOGE("audio-channel-cpp tag 0-3 swr_init ret=%d", ret);
    if (ret < 0) {
        //LOGE("audio-channel-cpp tag 0-3-1: %s", av_err2str(ret));
    }

    init_open_sl();
}

AudioChannel::~AudioChannel() {
    //LOGE("audio-channel-cpp destroy tag 0");
    shutdown_open_sl();
    if (swr_context) {
        swr_free(&swr_context);
        swr_context = nullptr;
    }

    if (out_buffer) {
        free(out_buffer);
        out_buffer = nullptr;
    }
    //LOGE("audio-channel-cpp destroy tag 0-end");
}

size_t AudioChannel::get_pcm() {
    //LOGE("audio-channel-cpp get_pcm tag 0-0-0 v_diff=%f", ren_def_diff);
    AVFrame *frame = nullptr;
    while (is_playing) {
        int ret = frames.pop(frame);
        if (ret == 0) {
            releaseFrame(&frame);
            continue;
        }

        this->pts_second = static_cast<double>(frame->pts) * av_q2d(av_rational);
        if (ren_def_diff == 0) {
            ren_def_diff = getSysDiff(this->pts_second);
            //LOGE("audio-channel-cpp get_pcm tag 0-0 v_diff=%f", ren_def_diff);
        }

        a_clock_diff = ren_def_diff - getSysDiff(this->pts_second);
        double v_a_diff = video_channel->v_clock_diff - a_clock_diff;

        //LOGE("audio-channel-cpp render tag 0-1 v_diff=%f a_diff=%f v_a_diff=%f play_second=%d", video_channel->v_clock_diff, a_clock_diff, v_a_diff,
             //play_second);

        if (a_clock_diff < -0.1) {
            //LOGE("audio-channel-cpp get_pcm a_diff=%f packets.size=%d", a_clock_diff, packets.size());
            releaseFrame(&frame);
            continue;
        } else if (a_clock_diff > 0) {
            //LOGE("audio-channel-cpp get_pcm tag 0-2 sleep...");
            av_usleep(a_clock_diff * 1000000);
        }

        int64_t delays = swr_get_delay(swr_context, frame->sample_rate);
        int64_t max_samples = av_rescale_rnd(delays + frame->nb_samples, out_sample_rate, frame->sample_rate, AV_ROUND_UP);
        int samples = swr_convert(swr_context, &out_buffer, static_cast<int>(max_samples),
                                  (const uint8_t **) frame->data, frame->nb_samples);

        if (samples < 0) {
            releaseFrame(&frame);
            continue;
        }

        releaseFrame(&frame);
        size_t size = samples * out_sample_size * out_channels;
        return size;
    }

    releaseFrame(&frame);
    return 0;
}

void AudioChannel::setVideoChannel(VideoChannel *video) {
    this->video_channel = video;
}

void AudioChannel::render() {
    //LOGE("audio-channel-cpp render tag 0");
    bufferQueueCallback(buffer_queue, this);
}

void AudioChannel::check_completion() {

}

void AudioChannel::play() {
    BaseChannel::play();
}

void AudioChannel::stop() {
    (*player_play)->SetPlayState(player_play, SL_PLAYSTATE_STOPPED);
    BaseChannel::stop();
}

void AudioChannel::sync() {
    //LOGE("audio-channel-cpp sync tag 0-0");
}

void AudioChannel::init_open_sl() {
    slCreateEngine(&engine_object, 0, nullptr, 0, nullptr, nullptr);
    (*engine_object)->Realize(engine_object, SL_BOOLEAN_FALSE);
    (*engine_object)->GetInterface(engine_object, SL_IID_ENGINE, &engine_engine);

    const SLInterfaceID ids_engine[1] = {SL_IID_ENVIRONMENTALREVERB};
    const SLboolean req_engine[1] = {SL_BOOLEAN_FALSE};
    (*engine_engine)->CreateOutputMix(engine_engine, &output_mix_object, 1, ids_engine, req_engine);
    (*output_mix_object)->Realize(output_mix_object, SL_BOOLEAN_FALSE);

    // 创建缓冲队列音频播放器
    SLDataLocator_AndroidSimpleBufferQueue loc_bufq = {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2};
    SLDataFormat_PCM format_pcm = {
            SL_DATAFORMAT_PCM, 2, SL_SAMPLINGRATE_44_1,
            SL_PCMSAMPLEFORMAT_FIXED_16, SL_PCMSAMPLEFORMAT_FIXED_16,
            SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT,
            SL_BYTEORDER_LITTLEENDIAN
    };
    SLDataSource audio_src = {&loc_bufq, &format_pcm};

    SLDataLocator_OutputMix loc_outmix = {SL_DATALOCATOR_OUTPUTMIX, output_mix_object};
    SLDataSink audio_sink = {&loc_outmix, nullptr};

    const SLInterfaceID ids[3] = {SL_IID_BUFFERQUEUE, SL_IID_EFFECTSEND, SL_IID_VOLUME};
    const SLboolean req[3] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE};

    (*engine_engine)->CreateAudioPlayer(engine_engine, &player_object,
                                        &audio_src, &audio_sink, 3, ids, req);
    (*player_object)->Realize(player_object, SL_BOOLEAN_FALSE);
    (*player_object)->GetInterface(player_object, SL_IID_PLAY, &player_play);
    (*player_object)->GetInterface(player_object, SL_IID_ANDROIDSIMPLEBUFFERQUEUE, &buffer_queue);
    (*player_object)->GetInterface(player_object, SL_IID_PLAYBACKRATE, &playback_rate);
    (*buffer_queue)->RegisterCallback(buffer_queue, bufferQueueCallback, this);
    (*player_play)->SetPlayState(player_play, SL_PLAYSTATE_PLAYING);
}

void AudioChannel::shutdown_open_sl() {
    //LOGE("audio-channel-cpp shutdown_open_sl tag 1");

    if (player_object) {
        (*player_object)->Destroy(player_object);
    }

    //LOGE("audio-channel-cpp shutdown_open_sl tag 2");
    if (output_mix_object) {
        (*output_mix_object)->Destroy(output_mix_object);
    }

    //LOGE("audio-channel-cpp shutdown_open_sl tag 3");
    if (engine_object) {
        (*engine_object)->Destroy(engine_object);
    }

    //LOGE("audio-channel-cpp shutdown_open_sl tag 4-end");
}
