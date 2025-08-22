package com.z.player.js;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.alibaba.fastjson.JSON;
import com.z.player.App;
import com.z.player.activity2.BrowserActivity;
import com.z.player.help.ActivityCollector;
import com.z.player.http.HttpUtils;
import com.z.player.http.SimpleResponseCallback;
import com.z.player.util.AppUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 作者: zyl on 2019/7/18 16:08
 * 描述: ${DESCRIPTION}
 * 修改人:
 * 修改描述:
 * 修改日期:
 */
public class AndroidJs {
    private static final String TAG = AndroidJs.class.getName();
    private boolean mLiveFlag;
    private Context mContext;
    private Map<String, String> mMap;

    public AndroidJs(Context context) {
        this.mContext = context;
        this.mMap = new HashMap<>();
    }

    public Map<String, String> getAidlMap() {
        return mMap;
    }

    @JavascriptInterface
    public void reboot() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                PowerManager pm = (PowerManager) App.getInstance().getSystemService(Context.POWER_SERVICE);
                pm.reboot("");
            }
        });
    }

    @JavascriptInterface
    public void finish() {
        Log.d(TAG, "finish: ---> 1");
        if (mContext instanceof BrowserActivity) {
            Log.d(TAG, "finish: ---> 2");
            BrowserActivity activity = (BrowserActivity) mContext;
            activity.finish();
        }
    }

    @JavascriptInterface
    public void finishEPG() {
        Log.d(TAG, "finishEPG: ---> tag0 close epg");
        exitApp();
    }

    @JavascriptInterface
    public void exitApp() {
        Log.d(TAG, "exitApp: ---> tag0 exit");
        ActivityCollector.finishAll();
    }

    @JavascriptInterface
    public void log(String log) {
        Log.d(TAG, "log: ---> debug log===" + log);
    }


    /**
     * 获取序列号
     *
     * @return
     */
    @JavascriptInterface
    public String getSTBId() {
        return AppUtils.getDeviceSerialNum();
    }

    /**
     * 获取MAC
     *
     * @return
     */
    @JavascriptInterface
    public String getMAC() {
        return AppUtils.getMacAddress();
    }

    /**
     * 获取IP地址
     *
     * @return
     */
    @JavascriptInterface
    public String getIP() {
        return AppUtils.getLocalIp();
    }

    /**
     * 获取型号
     *
     * @return
     */
    @JavascriptInterface
    public String getDeviceModel() {
        return AppUtils.getDeviceModel();
    }

    /**
     * 获取rom版本
     *
     * @return
     */
    @JavascriptInterface
    public String getRomVersion() {
        return AppUtils.getRomVersion();
    }

    /**
     * 获取app版本
     *
     * @return
     */
    @JavascriptInterface
    public String getAppVersion() {
        return AppUtils.getVersionName();
    }

    /**
     * 获取app版本-versionName(通过app包名)
     *
     * @return
     */
    @JavascriptInterface
    public String getAppVersionByPackageName(String packageName) {
        return AppUtils.getVersionName(packageName);
    }

    /**
     * js方法回调
     */
    @JavascriptInterface
    public void jsCallbackMethod(String result) {
        Log.d(TAG, "jsCallbackMethod: --->" + result);
    }

    @JavascriptInterface
    public boolean getLiveFlag() {
        Log.d(TAG, "getLiveFlag: ---> return false.....");
        return mLiveFlag;
    }

    @JavascriptInterface
    public void setLiveFlag(boolean after) {
        Log.d(TAG, "setLiveFlag: ---> flag=" + after);
        mLiveFlag = after;
    }

    /**
     * @param streamType 1:系统音量;3:音乐音量
     */
    @JavascriptInterface
    public int getStreamVolume(int streamType) {
        AudioManager am = (AudioManager) App.getInstance().getSystemService(Context.AUDIO_SERVICE);
        int volume = am.getStreamVolume(streamType);
        Log.d(TAG, "getStreamVolume: ---> volume=" + volume);
        return volume;
    }

    /**
     * 获取最大音量
     *
     * @param streamType 1:系统最大音量;3:音乐最大音量
     */
    @JavascriptInterface
    public int getStreamMaxVolume(int streamType) {
        AudioManager am = (AudioManager) App.getInstance().getSystemService(Context.AUDIO_SERVICE);
        int volume = am.getStreamMaxVolume(streamType);
        Log.d(TAG, "getStreamMaxVolume: ---> volume=" + volume);
        return volume;
    }

    /**
     * 判断app是否安装
     *
     * @param packageName 包名
     * @return
     */
    @JavascriptInterface
    public boolean isInstallApp(String packageName) {
        return AppUtils.isInstallApp(App.getInstance().getApplicationContext(), packageName);
    }


    @JavascriptInterface
    public void setKeyValue(String key, String value) {
        AppUtils.setKeyValue(key, value);
    }

    @JavascriptInterface
    public String getKeyValue(String key) {
        return AppUtils.getKeyValue(key);
    }

    @JavascriptInterface
    public String getVersionName() {
        return AppUtils.getVersionName();
    }

    @JavascriptInterface
    public int getVersionCode() {
        return AppUtils.getVersionCode();
    }

    private void callJs(String method) {
        if (mContext instanceof BrowserActivity && !TextUtils.isEmpty(method)) {
            BrowserActivity activity = (BrowserActivity) mContext;
            activity.callJs(method);
        }
    }

    @JavascriptInterface
    public void httpAjaxGet(String url, String callback) {
        Log.d(TAG, "httpAjaxGet: ---> method=" + callback + " url=" + url);
        HttpUtils.get(url, null, new SimpleResponseCallback<String>() {
            @Override
            public void onSucceed(String s) {
                super.onSucceed(s);
                s = JSON.toJSONString(s);
                String method = "javascript:" + callback + "(" + s + ")";
                Log.d(TAG, "httpAjaxGet: ---> tag callback=" + method);
                callJs(method);
            }
        });
    }

    @JavascriptInterface
    public void httpAjaxPost(String url, String json, String callback) {
        Log.d(TAG, "httpAjaxPost: ---> method=" + callback + " url=" + url);
        Log.d(TAG, "httpAjaxPost: ---> method=" + callback + " json=" + json);
        Map<String, String> params = JSON.parseObject(json, Map.class);
        HttpUtils.post(url, params, new SimpleResponseCallback<String>() {
            @Override
            public void onSucceed(String s) {
                super.onSucceed(s);
                s = JSON.toJSONString(s);
                String method = "javascript:" + callback + "(" + s + ")";
                Log.d(TAG, "httpAjaxPost: ---> tag callback=" + method);
                callJs(method);
            }
        });
    }

    @JavascriptInterface
    public void httpAjaxPostJson(String url, String json, String callback) {
        Log.d(TAG, "httpAjaxPostJson: ---> method=" + callback + " url=" + url);
        Log.d(TAG, "httpAjaxPostJson: ---> method=" + callback + " json=" + json);
        HttpUtils.post(url, json, new SimpleResponseCallback<String>() {
            @Override
            public void onSucceed(String s) {
                super.onSucceed(s);
                s = JSON.toJSONString(s);
                String method = "javascript:" + callback + "(" + s + ")";
                Log.d(TAG, "httpAjaxPostJson: ---> tag callback=" + method);
                callJs(method);
            }
        });
    }

    @JavascriptInterface
    public String getPackageName() {
        return mContext.getPackageName();
    }

    /*@JavascriptInterface
    public boolean isAidlConn() {
        Log.d(TAG, "isAidlConn: --->");
        return IConnManager.getInstance().isConnect();
    }

    @JavascriptInterface
    public void initAidl() {
        Log.d(TAG, "initAidl: --->");
        IConnManager.getInstance().init(mContext.getApplicationContext());
    }

    *//**
     * {
     * "aidlToken": "******",
     * "intentExtra": {
     * "ottdata": "{\"Function\":\"8\"}"
     * }
     * }
     *
     * @param cmd
     * @param params
     * @param callback
     *//*
    @JavascriptInterface
    public void invokeCMD(String cmd, String params, String callback) {
        Log.d(TAG, "invokeCMD: ---> method=" + callback + " cmd=" + cmd + " params=" + params);
        if (mMap != null) {
            mMap.put(cmd, callback);
        }
        IConnManager.getInstance().invokeCMD(cmd, params);
    }*/
}
