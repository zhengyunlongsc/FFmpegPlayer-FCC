package com.z.player.widget;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.widget.FrameLayout;

import com.alibaba.fastjson.JSON;
import com.z.player.activity2.BrowserActivity;
import com.z.player.common.Const;
import com.z.player.help.MulHelper;
import com.z.player.impl.FFPlayer;
import com.z.player.util.AppUtils;
import com.z.player.util.TimeUtils;

public class FFPlayerView extends SurfaceView {
    private static final String TAG = FFPlayerView.class.getName();
    private static final int HANDLER_MSG_SURFACE_CREATE_SUCCESS = 1;
    private Handler mHandler;
    private BrowserActivity mBrowserActivity;
    private SurfaceHolder.Callback mCallback;
    private FFPlayer mFPlayer;
    private String mDataSource;
    private boolean mSurfaceCreateSuccess;
    private boolean isMediaOverlay;
    private int mLeft;
    private int mTop;
    private int mRight;
    private int mBottom;
    private int mCurPlayType = 1;
    private String mTimestamp;

    public FFPlayerView(Context context) {
        super(context);
        init(context);
    }

    public FFPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FFPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        Log.d(TAG, "init: ---> tag 0-0 context=" + context);
        if (context instanceof BrowserActivity) {
            Log.d(TAG, "init: ---> tag 0-0 activity is not null");
            mBrowserActivity = (BrowserActivity) context;
        }

        setLayerType(View.LAYER_TYPE_HARDWARE, null);

        setRunOnUIThreadListener(what -> {
            switch (what) {
                case VISIBLE:
                    Log.d(TAG, "onUIThread: ---> tag 0-2 visible");
                    setVisibility(VISIBLE);
                    break;
                case INVISIBLE:
                    Log.d(TAG, "onUIThread: ---> tag 0-3 invisible");
                    setVisibility(INVISIBLE);
                    break;
            }
        });

        getHolder().setFormat(PixelFormat.TRANSPARENT);
    }

    private void initHandler() {
        cleanHandler();
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                int what = msg.what;
                switch (what) {
                    case HANDLER_MSG_SURFACE_CREATE_SUCCESS:
                        Log.d(TAG, "handleMessage: ---> tag 0-4 what=" + what);
                        play(mCurPlayType, mTimestamp, mDataSource);
                        break;
                }
            }
        };
    }

    private void runOnUIThread(int what) {
        Context context = getContext();
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            activity.runOnUiThread(() -> {
                if (mRunOnUIThreadListener != null) {
                    mRunOnUIThreadListener.onUIThread(what);
                }
            });
        }
    }

    public void setParentView(FrameLayout parent) {
    }

    private FFPlayer getFPlayer() {
        Log.d(TAG, "getFPlayer: ---> tag 0-0");
        if (mFPlayer != null) {
            return mFPlayer;
        }

        mFPlayer = FFPlayer.getInstance();
        mFPlayer.setLogEnable(Const.KEY_LOG_ENABLE);
        mFPlayer.setMediaCodec(true);
        Log.d(TAG, "getFPlayer: ---> tag 0-2");
        mFPlayer.setOnPlayerListener(new FFPlayer.OnPlayerListener() {
            @Override
            public void onCallback(int code, String message) {
                Log.d(TAG, "onError: ---> code=" + code + " msg=" + message);
            }

            @Override
            public void onPrepared() {
                Log.d(TAG, "onPrepare: ---> tag 0-0");

                mFPlayer.start();

                if (mCurPlayType > 2) {
                    Log.d(TAG, "onPrepare: ---> tag 0-1");
                    String method = "javascript:onPrepared()";
                    callJs(method);
                }
            }

            @Override
            public void onProgress(double progress) {

            }

            @Override
            public void onStart() {

            }

            @Override
            public void onCompletion() {
                Log.d(TAG, "onCompletion: ---> tag 0-0");
                String method = "javascript:onCompletion()";
                callJs(method);
            }
        });

        Log.d(TAG, "getFPlayer: ---> tag 0-3 init player finish...");
        return mFPlayer;
    }

    private void callJs(String method) {
        BrowserActivity activity = mBrowserActivity;
        if (activity != null && !TextUtils.isEmpty(method)) {
            Log.d(TAG, "callJs: ---> tag 0-0 method=" + method);
            activity.callJs(method);
        }
    }

    private void play(int type, String timestamp, String url) {
        Log.d(TAG, "play: ---> tag 0-0 to play url=" + url);
        Log.d(TAG, "play: ---> tag 0-0 surface create=" + mSurfaceCreateSuccess);
        if (!TextUtils.isEmpty(url) && mSurfaceCreateSuccess) {
            Log.d(TAG, "play: ---> tag 0-0 type=" + type);
            if (!TimeUtils.isIntervalExecutable(1, 150)) {
                Log.d(TAG, "play: ---> tag 0-1 this is too quick");
                return;
            }

            switch (type) {
                case 2:
                    url = url.split(";")[0];
                    break;
                case 3:
                    //destroyPlayer();
                    break;
            }

            Log.d(TAG, "play: ---> tag 0-1");
            mFPlayer = getFPlayer();
            Log.d(TAG, "play: ---> tag 0-2");
            mFPlayer.setSurface(this);
            Log.d(TAG, "play: ---> tag 0-3");
            mFPlayer.setDataSource(url);
            Log.d(TAG, "play: ---> tag 0-4");
            mFPlayer.seekTo(timestamp, type);
            Log.d(TAG, "play: ---> tag 0-5");
            mFPlayer.prepare();
        }
    }

    /**
     * 如果上面有一层view,suface将不会执行回调create
     */
    private void addCallback() {
        Log.d(TAG, "addCallback: ---> callback=" + mCallback);
        getHolder().addCallback(mCallback = new SurfaceHolderCallback());
    }

    private void removeCallback() {
        Log.d(TAG, "removeCallback: ---> tag 0-0 callback=" + mCallback);
        if (mCallback != null) {
            getHolder().removeCallback(mCallback);
            mCallback = null;
        }
    }

    @JavascriptInterface
    public void isMulticast(String path, String callback) {
        Log.d(TAG, "isMulticast: ---> tag0 path=" + path);
        Log.d(TAG, "isMulticast: ---> tag0 callback=" + callback);
        boolean isEnable = MulHelper.getInstance().isMulEnable();
        Log.d(TAG, "isMulticast: ---> mul enable=" + isEnable + " path=" + path);
        if (mBrowserActivity != null) {
            String js = "javascript:" + callback + "(" + isEnable + ")";
            Log.d(TAG, "isMulticast: ---> tag0 js=" + js);
            mBrowserActivity.callJs(js);
        }
    }

    @JavascriptInterface
    public void initMediaPlayer() {
        Log.d(TAG, "initMediaPlayer: --->");
        mFPlayer = getFPlayer();
        initHandler();
    }

    @JavascriptInterface
    public void setVideoDisplayArea(int left, int top, int right, int bottom) {
        Log.d(TAG, "setVideoDisplayArea: --->1 left=" + left + " top=" + top + " right=" + right + " bottom=" + bottom);

        float sx = getSclingX();
        float sy = getSclingY();
        Log.d(TAG, "setVideoDisplayArea: --->2 sx=" + sx + " sy=" + sy);

        this.mLeft = (int) (left * sx);
        this.mTop = (int) (top * sy);
        this.mRight = (int) (right * sx);
        this.mBottom = (int) (bottom * sy);

        Log.d(TAG, "setVideoDisplayArea: --->3 left=" + mLeft + " top=" + mTop + " right=" + mRight + " bottom=" + mBottom);
        updateSurfaceViewLayout();
    }


    @JavascriptInterface
    public void setDataSource(String url) {
        Log.d(TAG, "setDataSource: ---> tag 0 url=" + url);

        url = AppUtils.removeParamsFromUrl(url, "ChannelFCCPort", "ChannelFCCIP");
        Log.d(TAG, "setDataSource: ---> tag 1 url=" + url);

        if (TextUtils.equals(url, mDataSource)) {
            Log.d(TAG, "setDataSource: ---> return...");
            return;
        }
        mDataSource = url;
    }

    @JavascriptInterface
    public boolean isPlaying() {
        if (mFPlayer != null) {
            return mFPlayer.isPlaying();
        }
        return false;
    }

    @JavascriptInterface
    public void playByTime(int type, String timestamp, int speed) {
        Log.d(TAG, "playByTime: ---> tag 0-0 type=" + type + " timestamp=" + timestamp + " speed=" + speed);
        mCurPlayType = type;
        mTimestamp = timestamp;
        play(type, timestamp, mDataSource);

        Log.d(TAG, "playByTime: ---> tag 0-2");
        if (!mSurfaceCreateSuccess) {
            Log.d(TAG, "playByTime: ---> tag 0-3");
            addCallback();
            runOnUIThread(INVISIBLE);
            runOnUIThread(VISIBLE);
        }
    }

    @JavascriptInterface
    public void start() {
        resume();
    }

    @JavascriptInterface
    public void resume() {
        Log.d(TAG, "resume: --->");
        try {
            if (mFPlayer != null) {
                Log.d(TAG, "resume: ---> start");
                mFPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void pause() {
        Log.d(TAG, "pause: --->");
        if (mFPlayer != null && mFPlayer.isPlaying()) {
            mFPlayer.pause();
        }
    }

    @JavascriptInterface
    public void destroy() {
        Log.d(TAG, "destroy: --->");
        isMediaOverlay = false;
        destroyPlayer();
    }

    private void cleanHandler() {
        if (mHandler != null) {
            Log.d(TAG, "cleanHandler: ---> tag 0-1");
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
    }

    private void destroyPlayer() {
        Log.d(TAG, "destroyPlayer: ---> tag 0-0");
        try {
            if (mFPlayer != null) {
                mFPlayer.stop();
                mFPlayer.release();
                mFPlayer = null;
                Log.d(TAG, "destroyPlayer: ---> tag 0-1");
            }
        } catch (Exception e) {
            Log.d(TAG, "destroyPlayer: ---> tag 0-e msg=" + e.getMessage());
        }
    }

    @JavascriptInterface
    public long getDuration() {
        long dur = 0;
        try {
            if (mFPlayer != null) {
                dur = mFPlayer.getDuration();
            }
        } catch (Exception e) {
            Log.d(TAG, "getDuration: ex msg=" + e.getMessage());
        }
        Log.d(TAG, "getDuration: ---> tag 0-0 duration=" + dur);
        return dur;
    }

    @JavascriptInterface
    public long getCurrentPosition() {
        long position = 0;
        try {
            if (mFPlayer != null) {
                position = mFPlayer.getPosition();
            }
        } catch (Exception e) {
            Log.d(TAG, "getCurrentPosition: ---> ex msg=" + e.getMessage());
        }
        Log.d(TAG, "getCurrentPosition: ---> position=" + position);
        return position;
    }

    @JavascriptInterface
    public int[] getVideoDisplayArea() {
        int[] rect = new int[4];
        rect[0] = this.mLeft;
        rect[1] = this.mTop;
        rect[2] = this.mRight;
        rect[3] = this.mBottom;

        Log.d(TAG, "getVideoDisplayArea: ---> rect=" + JSON.toJSONString(rect));
        return rect;
    }

    /**
     * @param isMediaOverlay true 内容显示在播放窗体之上
     */
    @JavascriptInterface
    public void setMediaOverlay(boolean isMediaOverlay) {
        Log.d(TAG, "setMediaOverlay: ---> tag 0-0 isMediaOverlay=" + isMediaOverlay);
        if (this.isMediaOverlay == isMediaOverlay) {
            Log.d(TAG, "setMediaOverlay: ---> tag 0-1 return...");
            return;
        }
        this.isMediaOverlay = isMediaOverlay;
    }

    /**
     * 更新surface布局
     */
    private void updateSurfaceViewLayout() {
        Log.d(TAG, "updateSurfaceViewLayout: ---> tag 0-0 isMediaOverlay=" + isMediaOverlay);
        if (mBrowserActivity != null) {
            Log.d(TAG, "updateSurfaceViewLayout: ---> tag 0-0 run ui thread");
            mBrowserActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                    int[] rect = getVideoDisplayArea();
                    params.setMargins(rect[0], rect[1], rect[2], rect[3]);
                    setZOrderOnTop(isMediaOverlay);
                    setZOrderMediaOverlay(isMediaOverlay);

                    setLayoutParams(params);
                    Log.d(TAG, "updateSurfaceViewLayout: ---> tag 0-1");
                }
            });
        }
    }

    public class SurfaceHolderCallback implements SurfaceHolder.Callback {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "surfaceCreated: ---> media player=" + mFPlayer);
            mSurfaceCreateSuccess = true;
            mHandler.sendEmptyMessage(HANDLER_MSG_SURFACE_CREATE_SUCCESS);
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d(TAG, "surfaceChanged: ---> width=" + width + " height=" + height);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "surfaceDestroyed: --->");
            mSurfaceCreateSuccess = false;
        }
    }

    private RunOnUIThreadListener mRunOnUIThreadListener;

    private void setRunOnUIThreadListener(RunOnUIThreadListener listener) {
        this.mRunOnUIThreadListener = listener;
    }

    private interface RunOnUIThreadListener {
        void onUIThread(int what);
    }

    private static final float DESIGN_X = 1280f;
    private static final float DESIGN_Y = 720f;
    private DisplayMetrics mDisplayMetrics;
    private float mSclingX;
    private float mSclingY;

    private DisplayMetrics getDisplayMetrics(Context context) {
        mDisplayMetrics = context.getResources().getDisplayMetrics();
        Log.d(TAG, "LayoutHelper: --->分辨率 w=" + mDisplayMetrics.widthPixels + " h="
                + mDisplayMetrics.heightPixels + " densityDpi=" + mDisplayMetrics.densityDpi
                + " density=" + mDisplayMetrics.density);

        return mDisplayMetrics;
    }

    private float getSclingX() {
        if (mDisplayMetrics == null) {
            mDisplayMetrics = getDisplayMetrics(getContext());
        }

        if (mDisplayMetrics != null && mSclingX == 0) {
            mSclingX = mDisplayMetrics.widthPixels / DESIGN_X;
        }
        return mSclingX;
    }

    private float getSclingY() {
        if (mDisplayMetrics == null) {
            mDisplayMetrics = getDisplayMetrics(getContext());
        }

        if (mDisplayMetrics != null && mSclingY == 0) {
            mSclingY = mDisplayMetrics.heightPixels / DESIGN_Y;
        }
        return mSclingY;
    }
}
