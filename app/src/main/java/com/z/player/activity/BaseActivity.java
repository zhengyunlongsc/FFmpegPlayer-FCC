package com.z.player.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.alibaba.fastjson.JSON;
import com.gyf.immersionbar.ImmersionBar;
import com.z.player.R;
import com.z.player.util.AppUtils;

/**
 * 作者: zyl on 2019/12/24 10:36
 * 修改人:
 * 修改描述:
 * 修改日期:
 */
public abstract class BaseActivity extends AppCompatActivity implements View.OnClickListener {
    public String TAG;
    public static final int PERMISSION_ACCESS_WIFI_STATE = 0x1000;
    public static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 0x1001;
    private FrameLayout layout_content;
    private View mRootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TAG = getClass().getName();
        Log.e("-------------->>>init", TAG);
        setContentView(R.layout.activity_base);
        initImmersionBar();
        initTitleBarView();
        initContentView();

        boolean b = AppUtils.checkAndRequestPermissions(this,
                Manifest.permission.ACCESS_WIFI_STATE, PERMISSION_ACCESS_WIFI_STATE);
        Log.d(TAG, "onCreate: ---> tag b=" + b + " getMacAddress " + AppUtils.getMacAddress());
        Log.d(TAG, "onCreate: ---> tag b=" + b + " getMacAddress1 " + AppUtils.getMacAddress1());
        Log.d(TAG, "onCreate: ---> tag b=" + b + " getMacAddress2 " + AppUtils.getMacAddress2());
        if (b) {
            init();
            initView();
            initListener();
            initData();
        }

        /*Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        getWindow().getDecorView().setLayerType(View.LAYER_TYPE_HARDWARE, paint);*/
    }

    public void init() {
        mRootView = findViewById(android.R.id.content);
    }

    private void initTitleBarView() {
    }

    private void initContentView() {
        layout_content = findViewById(R.id.layout_content);
        layout_content.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                Log.d(TAG, "onViewAttachedToWindow: ------>>");
                findNestedScrollView(layout_content);
            }

            @Override
            public void onViewDetachedFromWindow(View v) {

            }
        });

        LayoutInflater.from(this).inflate(initLayout(), layout_content);
    }

    public View getRootView() {
        return mRootView;
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    private void initImmersionBar() {
        ImmersionBar.with(this)
                .fullScreen(true)
                .statusBarView(getStatusBarId())//设置后将全屏,把状态栏高度设置到view上
                .keyboardEnable(false)//输入框问题
                .init();
    }

    public int getStatusBarId() {
        return R.id.v_bar;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: ------>>");
    }


    public void findNestedScrollView(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                //Log.d(TAG, "findNestedScrollView: ------>> view=" + child);
                if (child instanceof ViewGroup) {
                    findNestedScrollView(child);
                }
            }
        }
    }

    /*@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.d("BaseActivity", "onKeyDown: keyCode=" + keyCode);
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            this.finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }*/

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public boolean checkPermission(String permission, int request_code) {
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            /*if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                //Toast.makeText(this, "权限被拒绝!", Toast.LENGTH_SHORT).show();
            }*/
            ActivityCompat.requestPermissions(this, new String[]{permission}, request_code);
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissions.length != 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "权限被拒绝!", Toast.LENGTH_SHORT).show();
            onRequestPermissionsResult(false, requestCode, permissions);
        } else {
            onRequestPermissionsResult(true, requestCode, permissions);
        }
    }

    public void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    public void showToast(int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
    }

    public void onRequestPermissionsResult(boolean isGranted, int requestCode, String[] permissions) {
        if (!isGranted) {
            showToast("权限拒绝:" + JSON.toJSONString(permissions));
        }
    }

    public void appExit(){
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
    }

    public abstract int initLayout();

    public abstract void initView();

    public abstract void initListener();

    public abstract void initData();

    public View getContentLayout() {
        return layout_content;
    }

    public Context getContext() {
        return this;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View view = getCurrentFocus();
            if (isHideInput(view, ev)) {
                hideSoftInput(view.getWindowToken());
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 判定是否需要隐藏
     *
     * @param v
     * @param ev
     * @return
     */
    private boolean isHideInput(View v, MotionEvent ev) {
        if (v instanceof EditText) {
            int[] l = {0, 0};
            v.getLocationInWindow(l);
            int left = l[0], top = l[1], bottom = top + v.getHeight(), right = left
                    + v.getWidth();
            if (ev.getX() > left && ev.getX() < right && ev.getY() > top
                    && ev.getY() < bottom) {
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    /**
     * 隐藏软键盘
     *
     * @param token
     */
    private void hideSoftInput(IBinder token) {
        if (token != null) {
            InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    public void canDrawOverlays(boolean can) {
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    public int getDimensionPixelSize(int r) {
        float dim = getResources().getDimensionPixelSize(r);
        //Log.d(TAG, "getDimension: ------>>  dim=" + dim);
        return (int) dim;
    }
}
