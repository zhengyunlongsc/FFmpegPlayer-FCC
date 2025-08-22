package com.z.player.js;

import android.util.Log;
import android.webkit.JavascriptInterface;

public class LogUtils {
    private static String TAG = LogUtils.class.getName() + ":---> LogFromWebView";

    @JavascriptInterface
    public void e(String msg) {
        Log.e(TAG, msg);
    }

    @JavascriptInterface
    public void w(String msg) {
        Log.w(TAG, msg);
    }

    @JavascriptInterface
    public void d(String msg) {
        Log.d(TAG, msg);
    }

}
