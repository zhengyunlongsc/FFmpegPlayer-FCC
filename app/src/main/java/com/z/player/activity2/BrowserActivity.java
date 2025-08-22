package com.z.player.activity2;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.z.player.R;
import com.z.player.common.Const;
import com.z.player.js.AndroidJs;
import com.z.player.js.Authentication;
import com.z.player.util.AppUtils;
import com.z.player.widget.FFPlayerView;

public class BrowserActivity extends BaseActivity {
    private static final String TAG = BrowserActivity.class.getName();
    private WebView web_view;
    private FFPlayerView player_view;
    private AndroidJs mAndroidJs;
    private Authentication mAuthentication;
    private Handler mHandler;
    private int mKeyCodeValue;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent: ---> tag ");
        setIntent(intent);
        loadData();
    }

    @Override
    public int initLayout() {
        return R.layout.activity_browser;
    }

    @Override
    public void init() {
        super.init();
        mHandler = new Handler(Looper.getMainLooper());
        mAndroidJs = new AndroidJs(this);
        mAuthentication = new Authentication();
    }

    @Override
    public void initView() {
        super.initView();
        initWebView();
    }

    @Override
    public void initListener() {
        super.initListener();
    }

    @Override
    public void initData() {
        super.initData();
        loadData();
    }

    @Override
    public AndroidJs getAndroidJs() {
        return mAndroidJs;
    }

    private void loadData() {
        String url = getIntent().getStringExtra(Const.KEY_DATA);
        if (TextUtils.isEmpty(url)) {
            url = Const.DEFAULT_HOME_ADDRESS;
        }

        loadUrl(url);
    }

    @SuppressLint({"JavascriptInterface", "SetJavaScriptEnabled"})
    private void initWebView() {
        player_view = findViewById(R.id.player_view);
        web_view = findViewById(R.id.web_view);

        web_view.addJavascriptInterface(player_view, "MPlayer");
        web_view.addJavascriptInterface(mAndroidJs, "AndroidJs");
        web_view.addJavascriptInterface(mAuthentication, "Authentication");
        web_view.setBackgroundColor(Color.TRANSPARENT);
        web_view.setDrawingCacheEnabled(true);
        web_view.setAlwaysDrawnWithCacheEnabled(true);
        web_view.setVisibility(View.VISIBLE);
        web_view.bringToFront();
        web_view.setFocusable(true);

        web_view.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "onPageFinished: ---> tag 0-0 url=" + url);
                view.requestFocus();
            }
        });

        web_view.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                Log.d(TAG, "onProgressChanged: ---> tag0 newProgress=" + newProgress);

                String url = view.getUrl();
                Log.d(TAG, "onProgressChanged: ---> tag0-0 url=" + url);
                /*if (newProgress > 0) {
                    view.setVisibility(View.VISIBLE);
                }*/
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                Log.d(TAG, "onReceivedTitle: ---> tag0 title=" + title);
                if (title.contains("404") || title.contains("500") || title.contains("error")
                        || title.contains("Error") || title.contains("无法访问")
                        || title.contains("找不到网页") || title.contains("网页无法打开")) {
                    loadBackupHtml();
                }
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                Log.d(TAG, "onPermissionRequest: ---> tag 0-0 request=" + request.toString());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    request.grant(request.getResources());
                    request.getOrigin();
                }
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                Log.d(TAG, "onJsAlert: ---> tag 0-0 url=" + url);
                Log.d(TAG, "onJsAlert: ---> tag 0-0 msg=" + message);
                result.confirm();
                return true;
            }
        });

        WebSettings settings = web_view.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setSupportMultipleWindows(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setDatabaseEnabled(false);
        settings.setAppCacheEnabled(false);
        settings.setDefaultTextEncodingName("UTF-8");


        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setAllowFileAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
    }

    private void loadBackupHtml() {
        loadUrl("about:blank");
        loadUrl(Const.DEFAULT_HOME_ADDRESS);
    }

    public void loadUrl(String url) {
        Log.d(TAG, "loadUrl: ---> url=" + url);
        if (TextUtils.isEmpty(url)) {
            url = Const.DEFAULT_HOME_ADDRESS;
        }
        Log.d(TAG, "loadUrl: ---> tag0 load url=" + url);

        web_view.loadUrl(url);
    }

    @Override
    public void callJs(String js) {
        super.callJs(js);
        Log.d(TAG, "callJs: ---> tag js=" + js);
        if (web_view != null) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    web_view.evaluateJavascript(js, null);
                }
            }, 200);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        callJs("javascript:onPause()");
    }

    @Override
    protected void onResume() {
        super.onResume();
        callJs("javascript:onResume()");
    }


    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: ---> tag ");
        if (player_view != null) {
            player_view.destroy();
        }

        if (web_view != null) {
            web_view.stopLoading();
            web_view.removeJavascriptInterface("MPlayer");
            web_view.removeJavascriptInterface("AndroidJs");
            web_view.removeJavascriptInterface("Authentication");
            web_view.setWebViewClient(null);
            web_view.setWebChromeClient(null);
            web_view.clearHistory();
            web_view.clearCache(true);
            web_view.loadUrl("about:blank");
            ((ViewGroup) web_view.getParent()).removeView(web_view);
            web_view.destroy();
            web_view = null;
        }

        super.onDestroy();
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyDown: ---> keyCode=" + keyCode + " event=" + event);
        switch (keyCode) {
            case 13:
                mKeyCodeValue += keyCode;
                if (mKeyCodeValue == 104) {//66666666
                    mKeyCodeValue = 0;
                    String text = AppUtils.getVersionName()
                            + "-AIP" + Build.VERSION.RELEASE
                            + "-INT" + Build.VERSION.SDK_INT;
                    showToast(text);
                }

                resetCodeValue();
                break;
            case KeyEvent.KEYCODE_BACK:
                callJs("javascript:backEvent()");
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    public void resetCodeValue() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mKeyCodeValue = 0;
                }
            }, 3000);
        }
    }
}
