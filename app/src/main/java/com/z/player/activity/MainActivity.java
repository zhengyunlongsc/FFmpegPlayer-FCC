package com.z.player.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alibaba.fastjson.JSON;
import com.z.player.BuildConfig;
import com.z.player.R;
import com.z.player.adapter.BaseRecyclerViewAdapter;
import com.z.player.adapter.ChannelListAdapter;
import com.z.player.bean.ChannelZT;
import com.z.player.bean.Login;
import com.z.player.common.Const;
import com.z.player.impl.FFPlayer;
import com.z.player.help.MulHelper;
import com.z.player.http.HttpResponse;
import com.z.player.http.HttpUtils;
import com.z.player.http.SimpleResponseCallback;
import com.z.player.interfaces.IPlayer;
import com.z.player.util.AppUtils;

import org.litepal.LitePal;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class MainActivity extends BaseActivity {
    private static final String TAG = MainActivity.class.getName();
    private static final int HANDLER_MSG_GET_CHANNEL_SUCCESS = 1;
    private static final int HANDLER_MSG_SURFACE_CREATE_SUCCESS = 2;
    private static final int HANDLER_MSG_HIDE_VIEW_CHANNEL = 3;
    private static final int HANDLER_MSG_UP_CHANNEL = 4;
    private static final int HANDLER_MSG_DOWN_CHANNEL = 5;
    private static final int HANDLER_MSG_SHOW_CHANNEL_LIST_VIEW = 6;
    private static final int HANDLER_MSG_HIDE_CHANNEL_LIST_VIEW = 7;
    private static final int HANDLER_MSG_INPUT_CHANNEL_NUM = 8;
    private static final int HANDLER_MSG_MUL_CHECK_SUCCESS = 9;
    private static final int HANDLER_MSG_LOGIN_SUCCESS = 10;
    private static final int HANDLER_MSG_CHECK_APP_UPDATE = 16;
    private static final int HANDLER_MSG_SHOW_VIEW_MULTICAST = 17;
    private static final int PERMISSION_READ_EXTERNAL_STORAGE = 0x1000;
    private static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 0x1001;

    private FrameLayout fl_surface;
    private SurfaceView surfaceView;
    private RecyclerView recycler_view;
    private LinearLayout ll_seekbar;
    private SeekBar mSeekBar;
    private TextView tv_seek_start;
    private TextView tv_seek_end;
    private TextView tv_channel;
    private TextView tv_channel_name;
    private LinearLayout ll_channel;

    private boolean mSurfaceCreateSuccess;
    private boolean mChannelDataGetSuccess;
    private boolean mMulCheckSuccess;
    private boolean mLoginSuccess;
    private boolean isProgressShow = true;
    private int mProgressCount;

    private GridLayoutManager mGridLayoutManager;
    private ChannelListAdapter mAdapter;

    private List<ChannelZT> mAllChannelList;
    private ChannelZT mCurChannel;

    private SimpleDateFormat mGmtTimeFormat;
    private SimpleDateFormat mUtcTimeFormat;
    private SimpleDateFormat mSimTimeFormat;
    private IPlayer mFPlayer;
    private Handler mHandler;
    private String inputChannelNum = "";
    private String mCurMulUrl;
    private String mCurUniUrl;
    private long time;
    private int mShowMulticastCount;
    private int mStreamMode;


    @Override
    public void init() {
        super.init();
        Log.d(TAG, "init: --->");
        if (Build.VERSION.SDK_INT >= 23) {// 6.0
            Log.d(TAG, "init: ---> permission");
            AppUtils.checkAndRequestPermissions(this, Manifest.permission.READ_EXTERNAL_STORAGE
                    , PERMISSION_READ_EXTERNAL_STORAGE);
            AppUtils.checkAndRequestPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE
                    , PERMISSION_WRITE_EXTERNAL_STORAGE);
        }

        mGridLayoutManager = new GridLayoutManager(this, 1);
        mSimTimeFormat = new SimpleDateFormat(Const.PATTERN_YMDHMS, Locale.CHINESE);
        mUtcTimeFormat = new SimpleDateFormat(Const.PATTERN_HMS, Locale.CHINESE);
        mGmtTimeFormat = new SimpleDateFormat(Const.PATTERN_HMS, Locale.CHINESE);
        mGmtTimeFormat.setTimeZone(TimeZone.getTimeZone(Const.TIME_ZONE));

        initHandler();
    }

    @Override
    public int initLayout() {
        return R.layout.activity_main;
    }

    @Override
    public void initView() {
        recycler_view = findViewById(R.id.recycler_view);
        surfaceView = findViewById(R.id.surface_view);
        mSeekBar = findViewById(R.id.pb_progress);
        ll_seekbar = findViewById(R.id.ll_seekbar);
        tv_seek_start = findViewById(R.id.tv_seek_start);
        tv_seek_end = findViewById(R.id.tv_seek_end);

        ll_channel = findViewById(R.id.ll_channel);
        tv_channel = findViewById(R.id.tv_channel);
        tv_channel_name = findViewById(R.id.tv_channel_name);

        fl_surface = findViewById(R.id.fl_surface);

        ll_seekbar.setVisibility(View.GONE);
    }

    @Override
    public void initListener() {
        fl_surface.setOnClickListener(this);

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(TAG, "surfaceCreated: --->");
                mSurfaceCreateSuccess = true;
                mHandler.sendEmptyMessage(HANDLER_MSG_SURFACE_CREATE_SUCCESS);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int i, int i1, int i2) {
                Log.d(TAG, "surfaceChanged: --->");
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d(TAG, "surfaceDestroyed: --->");
                mSurfaceCreateSuccess = false;
            }
        });
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                int pro = seekBar.getProgress();
                Log.d(TAG, "onSeeking: ---> tag 0-0 progress=" + pro);
                onSeekPlay();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                Log.d(TAG, "onStartTrackingTouch: ---> seekto=" + seekBar.getProgress());
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.d(TAG, "onStopTrackingTouch: ---> seekto=" + seekBar.getProgress());
            }
        });

        recycler_view.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                switch (newState) {
                    case RecyclerView.SCROLL_STATE_DRAGGING:
                        mHandler.removeMessages(HANDLER_MSG_HIDE_CHANNEL_LIST_VIEW);
                        break;
                    case RecyclerView.SCROLL_STATE_IDLE:
                        mHandler.sendEmptyMessageDelayed(HANDLER_MSG_HIDE_CHANNEL_LIST_VIEW, 5000);
                        break;
                }
            }
        });
    }

    @Override
    public void initData() {
        login();
    }

    private void initHandler() {
        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Log.d(TAG, "handleMessage: ---> tag what=" + msg.what);
                switch (msg.what) {
                    case HANDLER_MSG_GET_CHANNEL_SUCCESS:
                        //ChannelSorter.sort(mAllChannelList);
                        checkMulticast(getChannel(0));
                        mAdapter = new ChannelListAdapter(MainActivity.this, mAllChannelList);
                        recycler_view.setLayoutManager(mGridLayoutManager);
                        recycler_view.setAdapter(mAdapter);
                        setOnItemClickListener();
                        mChannelDataGetSuccess = true;
                        playByTime(1, null, mCurChannel);
                        break;
                    case HANDLER_MSG_CHECK_APP_UPDATE:
                    case HANDLER_MSG_UP_CHANNEL:
                    case HANDLER_MSG_DOWN_CHANNEL:
                    case HANDLER_MSG_SURFACE_CREATE_SUCCESS:
                    case HANDLER_MSG_MUL_CHECK_SUCCESS:
                        playByTime(1, null, mCurChannel);
                        break;
                    case HANDLER_MSG_LOGIN_SUCCESS:
                        mLoginSuccess = true;
                        playByTime(1, null, mCurChannel);
                        break;
                    case HANDLER_MSG_SHOW_CHANNEL_LIST_VIEW:
                        if (recycler_view.getVisibility() != View.VISIBLE) {
                            recycler_view.setVisibility(View.VISIBLE);
                            selectListViewItem(-1);
                        }

                        mHandler.removeMessages(HANDLER_MSG_HIDE_CHANNEL_LIST_VIEW);
                        mHandler.sendEmptyMessageDelayed(HANDLER_MSG_HIDE_CHANNEL_LIST_VIEW, 5000);
                        break;
                    case HANDLER_MSG_HIDE_CHANNEL_LIST_VIEW:
                        recycler_view.setVisibility(View.GONE);
                        break;
                    case HANDLER_MSG_HIDE_VIEW_CHANNEL:
                        ll_channel.setVisibility(View.GONE);
                        break;
                    case HANDLER_MSG_INPUT_CHANNEL_NUM:
                        inputChannelNum = "";
                        playByTime(1, null, mCurChannel);
                        break;
                    case HANDLER_MSG_SHOW_VIEW_MULTICAST:
                        mShowMulticastCount = 0;
                        break;
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ---> tag 0-0");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: ---> tag 0-0");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: ---> tag 0-0");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: ---> tag 0-0");
        destroyPlayer();
    }

    private void checkMulticast(ChannelZT c) {
        if (c != null) {
            String multicastUrl = c.multicastUrl;
            if (!TextUtils.isEmpty(multicastUrl)) {
                String[] host = multicastUrl.replace("rtp://", "").replace("igmp://", "").split(":");
                Log.d(TAG, "checkMulticast: ---> host=" + JSON.toJSONString(host));
                if (host != null && !TextUtils.isEmpty(host[0]) && !TextUtils.isEmpty(host[1])) {
                    MulHelper.getInstance().checkMulEnable(host[0], Integer.parseInt(host[1]), new MulHelper.OnMulCallback() {
                        @Override
                        public void onCallback(boolean b) {
                            mMulCheckSuccess = true;
                            mHandler.sendEmptyMessage(HANDLER_MSG_MUL_CHECK_SUCCESS);
                            Log.d(TAG, "onCallback: ---> mul enable=" + b);
                        }
                    }, 200);
                }
            }
        }
    }

    private IPlayer getFPlayer() {
        mFPlayer = FFPlayer.getInstance();
        mFPlayer.setLogEnable(Const.KEY_LOG_ENABLE);
        mFPlayer.setMediaCodec(true);
        mFPlayer.setOnPlayerListener(new IPlayer.OnPlayerListener() {
            @Override
            public void onCallback(int code, String message) {
                Log.d(TAG, "onCallback: ---> tag 0-0 code=" + code + " msg=" + message);
            }

            @Override
            public void onPrepared() {
                if (surfaceView != null) {
                    Surface surface = surfaceView.getHolder().getSurface();
                    if (surface == null || !surface.isValid()) {
                        Log.d(TAG, "onPrepare: ---> Surface is invalid or destroyed!");
                    }
                }

                int playType = mFPlayer.getPlayType();
                long duration = mFPlayer.getDuration();
                long position = mFPlayer.getPosition();
                Log.d(TAG, "onPrepare: ---> type=" + playType + " dur=" + duration + " pos=" + position);

                switch (playType) {
                    case 1:
                        mSeekBar.setMax(Const.MAX_TIME_SHIFT);
                        setSeekBar(mSeekBar.getMax() - position);
                        break;
                    case 2:
                        setSeekBar(mSeekBar.getMax() - Math.abs(position));
                        break;
                    case 3:
                    case 4:
                        mSeekBar.setMax((int) duration);
                        setSeekBar(position);
                        break;

                }

                mFPlayer.start();
            }

            @Override
            public void onProgress(double progress) {
                Log.d(TAG, "onProgress: --->");
            }

            @Override
            public void onStart() {
                Log.d(TAG, "onStart: ---> tag 0-0");
            }

            @Override
            public void onCompletion() {
                Log.d(TAG, "onCompletion: ---> tag 0-0");
            }
        });

        return mFPlayer;
    }

    private void selectListViewItem(int index) {
        if (index == -1) {
            index = getCurChannelIndex();
        }
        int finalIndex = index;

        recycler_view.scrollToPosition(index);
        recycler_view.postDelayed(new Runnable() {
            @Override
            public void run() {
                RecyclerView.ViewHolder viewHolder = recycler_view.findViewHolderForAdapterPosition(finalIndex);
                if (viewHolder != null) {
                    View itemView = viewHolder.itemView;
                    itemView.requestFocusFromTouch();
                }
            }
        }, 0);
    }

    private void setOnItemClickListener() {
        mAdapter.setOnItemClickListener(new BaseRecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(RecyclerView.ViewHolder holder, View view, int position) {
                ChannelZT c = mAllChannelList.get(position);
                setChannelText(c);
                playByTime(1, null, c);
            }
        });
    }

    public void login() {
        Login login = LitePal.findFirst(Login.class);
        Map<String, String> params = new HashMap<>();
        params.put("tvMac", AppUtils.getMacAddress());
        params.put("tvSn", AppUtils.getDeviceSerialNum());
        params.put("tvModel", AppUtils.getModelName());
        params.put("hotelCode", Const.HOTEL_CODE);
        params.put("tvRandomCode", login == null ? "" : login.code);
        HttpUtils.post(Const.LOGIN, JSON.toJSONString(params), new SimpleResponseCallback<HttpResponse<String>>() {
            @Override
            public void onSucceed(HttpResponse<String> info) {
                super.onSucceed(info);
                Log.d(TAG, "onSucceed: ---> login res=" + info);
                if (info != null) {
                    if (info.code == 200) {
                        String code = info.data;
                        String token = info.token;
                        String name = info.hotelName;
                        int mode = info.communicationMode;
                        MulHelper.getInstance().setStreamMode(mode);

                        Login l = new Login();
                        l.code = code;
                        l.token = token;
                        l.hotelName = name;
                        l.saveOrUpdate();

                        if (!TextUtils.isEmpty(code) && TextUtils.isEmpty(token)) {
                            login();
                            return;
                        }

                        if (!TextUtils.isEmpty(code) && !TextUtils.isEmpty(token)) {
                            HttpUtils.setToken(info.token);
                            mHandler.sendEmptyMessage(HANDLER_MSG_LOGIN_SUCCESS);
                            getChannels();
                        }
                    }
                }
            }
        });
    }

    private boolean hasChannelData() {
        if (mAllChannelList != null && mAllChannelList.size() > 0) {
            return true;
        }

        return false;
    }

    private void getChannels() {
        Log.d(TAG, "getChannels: ---> tag 0-0");
        Map<String, String> params = new HashMap<>();
        params.put("hotelCode", Const.HOTEL_CODE);
        params.put("tvMac", AppUtils.getMacAddress());
        params.put("tvSn", AppUtils.getDeviceSerialNum());
        params.put("tvModel", AppUtils.getModelName());

        HttpUtils.get(Const.GET_CHANNEL, params, new SimpleResponseCallback<HttpResponse<List<ChannelZT>>>() {
            @Override
            public void onSucceed(HttpResponse<List<ChannelZT>> response) {
                super.onSucceed(response);
                Log.d(TAG, "onSucceed: ---> channels=" + response);
                if (response != null) {
                    mAllChannelList = response.data;
                    mHandler.sendEmptyMessage(HANDLER_MSG_GET_CHANNEL_SUCCESS);
                }
            }
        });
    }

    /**
     * 设置进度条
     *
     * @param position 秒
     */
    private void setSeekBar(long position) {
        Log.d(TAG, "setSeekBar: ---> position=" + position);
        if (mSeekBar != null && mFPlayer != null) {
            long millis = position * 1000L;
            String startText = mGmtTimeFormat.format(millis);
            String endText = mGmtTimeFormat.format(mSeekBar.getMax() * 1000);
            String popupText = mGmtTimeFormat.format(millis);

            int type = mFPlayer.getPlayType();
            Log.d(TAG, "setSeekBar: ---> tag 0-1 pro millis=" + millis + " type=" + type);

            switch (type) {
                case 1:
                case 2:
                    mSeekBar.setProgress((int) position);
                    Date date = new Date();
                    long time = date.getTime();
                    time -= (Const.MAX_TIME_SHIFT - position) * 1000L;

                    Date sDate = new Date(time);
                    Date eDate = new Date();
                    startText = mUtcTimeFormat.format(sDate);
                    endText = mUtcTimeFormat.format(eDate);
                    popupText = mUtcTimeFormat.format(sDate);
                    break;
                case 3:
                case 4:
                    mSeekBar.setProgress((int) position);
                    break;
            }

            Log.d(TAG, "setSeekBar: ---> start=" + startText + " end=" + endText + " text=" + popupText
                    + " progress=" + mSeekBar.getProgress() + " max=" + mSeekBar.getMax());

            tv_seek_start.setText(startText);
            tv_seek_end.setText(endText);
        }
    }

    private void playByTime(int type, String timestamp, ChannelZT c) {
        Log.d(TAG, "playByTime: ---> tag 0-0 channel is not null " + (mAllChannelList != null));
        Log.d(TAG, "playByTime: ---> tag 0-0 surface create succ " + mSurfaceCreateSuccess);
        Log.d(TAG, "playByTime: ---> tag 0-0 multicast check succ " + mMulCheckSuccess);
        Log.d(TAG, "playByTime: ---> tag 0-0 login succ " + mLoginSuccess);
        Log.d(TAG, "playByTime: ---> tag 0-0 is debug " + BuildConfig.DEBUG);
        if (hasChannelData() && mSurfaceCreateSuccess && mMulCheckSuccess
                && mChannelDataGetSuccess && mLoginSuccess) {
            Log.d(TAG, "playByTime: ---> tag 0-1");
            if (c == null) {
                c = mAllChannelList.get(0);
            }
            mCurChannel = c;
            Log.d(TAG, "playByTime: ---> tag 0-2 type=" + type + " timestamp=" + timestamp);
            if (c != null) {
                hideSeekBar(type == 1);
                setChannelText(c);

                String multicastUrl = c.multicastUrl;
                String playUrl = TextUtils.isEmpty(c.playUrl) ? c.unicastUrl : c.playUrl;
                Log.d(TAG, "playByTime: ---> tag 0-0 playUrl=" + playUrl);

                if (!TextUtils.isEmpty(playUrl)) {
                    playUrl = AppUtils.parseUrlParams(playUrl.split(";")[0]);
                }

                Log.d(TAG, "playByTime: ---> tag 0-0 multicastUrl=" + multicastUrl);
                Log.d(TAG, "playByTime: ---> tag 0-1 playUrl=" + playUrl);

                String url = playUrl + ";" + multicastUrl;
                //url="rtsp://123.147.112.17:8089/04000001/00000001000000020000000009473384";//三个流

                /*url = "rtsp://123.147.112.17:8089/04000001/00000001000000020000000009264663?AuthInfo=xxx&userid=test04@ziptv";
                type = 4;*/

                /*url = "rtsp://123.147.112.17:8089/04000001/01000000004000000000000000000231?AuthInfo=xxx&Playtype=1&Playseek=20250330160000-20250330170600";
                type = 3;*/

                /*url = "rtsp://123.147.112.17:8089/04000001/02000000000000000000000094459544?AuthInfo=xxx";
                type = 3;*/

                Log.d(TAG, "playByTime: ---> tag 0-3 url=" + url);

                mCurMulUrl = multicastUrl;
                mCurUniUrl = playUrl;

                switch (type) {
                    case 2:
                        url = mCurUniUrl.split(";")[0];
                        break;
                    case 4:
                        destroyPlayer();
                        break;
                }

                Log.d(TAG, "playByTime: ---> tag 0-4 type=" + type + " source=" + url);
                mFPlayer = getFPlayer();
                mFPlayer.setSurface(surfaceView);
                //mFPlayer.setDataSource(url);
                mFPlayer.setDataSource(mCurMulUrl.replace("rtp://","fcc://"));
                mFPlayer.seekTo(timestamp, type);
                mFPlayer.prepare();
            }
        }
    }

    private ChannelZT getChannel(int i) {
        int index = getCurChannelIndex() + i;
        if (index == mAllChannelList.size() - 1) {
            index = 0;
        }

        if (index < 0) {
            index = mAllChannelList.size() - 1;
        }

        return mAllChannelList.get(index);
    }

    private int getCurChannelIndex() {
        if (mAllChannelList != null && mAllChannelList.size() > 0) {
            for (int i = 0; i < mAllChannelList.size(); i++) {
                ChannelZT c = mAllChannelList.get(i);
                if (c == mCurChannel) {
                    return i;
                }
            }
        }

        return 0;
    }

    private void onSeekPlay() {
        if (mHandler != null && mProgressCount > 0) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler.postDelayed(new Runnable() {

                @Override
                public void run() {
                    mProgressCount = 0;
                    if (mSeekBar != null) {
                        int seek = mSeekBar.getProgress();
                        int diff = mSeekBar.getMax() - seek;
                        Log.d(TAG, "onSeekPlay: ---> tag 0-0 seek=" + seek + " diff=" + diff);

                        if (mFPlayer != null) {
                            int type = mFPlayer.getPlayType();
                            switch (type) {
                                case 1:
                                case 2:
                                    if (diff <= 5) {
                                        Log.d(TAG, "onSeekPlay: ---> switch live...");
                                        playByTime(1, null, mCurChannel);
                                        return;
                                    } else if (seek <= 0) {
                                        seek = 5;
                                    }

                                    Log.d(TAG, "onSeekPlay: ---> tag 0-2 seek=" + seek);
                                    String timeShitAbt = getTimeShiftAbt(seek);
                                    Log.d(TAG, "onSeekPlay: ---> tag 0-2-1 timeShitAbt=" + timeShitAbt + " time shit diff=" + (-diff));
                                    playByTime(2, String.valueOf(-diff), mCurChannel);
                                    break;
                                case 3://回看
                                case 4://点播
                                    playByTime(type, String.valueOf(seek), mCurChannel);
                                    break;
                            }
                        }

                        showProgressView();
                    }
                }
            }, 1000);
        }
    }

    private String getTimeShiftAbt(int seek) {
        Date now = new Date();//"20241226T143000Z"
        now.setTime(now.getTime() - (Const.MAX_TIME_SHIFT - seek) * 1000L);
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'", Locale.CHINESE);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        String time = format.format(now);
        Log.d(TAG, "getTimeShiftAbt: ---> tag 0-0 time=" + time);
        return time;
    }


    private void showProgressView() {
        Log.d(TAG, "showProgressView: ---> tag0-0");
        ll_seekbar.setVisibility(View.VISIBLE);
        mHandler.removeCallbacksAndMessages(null);
        mHandler.postDelayed(new Runnable() {
            int count = 0;

            @Override
            public void run() {
                if (count >= 5) {
                    hideSeekBar(false);
                    return;
                }

                if (mFPlayer != null && mFPlayer.isPlaying()) {
                    int type = mFPlayer.getPlayType();
                    long position = mFPlayer.getPosition();
                    Log.d(TAG, "showProgressView: ---> onSeeking pos=" + position + " type=" + type);
                    switch (type) {
                        case 1:
                        case 2:
                            position = Const.MAX_TIME_SHIFT - Math.abs(position);
                            break;
                    }

                    setSeekBar(position);
                }

                count++;
                mHandler.postDelayed(this, 1000);
            }
        }, 1000);
    }

    private void hideSeekBar(boolean reset) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mSeekBar != null) {
                    if (reset) {
                        Log.d(TAG, "hideSeekBar: ---> reset seek bar 7200");
                        mSeekBar.setProgress(mSeekBar.getMax());
                    }
                }

                if (mHandler != null) {
                    Log.d(TAG, "hideSeekBar: ---> tag 0-2 hide channel");
                    mHandler.removeCallbacksAndMessages(null);
                    mHandler.sendEmptyMessage(HANDLER_MSG_HIDE_VIEW_CHANNEL);
                }

                Log.d(TAG, "hideSeekBar: ---> tag 0-2 hide seek bar");
                ll_seekbar.setVisibility(View.GONE);
            }
        });
    }

    private boolean onClickLeftOrRight(int state) {
        if (ll_seekbar.getVisibility() != View.VISIBLE) {
            showProgressView();
            return true;
        }

        if (ll_seekbar.getVisibility() == View.VISIBLE) {
            isProgressShow = false;
            if (state < 0) {
                mProgressCount++;
                int progress = mSeekBar.getProgress() - Const.STEP_INTERVAL - mProgressCount;
                Log.d(TAG, "onClickLeftOrRight: ---> tag 0-0 progress=" + progress);
                if (progress < 0) {
                    Log.d(TAG, "onClickLeftOrRight: ---> tag0");
                    progress = 0;
                }
                Log.d(TAG, "onClickLeftOrRight: ---> tag2 progress=" + progress);
                setSeekBar(progress);
                return true;
            } else {
                mProgressCount++;
                int progress = mSeekBar.getProgress() + Const.STEP_INTERVAL + mProgressCount;
                Log.d(TAG, "onClickLeftOrRight: ---> tag 0-1 progress=" + progress);
                if (progress >= mSeekBar.getMax()) {
                    progress = mSeekBar.getMax();
                    Log.d(TAG, "onClickLeftOrRight: ---> tag1");
                }

                Log.d(TAG, "onClickLeftOrRight: ---> tag3 progress=" + progress);
                setSeekBar(progress);
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown: ---> tag keyCode=" + keyCode);
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_BACK:
                case KeyEvent.KEYCODE_DEL:
                    if (recycler_view.getVisibility() == View.VISIBLE) {
                        recycler_view.setVisibility(View.GONE);
                        return true;
                    }

                    if (isExit()) {
                        appExit();
                    } else {
                        return true;
                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_UP:
                    if (recycler_view.getVisibility() == View.VISIBLE) {
                        mHandler.removeMessages(HANDLER_MSG_HIDE_CHANNEL_LIST_VIEW);
                        mHandler.sendEmptyMessageDelayed(HANDLER_MSG_HIDE_CHANNEL_LIST_VIEW, 5000);
                        break;
                    }
                    mCurChannel = getChannel(1);
                    setChannelText(mCurChannel);
                    mHandler.removeMessages(HANDLER_MSG_HIDE_VIEW_CHANNEL);
                    mHandler.removeMessages(HANDLER_MSG_UP_CHANNEL);
                    mHandler.sendEmptyMessageDelayed(HANDLER_MSG_UP_CHANNEL, 200);
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if (recycler_view.getVisibility() == View.VISIBLE) {
                        mHandler.removeMessages(HANDLER_MSG_HIDE_CHANNEL_LIST_VIEW);
                        mHandler.sendEmptyMessageDelayed(HANDLER_MSG_HIDE_CHANNEL_LIST_VIEW, 5000);
                        break;
                    }

                    mCurChannel = getChannel(-1);
                    setChannelText(mCurChannel);
                    mHandler.removeMessages(HANDLER_MSG_HIDE_VIEW_CHANNEL);
                    mHandler.removeMessages(HANDLER_MSG_DOWN_CHANNEL);
                    mHandler.sendEmptyMessageDelayed(HANDLER_MSG_DOWN_CHANNEL, 200);
                    break;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    onClickLeftOrRight(-1);
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    onClickLeftOrRight(1);
                    /*mShowMulticastCount++;
                    if (mShowMulticastCount == 6) {
                        mShowMulticastCount = 0;
                        tv_inet_address.setVisibility(View.VISIBLE);
                    }
                    mHandler.removeMessages(HANDLER_MSG_SHOW_VIEW_MULTICAST);
                    mHandler.sendEmptyMessageDelayed(HANDLER_MSG_SHOW_VIEW_MULTICAST, 5000);*/
                    break;
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    mHandler.sendEmptyMessage(HANDLER_MSG_SHOW_CHANNEL_LIST_VIEW);
                    break;
                case KeyEvent.KEYCODE_0:
                case KeyEvent.KEYCODE_1:
                case KeyEvent.KEYCODE_2:
                case KeyEvent.KEYCODE_3:
                case KeyEvent.KEYCODE_4:
                case KeyEvent.KEYCODE_5:
                case KeyEvent.KEYCODE_6:
                case KeyEvent.KEYCODE_7:
                case KeyEvent.KEYCODE_8:
                case KeyEvent.KEYCODE_9:
                    inputChannelNum += (keyCode - 7);

                    ChannelZT cc = null;
                    for (int i = 0; i < mAllChannelList.size(); i++) {
                        ChannelZT c = mAllChannelList.get(i);
                        if (TextUtils.equals(inputChannelNum, c.channelNumber)) {
                            cc = c;
                            break;
                        }
                    }

                    if (cc == null) {
                        cc = new ChannelZT();
                        cc.channelNumber = inputChannelNum;
                        cc.name = "频道不存在";
                        inputChannelNum = "";
                        mHandler.removeMessages(HANDLER_MSG_INPUT_CHANNEL_NUM);
                    } else {
                        mCurChannel = cc;
                        mHandler.removeMessages(HANDLER_MSG_INPUT_CHANNEL_NUM);
                        mHandler.sendEmptyMessageDelayed(HANDLER_MSG_INPUT_CHANNEL_NUM, 800);
                    }

                    setChannelText(cc);
                    break;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void setChannelText(ChannelZT c) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tv_channel.setText(c.channelNumber);
                tv_channel_name.setText(TextUtils.isEmpty(c.name) ? c.channelName : c.name);
                ll_channel.setVisibility(View.VISIBLE);
            }
        });
        mHandler.removeMessages(HANDLER_MSG_HIDE_VIEW_CHANNEL);
        mHandler.sendEmptyMessageDelayed(HANDLER_MSG_HIDE_VIEW_CHANNEL, 3000);
    }

    public boolean isExit() {
        long t = System.currentTimeMillis() - time;
        Log.d(TAG, "isExit: ---> tag t=" + t);
        if (t < 2000) {
            return true;
        }

        time = System.currentTimeMillis();
        Toast.makeText(this, "再按一次退出", Toast.LENGTH_LONG).show();
        return false;
    }

    @Override
    public void onClick(View view) {
        Log.d(TAG, "onClick: ---> view id=" + view.getId() + " visibility=" + recycler_view.getVisibility());
        switch (view.getId()) {
            case R.id.fl_surface:
                if (recycler_view.getVisibility() == View.VISIBLE) {
                    recycler_view.setVisibility(View.GONE);
                } else {
                    mHandler.sendEmptyMessage(HANDLER_MSG_SHOW_CHANNEL_LIST_VIEW);
                }
                break;
        }
    }

    private void destroyPlayer() {
        if (mFPlayer != null) {
            mFPlayer.stop();
            mFPlayer.release();
            mFPlayer = null;
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: ---jni_log");
        destroyPlayer();
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: ---> requestCode=" + requestCode);
        switch (requestCode) {
            case PERMISSION_READ_EXTERNAL_STORAGE:
            case PERMISSION_WRITE_EXTERNAL_STORAGE:
                if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "权限拒绝!" + requestCode, Toast.LENGTH_LONG).show();
                } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED
                        && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "onRequestPermissionsResult: --> to play..");
                    playByTime(1, null, mCurChannel);
                }
                break;
        }
    }
}
