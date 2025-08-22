//标记线程 因为子线程需要attach
#define THREAD_MAIN 1
#define THREAD_CHILD 2

//错误代码
//打不开视频
#define FFMPEG_CAN_NOT_OPEN_URL 1
//找不到流媒体
#define FFMPEG_CAN_NOT_FIND_STREAMS 2
//找不到解码器
#define FFMPEG_FIND_DECODER_FAIL 3
//无法根据解码器创建上下文
#define FFMPEG_ALLOC_CODEC_CONTEXT_FAIL 4
//根据流信息 配置上下文参数失败
#define FFMPEG_CODEC_CONTEXT_PARAMETERS_FAIL 6
//打开解码器失败
#define FFMPEG_OPEN_DECODER_FAIL 7
//没有音视频
#define FFMPEG_NOMEDIA 8
#define FFMPEG_PACKET_TIMEOUT 9

#define FFMPEG_FCC_RECEIVE_FAIL 10
#define FFMPEG_FCC_RESPONSE_FAIl 11
#define FFMPEG_FCC_RESPONSE_SUCCESS 12
#define FFMPEG_FCC_RECEIVE_FAIL_MSG "FCC服务器接收数据错误!"

#define FFMPEG_PACKET_STREAM_EXCEPTION "数据包接收异常,请检查网络!"
#define FFMPEG_SEEK_FAILED_CODE 13
#define FFMPEG_SEEK_FAILED_MSG "打开进度播放失败!"

#define SAFE_FREE_STRING(ptr) if (ptr) { free(ptr); ptr = nullptr; }
#define SAFE_DELETE_OBJECT(ptr) if (ptr) { delete ptr; ptr = nullptr; }
//addr2live.exe libexample.so 0x12345678

