package com.z.player.interfaces;

import android.view.SurfaceView;
import android.view.TextureView;

public interface IPlayer {
    void setDataSource(String dataSource);

    void setSurface(SurfaceView surface);

    void setSurface(TextureView textureView);

    void prepare();

    void seekTo(String i, int type);

    void start();

    void stop();

    void release();

    void pause();

    long getDuration();

    long getPosition();

    int getPlayType();

    boolean isPlaying();

    void setLogEnable(boolean enable);

    void setMediaCodec(boolean b);

    void setOnPlayerListener(OnPlayerListener listener);

    interface OnPlayerListener {
        void onPrepared();

        void onCallback(int code, String message);

        void onProgress(double progress);

        void onStart();

        void onCompletion();
    }
}
