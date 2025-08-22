package com.z.player.activity2;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.z.player.common.Const;
import com.z.player.help.MulHelper;
import com.z.player.help.ActivityCollector;
import com.z.player.js.AndroidJs;
import com.z.player.receiver.KeyPressReceiver;
import com.z.player.receiver.NetworkChangeReceiver;
import com.z.player.util.AppUtils;

import java.util.Iterator;
import java.util.Map;

/**
 * 作者: zyl on 2020/4/15 9:29
 * 修改人:
 * 修改描述:
 * 修改日期:
 */
public abstract class BaseActivity extends Activity {
    public static String TAG;
    private static final int UPDATE_TIME = 1;
    private NetworkChangeReceiver mNetworkReceiver;
    private KeyPressReceiver mKeyPressReceiver;
    private BroadcastReceiver mAidlReceive;

    private Handler mHandler = new Handler(Looper.myLooper()) {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case UPDATE_TIME:
                    mHandler.sendEmptyMessageDelayed(UPDATE_TIME, 30 * 1000);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TAG = BaseActivity.class.getName();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(initLayout());
        checkMulticast();
    }

    private void checkMulticast() {
        String url = "rtp://225.0.4.74:7980";
        if (!TextUtils.isEmpty(url)) {
            String[] host = url.replace("rtp://", "").replace("igmp://", "").split(":");
            Log.d(TAG, "checkMulticast: ---> host=" + JSON.toJSONString(host));
            if (!TextUtils.isEmpty(host[0]) && !TextUtils.isEmpty(host[1])) {
                MulHelper.getInstance().checkMulEnable(host[0], Integer.parseInt(host[1]), new MulHelper.OnMulCallback() {
                    @Override
                    public void onCallback(boolean b) {
                        Log.d(TAG, "onCallback: ---> mul enable=" + b);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                init();
                                initView();
                                initListener();
                                initData();
                            }
                        });
                    }
                }, 200);
            }
        }
    }

    public void init() {
        addActivity();
    }

    public abstract int initLayout();

    public void initView() {
    }

    public void initListener() {
        //registerKeyPressListener();
        registerAidlReceiver();
    }

    public void initData() {
    }

    public void callJs(String js) {
    }

    public void addActivity() {
        ActivityCollector.addActivity(this);
    }

    private void registerAidlReceiver() {
        mAidlReceive = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null) {
                    Log.d(TAG, "onReceive: ---> aidl tag 0-0 return...");
                    return;
                }

                AndroidJs js = getAndroidJs();
                if (js == null) {
                    Log.d(TAG, "onReceive: ---> aidl tag 0-1 return...");
                    return;
                }

                String action = intent.getStringExtra(Const.KEY);
                String result = intent.getStringExtra(Const.RESULT);

                Map<String, String> map = js.getAidlMap();
                Iterator<Map.Entry<String, String>> iterator = map.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<String, String> obj = iterator.next();
                    String key = obj.getKey();
                    String value = obj.getValue();
                    Log.d(TAG, "onReceive: ---> aidl tag 0-2 key=" + key + " value=" + value);

                    if (TextUtils.equals(action, key)) {
                        iterator.remove();  // 使用 Iterator 的 remove 方法来删除元素
                        String method = "javascript:" + value + "(" + result + ")";
                        Log.d(TAG, "onReceive: ---> aidl tag 0-3 method=" + method);
                        callJs(method);
                        break;  // 找到匹配的键后退出循环
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Const.RECEIVE_ACTION_OTT_UNICOM);
        registerReceiver(mAidlReceive, filter);
    }

    private void registerKeyPressListener() {
        mKeyPressReceiver = new KeyPressReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                super.onReceive(context, intent);
                KeyEvent keyEvent = intent.getParcelableExtra(Const.KEY_EVENT);
                if (keyEvent != null) {
                    int action = keyEvent.getAction();
                    int keyCode = keyEvent.getKeyCode();
                    Log.d(TAG, "onReceive: ---> tag action=" + action + " keycode=" + keyCode);
                    callJs("javascript:keyEvent(" + keyCode + ")");

                    switch (keyCode) {
                        case 3:
                            finish();
                            break;
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Const.KEY_PRESS_ACTION);
        registerReceiver(mKeyPressReceiver, filter);
    }

    public void registerNetworkListener() {
        mNetworkReceiver = new NetworkChangeReceiver() {
            @Override
            public void onChange(Context context, Intent intent) {
                super.onChange(context, intent);
                if (intent != null) {
                    String action = intent.getAction();
                    if (TextUtils.equals(ConnectivityManager.CONNECTIVITY_ACTION, action)) {
                        boolean isConnected = AppUtils.isNetworkConnected(getApplicationContext());
                        onNetworkChange(isConnected);
                    }
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mNetworkReceiver, intentFilter);
    }

    public AndroidJs getAndroidJs() {
        return null;
    }

    public void onNetworkChange(boolean isConnected) {
        Log.d(TAG, "---> onNetworkChange: ---> isConnected=" + isConnected);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "---> onKeyDown: ---> keyCode=" + keyCode + " action=" + event.getAction());
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            this.finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "---> onDestroy: --->");
        if (mAidlReceive != null) {
            unregisterReceiver(mAidlReceive);
        }

        if (mNetworkReceiver != null) {
            unregisterReceiver(mNetworkReceiver);
        }

        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }

        if (mKeyPressReceiver != null) {
            unregisterReceiver(mKeyPressReceiver);
        }

        ActivityCollector.removeActivity(this);
        super.onDestroy();
    }

    public void showToast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }
}
