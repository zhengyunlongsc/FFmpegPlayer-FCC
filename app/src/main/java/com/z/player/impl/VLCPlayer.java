package com.z.player.impl;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;

import androidx.annotation.NonNull;

import com.z.player.interfaces.IPlayer;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.interfaces.IVLCVout;

import java.util.ArrayList;

/**
 * 版权 (C), Z
 * 描述:
 * 作者: zyl on 2020/7/26 12:52
 * 修改人:
 * 修改描述:
 * 修改日期:
 */
public class VLCPlayer implements IPlayer {
    private final static String TAG = VLCPlayer.class.getName();
    private Media mMedia;
    private LibVLC mLicVLC;
    private SurfaceHolderCallback mSurfaceHolderCallback;
    private SurfaceTextureListener mSurfaceTextureListener;
    private TextureView mTextureView;
    private SurfaceView mSurfaceView;
    private MediaPlayer mMediaPlayer;
    private boolean isSurfaceCreated;

    private VLCPlayer() {
        init();
    }

    public static VLCPlayer getInstance(boolean single) {
        return single ? VLCPlayer.Lazy.INSTANCE : new VLCPlayer();
    }

    public static VLCPlayer getInstance() {
        return VLCPlayer.Lazy.INSTANCE;
    }

    private static class Lazy {
        public static final VLCPlayer INSTANCE = new VLCPlayer();
    }

    private void init() {

    }

    public void initMediaPlayer(Context context) {
        if (context != null) {
            ArrayList<String> options = new ArrayList<>();
            options.add("-vvv");
            mLicVLC = new LibVLC(context.getApplicationContext(), options);
            mMediaPlayer = new MediaPlayer(mLicVLC);
            mMediaPlayer.setEventListener(new EventListener());
        }
    }

    @Override
    public void setDataSource(String str) {
        mMedia = new Media(mLicVLC, Uri.parse(str));
        mMedia.setHWDecoderEnabled(true, true);
        mMediaPlayer.setMedia(mMedia);
        mMedia.release();
    }

    @Override
    public void setSurface(SurfaceView surface) {
        mSurfaceView = surface;
        if (mSurfaceView != null) {
            if (mSurfaceHolderCallback == null) {
                mSurfaceHolderCallback = new SurfaceHolderCallback();
            }
            mSurfaceView.getHolder().addCallback(mSurfaceHolderCallback);
        }
    }

    @Override
    public void setSurface(TextureView textureView) {
        mTextureView = textureView;
        if (textureView != null) {
            if (mSurfaceTextureListener == null) {
                mSurfaceTextureListener = new SurfaceTextureListener();
            }
            textureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    public void prepare() {

    }

    @Override
    public void seekTo(String i, int type) {
        Log.d(TAG, "seekTo: ---> tag 0-0 seek=" + i + " type=" + type);
        if (mMediaPlayer != null && !TextUtils.isEmpty(i)) {
            try {
                mMediaPlayer.setTime(Long.parseLong(i));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void pause() {

    }

    @Override
    public long getDuration() {
        long duration = 0;
        if (mMediaPlayer != null) {
            duration = mMediaPlayer.getLength();
        }
        Log.d(TAG, "getDuration: ---> tag 0-0 duration=" + duration);
        return duration;
    }

    @Override
    public long getPosition() {
        long position = 0;
        if (mMediaPlayer != null) {
            position = mMediaPlayer.getTime();
        }
        Log.d(TAG, "getPosition: ---> tag 0-0 position=" + position);
        return position;
    }

    @Override
    public boolean isPlaying() {
        return false;
    }

    @Override
    public void release() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
        }

        if (mMedia != null) {
            mMedia.release();
        }

        if (mLicVLC != null) {
            mLicVLC.release();
        }
    }

    public void play() {
        if (mMediaPlayer != null) {
            if (isSurfaceCreated) {
                IVLCVout ivlcVout = mMediaPlayer.getVLCVout();
                ivlcVout.setWindowSize(getWidth(), getHeight());

                if (mSurfaceView != null) {
                    ivlcVout.setVideoSurface(getSurface(), mSurfaceView.getHolder());
                } else if (mTextureView != null) {
                    ivlcVout.setVideoSurface(getSurfaceTexture());
                }

                ivlcVout.attachViews();
                mMediaPlayer.play();
            }
        }
    }

    private int getWidth() {
        int w = 0;
        if (mTextureView != null) {
            w = mTextureView.getWidth();
        } else if (mSurfaceView != null) {
            w = mSurfaceView.getWidth();
        }
        return w;
    }

    private int getHeight() {
        int h = 0;
        if (mTextureView != null) {
            h = mTextureView.getHeight();
        } else if (mSurfaceView != null) {
            h = mSurfaceView.getHeight();
        }
        return h;
    }

    private SurfaceTexture getSurfaceTexture() {
        SurfaceTexture texture = null;
        if (mTextureView != null) {
            texture = mTextureView.getSurfaceTexture();
        }

        return texture;
    }

    private Surface getSurface() {
        Surface surface = null;
        if (mSurfaceView != null) {
            surface = mSurfaceView.getHolder().getSurface();
        }
        return surface;
    }

    public MediaPlayer getMediaPlayer() {
        return mMediaPlayer;
    }

    public void onPause() {
        Log.d(TAG, "onPause: ---> tag 0-0");
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
        }
    }

    public void onResume() {
        Log.d(TAG, "onResume: ---> tag 0-0");
        if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
            mMediaPlayer.play();
        }
    }

    @Override
    public int getPlayType() {
        return 0;
    }

    @Override
    public void setLogEnable(boolean enable) {

    }

    @Override
    public void setMediaCodec(boolean b) {

    }

    @Override
    public void setOnPlayerListener(IPlayer.OnPlayerListener listener) {

    }

    private interface OnPlayerListener {
        void onBuffering();
    }

    private OnPlayerListener mOnPlayerListener;

    public void setOnPlayerListener(OnPlayerListener listener) {
        this.mOnPlayerListener = listener;
    }

    class EventListener implements MediaPlayer.EventListener {

        @Override
        public void onEvent(MediaPlayer.Event event) {
            //Log.d(TAG, "onEvent: ------>> type=" + event.type);
            switch (event.type) {
                case MediaPlayer.Event.Opening:
                    Log.d(TAG, "onEvent: ------>> Opening");
                    break;
                case MediaPlayer.Event.Playing:
                    Log.d(TAG, "onEvent: ------>> Playing");
                    break;
                case MediaPlayer.Event.Buffering:
                    float buffering = event.getBuffering();
                    if (buffering == 100) {
                        Log.d(TAG, "onEvent: ------>> buffering=" + buffering + " cur_time=" + mMediaPlayer.getTime());
                        if (mOnPlayerListener != null) {
                            mOnPlayerListener.onBuffering();
                        }
                    }
                    break;
                case MediaPlayer.Event.PositionChanged:
                    //Log.d(TAG, "onEvent: ---> position=" + event.getPositionChanged());
                    break;
                case MediaPlayer.Event.TimeChanged:
                    long time = event.getTimeChanged();
                    //Log.d(TAG, "onEvent: ------>> time_changed=" + time);
                    break;
                case MediaPlayer.Event.EndReached://播放完成
                    Log.d(TAG, "onEvent: ------>> EndReached");
                    break;
                case MediaPlayer.Event.Stopped://停止播放
                    Log.d(TAG, "onEvent: ------>> Stopped");
                    break;
                case MediaPlayer.Event.Paused:
                    Log.d(TAG, "onEvent: ------>> Paused");
                    break;
            }
        }
    }

    class SurfaceHolderCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "surfaceCreated: ---> tag 0-0");
            isSurfaceCreated = true;
            play();
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d(TAG, "surfaceChanged: ---> tag 0-0 width=" + width + " height=" + height);
            if (mMediaPlayer != null) {
                mMediaPlayer.getVLCVout().setWindowSize(width, height);
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "surfaceDestroyed: ---> tag 0-0");
            isSurfaceCreated = false;
        }
    }

    class SurfaceTextureListener implements TextureView.SurfaceTextureListener {

        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int w, int h) {
            Log.d(TAG, "onSurfaceTextureAvailable: ---> tag 0-0 w=" + w + " h=" + h);
            isSurfaceCreated = true;
            play();
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int w, int h) {
            Log.d(TAG, "onSurfaceTextureSizeChanged: ---> tag 0-0 w=" + w + " h=" + h);
            if (mMediaPlayer != null) {
                mMediaPlayer.getVLCVout().setWindowSize(w, h);
            }
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
            //Log.d(TAG, "onSurfaceTextureUpdated: ---> tag 0-0");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
            Log.d(TAG, "onSurfaceTextureDestroyed: ---> tag 0-0");
            isSurfaceCreated = false;
            return true;
        }
    }


}
