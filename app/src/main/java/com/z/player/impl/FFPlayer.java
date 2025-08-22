package com.z.player.impl;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;

import com.z.player.help.MulHelper;
import com.z.player.interfaces.IPlayer;

import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;


/**
 * 作者: zyl on 2022/6/10 16:59
 * 修改人:
 * 修改描述:
 * 修改日期:
 */
public class FFPlayer implements IPlayer {
    private final static String TAG = FFPlayer.class.getName();
    private final static int HANDLER_MSG_ON_PREPARE = 0x0001;
    private final static int HANDLER_MSG_ON_CALLBACK = 0x0002;
    private final static int HANDLER_MSG_ON_PROGRESS = 0x0003;
    private final static int HANDLER_MSG_ON_START = 0x0004;
    private final static int HANDLER_MSG_ON_COMPLETED = 0x0005;
    private final static String FCC_SERVICE_PORT = "15970";
    private final static String FCC_SERVICE_IP = "123.147.117.148";//123.147.111.106
    private AudioTrack audioTrack;
    private Surface surface;
    private Handler mHandler;
    private String data_source;

    static {
        Log.d(TAG, "static initializer load native-lib 1");
        try {
            System.loadLibrary("native-lib");
        } catch (Exception e) {
            Log.d(TAG, "static initializer load native-lib 1-1");
            e.printStackTrace();
        }
        Log.d(TAG, "static initializer load native-lib 2");
    }

    private FFPlayer() {
        init();
    }

    public static FFPlayer getInstance() {
        return Lazy.get();
    }

    private static class Lazy {
        private static FFPlayer INSTANCE;

        private static void reset() {
            INSTANCE = null;
        }

        private static FFPlayer get() {
            if (INSTANCE == null) {
                INSTANCE = new FFPlayer();
            }
            return INSTANCE;
        }
    }

    private void init() {
        boolean isMul = MulHelper.getInstance().isMulEnable();
        int streamMode = MulHelper.getInstance().getStreamMode();
        Log.d(TAG, "init: ---> tag 0-2 isMulEnable=" + isMul + " streamMode=" + streamMode);

        initHandler();
        native_ffmpeg_init();
        native_set_mul_enable(isMul);
        native_set_stream_mode(streamMode);
        native_set_ip_address(getIpAddress());
        native_set_serial_no(getSerialNo());
        native_set_fcc_address(FCC_SERVICE_IP, FCC_SERVICE_PORT);

        Log.d(TAG, "init: ---> tag 0-3 init end...");
    }

    private native void native_log(boolean enable);

    private native void native_ffmpeg_init();

    private native void native_prepare();

    private native int native_position();

    private native int native_duration();

    private native void native_seek_to(String seek, int type);

    private native void native_reset_seek(long i);

    private native void native_start();

    private native void native_pause();

    private native boolean native_is_playing();

    private native void native_stop();

    private native void native_release();

    private native void native_set_mul_enable(boolean enable);

    private native void native_set_stream_mode(int streamMode);

    private native void native_set_data_source(String source);

    private native void native_set_surface(Surface surface);

    private native void native_set_play_type(int type);

    private native void native_set_mediacodec(boolean enable);

    private native void native_set_package_name(String pn);

    private native void native_set_ip_address(String ip);

    private native void native_set_serial_no(String id);

    private native void native_set_fcc_address(String ip, String port);

    private native int native_get_play_type();

    private native int native_get_lag_count();

    private native double native_get_cur_diff();

    private native boolean native_check_stream(String source);

    private native boolean get_fcc_enable();

    private native String native_get_cur_source();

    private native String native_version();

    private void initHandler() {
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case HANDLER_MSG_ON_PREPARE:
                        if (onPlayerListener != null) {
                            Log.d(TAG, "onPrepare: tag 1");
                            onPlayerListener.onPrepared();
                        }
                        break;
                    case HANDLER_MSG_ON_CALLBACK:
                        if (onPlayerListener != null) {
                            Log.d(TAG, "onCallback: tag 1");
                            onPlayerListener.onCallback(msg.arg1, (String) msg.obj);
                        }
                        break;
                    case HANDLER_MSG_ON_PROGRESS:
                        if (onPlayerListener != null) {
                            Log.v(TAG, "onProgress: tag 1");
                            onPlayerListener.onProgress((Double) msg.obj);
                        }
                        break;
                    case HANDLER_MSG_ON_START:
                        if (onPlayerListener != null) {
                            Log.v(TAG, "onStart: tag 1");
                            onPlayerListener.onStart();
                        }
                        break;
                    case HANDLER_MSG_ON_COMPLETED:
                        if (onPlayerListener != null) {
                            Log.v(TAG, "onCompleted: tag 1");
                            onPlayerListener.onCompletion();
                        }
                        break;
                }
            }
        };
    }

    public void prepare(String source) {
        Log.d(TAG, "prepare: tag 0, source=" + source);
        this.data_source = source;
        prepare();
    }

    @Override
    public void setDataSource(String source) {
        Log.d(TAG, "setDataSource: ---> tag 0, pre set source=" + data_source);
        Log.d(TAG, "setDataSource: ---> tag 0, cur set source=" + source);
        if (!TextUtils.equals(source, data_source)) {
            this.native_reset_seek(0);
        }
        this.data_source = source;
        this.native_set_data_source(source);
    }

    @Override
    public void setSurface(SurfaceView surfaceView) {
        Surface surface1 = null;
        if (surfaceView != null) {
            surface1 = surfaceView.getHolder().getSurface();
        }
        Log.d(TAG, "setSurface: tag 0, surface=" + surface1);
        Log.d(TAG, "setSurface: tag 0, this.surface=" + this.surface);
        this.surface = surface1;
        this.native_set_surface(surface1);
    }

    @Override
    public void setSurface(TextureView textureView) {

    }

    @Override
    public void prepare() {
        Log.d(TAG, "prepare: tag 0");
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                FFPlayer.this.native_prepare();
            }
        });
    }

    @Override
    public long getPosition() {
        return this.native_position();
    }

    @Override
    public long getDuration() {
        Log.d(TAG, "getDuration: tag 0");
        int duration = this.native_duration();
        Log.d(TAG, "getDuration: tag 1, duration=" + duration);
        return duration;
    }

    @Override
    public int getPlayType() {
        int type = native_get_play_type();
        Log.d(TAG, "getPlayType: ---> tag 0-0 type=" + type);
        return type;
    }

    @Override
    public void start() {
        Log.d(TAG, "start: tag 0");
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                FFPlayer.this.native_start();
            }
        });
    }

    @Override
    public void seekTo(String position, int type) {
        Log.d(TAG, "seekTo: ---> tag 0 position=" + position);
        this.native_seek_to(position, type);
    }

    @Override
    public boolean isPlaying() {
        Log.d(TAG, "isPlaying: tag 0");
        return this.native_is_playing();
    }

    @Override
    public void pause() {
        Log.d(TAG, "pause: tag 0");
        this.native_pause();
        Log.d(TAG, "pause: tag 1");
    }

    @Override
    public void stop() {
        Log.d(TAG, "stop: tag 0");
        this.native_stop();
    }

    @Override
    public void release() {
        Log.d(TAG, "release: tag 0");
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }

        this.surface = null;
        this.native_release();

        Lazy.reset();
    }

    public String getDataSource() {
        Log.d(TAG, "getDataSource: ---> tag 0-0 source=" + data_source);
        return this.data_source;
    }

    public void setPlayType(int type) {
        Log.d(TAG, "setPlayType: ---> tag 0-0 type=" + type);
        native_set_play_type(type);
    }

    public void setLogEnable(boolean enable) {
        Log.d(TAG, "setNativeLogEnable: tag 0, enable=" + enable);
        this.native_log(enable);
    }

    public String getVersion() {
        Log.d(TAG, "getVersion: tag 0");
        String v = native_version();
        Log.d(TAG, "getVersion: tag 1, version=" + v);
        return v;
    }

    public boolean checkStream(String path) {
        Log.d(TAG, "checkStream: ---> tag 0-0 path=" + path);
        boolean b = false;
        if (!TextUtils.isEmpty(path)) {
            b = native_check_stream(path);
        }
        Log.d(TAG, "checkStream: ---> tag 0-1 enable=" + b);
        return b;
    }

    public boolean getFccEnable() {
        Log.d(TAG, "getFccEnable: ---> tag 0-0 ");
        boolean b = get_fcc_enable();
        Log.d(TAG, "getFccEnable: ---> tag 0-1 fcc=" + b);
        return b;
    }

    public boolean isMulEnable() {
        boolean enable = MulHelper.getInstance().isMulEnable();
        Log.d(TAG, "isMulEnable: ---> isMulEnable=" + enable);
        return enable;
    }

    public void setPackageName(String pn) {
        Log.d(TAG, "setPackageName: ---> pn=" + pn);
        native_set_package_name(pn);
    }

    public void setMulEnable(boolean b) {
        Log.d(TAG, "setMulEnable: ---> enable=" + b + " -- " + isMulEnable());
        native_set_mul_enable(b);
    }

    public void setMediaCodec(boolean enable) {
        Log.d(TAG, "setMediaCodecEnable: ---> tag 0");
        this.native_set_mediacodec(enable);
    }

    public void setFccAddress(String ip, String port) {
        Log.d(TAG, "setFccAddress: ---> ip=" + ip + " port=" + port);
        this.native_set_fcc_address(ip, port);
    }

    public int getLagCount() {
        int count = native_get_lag_count();
        Log.d(TAG, "getLagCount: ---> count=" + count);
        return count;
    }

    public double getCurDiff() {
        double d = native_get_cur_diff();
        Log.d(TAG, "getCurDiff: ---> v_diff=" + d);
        return d;
    }

    public String getCurSource() {
        String s = native_get_cur_source();
        Log.d(TAG, "getCurSource: ---> source=" + s);
        return s;
    }

    private void appExit() {
        Log.d(TAG, "appExit: ---> tag 0-0");
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }

    private String getIpAddress() {
        String ip = null;
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            if (networkInterfaces == null) { // 非空判断
                return null;
            }
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                if (networkInterface == null) { // 非空判断
                    continue;
                }
                // 获取网络接口的所有 IP 地址
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                if (inetAddresses == null) { // 非空判断
                    continue;
                }
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    if (inetAddress == null) { // 非空判断
                        continue;
                    }
                    // 过滤回环地址和非 IPv4 地址
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        ip = inetAddress.getHostAddress(); // 找到后直接返回
                        if (ip != null && !ip.isEmpty()) { // 进一步确保 IP 地址有效
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace(); // 打印异常信息
        }
        return ip; // 如果未找到 IP 地址，返回 null
    }

    private String getSerialNo() {
        String value = "";
        try {
            String className = "android.os.SystemProperties";
            @SuppressLint("PrivateApi") Class<?> c = Class.forName(className);
            Method get = c.getMethod("get", String.class);
            value = (String) get.invoke(c, "ro.serialno");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return value;
    }

    private OnPlayerListener onPlayerListener;

    public void setOnPlayerListener(OnPlayerListener listener) {
        this.onPlayerListener = listener;
    }

    //==================================以下方法由C调用Java层方法===================================

    /**
     * 创建 AudioTrack
     *
     * @param sampleRate 采样率
     * @param channels   通道数
     */
    public void createAudioTrack(int sampleRate, int channels) {
        int channelConfig;
        if (channels == 1) {
            channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        } else if (channels == 2) {
            channelConfig = AudioFormat.CHANNEL_OUT_STEREO;
        } else {
            channelConfig = AudioFormat.CHANNEL_OUT_MONO;
        }
        int bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, AudioFormat.ENCODING_PCM_16BIT);
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate, channelConfig,
                AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
        audioTrack.play();
    }

    /**
     * 播放 AudioTrack
     *
     * @param data
     * @param length
     */
    public void playAudioTrack(byte[] data, int length) {
        if (audioTrack != null && audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
            audioTrack.write(data, 0, length);
        }
    }

    /**
     * 释放 AudioTrack
     */
    public void releaseAudioTrack() {
        if (audioTrack != null) {
            if (audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                audioTrack.stop();
            }
            audioTrack.release();
            audioTrack = null;
        }
    }

    public void onPrepare() {
        Log.d(TAG, "onPrepare: tag 0");
        if (mHandler != null) {
            mHandler.sendEmptyMessage(HANDLER_MSG_ON_PREPARE);
        }
    }

    public void onCallback(int code, String message) {
        Log.d(TAG, "onCallback: tag 0");
        if (mHandler != null) {
            Message msg = mHandler.obtainMessage();
            msg.what = HANDLER_MSG_ON_CALLBACK;
            msg.arg1 = code;
            msg.obj = message;
            mHandler.sendMessage(msg);
        }
    }

    public void onProgress(double progress) {
        Log.v(TAG, "onProgress: tag 0");
        if (mHandler != null) {
            Message msg = mHandler.obtainMessage();
            msg.what = HANDLER_MSG_ON_PROGRESS;
            msg.obj = progress;
            mHandler.sendMessage(msg);
        }
    }

    public void onStart() {
        Log.d(TAG, "onStart: tag 0");
        if (mHandler != null) {
            mHandler.sendEmptyMessage(HANDLER_MSG_ON_START);
        }
    }

    public void onCompleted() {
        Log.d(TAG, "onCompleted: tag 0");
        if (mHandler != null) {
            mHandler.sendEmptyMessage(HANDLER_MSG_ON_COMPLETED);
        }
    }

    //==================================以上方法由C调用Java层方法===================================
}
