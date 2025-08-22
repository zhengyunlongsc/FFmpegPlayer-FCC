package com.z.player;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.util.Log;

import org.litepal.LitePal;
import org.litepal.LitePalApplication;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.jessyan.autosize.AutoSizeConfig;
import me.jessyan.autosize.onAdaptListener;
import me.jessyan.autosize.utils.ScreenUtils;

public class App extends LitePalApplication {
    public final static String TAG = App.class.getName();
    private static App INSTANCE;
    private Map<String, String> mConfigMap;

    public static App getInstance() {
        return INSTANCE;
    }


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: --->> tag app");
        init();
    }

    private void init() {
        INSTANCE = this;
        mConfigMap = new HashMap<>();
        initAutoSize();
        LitePal.initialize(this);
    }

    private void initAutoSize() {
        //屏幕适配监听器
        AutoSizeConfig.getInstance().setOnAdaptListener(new onAdaptListener() {
            @Override
            public void onAdaptBefore(Object target, Activity activity) {
                Log.d(TAG, "initAutoSize: onAdaptBefore tag 0-0 ");
                //使用以下代码, 可以解决横竖屏切换时的屏幕适配问题
                //首先设置最新的屏幕尺寸，ScreenUtils.getScreenSize(activity) 的参数一定要不要传 Application !!!
                AutoSizeConfig.getInstance().setScreenWidth(ScreenUtils.getScreenSize(activity)[0]);
                AutoSizeConfig.getInstance().setScreenHeight(ScreenUtils.getScreenSize(activity)[1]);
                int width = getResources().getInteger(R.integer.design_width);
                int height = getResources().getInteger(R.integer.design_height);
                Log.d(TAG, "initAutoSize: ---> onAdaptBefore tag 0-1 width=" + width + " height=" + height);

                //根据屏幕方向，设置设计尺寸
                if (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    Log.d(TAG, "initAutoSize: ---> onAdaptBefore tag 1-0");
                    //设置横屏设计尺寸
                    AutoSizeConfig.getInstance()
                            .setDesignWidthInDp(width)
                            .setDesignHeightInDp(height);
                } else {
                    Log.d(TAG, "initAutoSize: ---> onAdaptBefore tag 2-0 ");
                    //设置竖屏设计尺寸
                    AutoSizeConfig.getInstance()
                            .setDesignWidthInDp(width)
                            .setDesignHeightInDp(height);
                }
            }

            @Override
            public void onAdaptAfter(Object target, Activity activity) {
                Log.d(TAG, "initAutoSize: ---> onAdaptAfter tag 0-0 ");
            }
        });
    }

    public Map<String, String> getConfigMap() {
        return mConfigMap;
    }

    private byte[] getSign(Context context) {
        PackageManager pm = context.getPackageManager();
        List<PackageInfo> apps = pm.getInstalledPackages(PackageManager.GET_SIGNATURES);
        for (PackageInfo info : apps) {
            String packageName = info.packageName;
            if (packageName.equals(getContext().getPackageName())) {
                return info.signatures[0].toByteArray();
            }
        }
        return null;
    }

    private void getPublicKey(byte[] signature) {
        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(signature));
            String publicKey = cert.getPublicKey().toString();
            Log.d(TAG, "getPublicKey: ---> tag 0-0 key=" + publicKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
