package com.z.player.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.alibaba.fastjson.JSON;
import com.z.player.App;
import com.z.player.bean.KeyValue;
import com.z.player.common.Const;

import org.litepal.LitePal;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppUtils {
    public static final String TAG = "AppUtils";

    @SuppressLint("PrivateApi")
    public static void setSystemProperties(String key, String value) {
        try {
            String className = "android.os.SystemProperties";
            Class<?> c = Class.forName(className);
            Method method = c.getMethod("set", String.class, String.class);
            method.invoke(c, key, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getMainActivity(Context context, String packageName) {
        String mainActivity = null;
        PackageManager manager = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_NEW_TASK);

        @SuppressLint("WrongConstant") List<ResolveInfo> list = manager.queryIntentActivities(intent, PackageManager.GET_ACTIVITIES);
        for (int i = 0; i < list.size(); i++) {
            ResolveInfo info = list.get(i);
            if (info.activityInfo.packageName.equals(packageName)) {
                mainActivity = info.activityInfo.name;
                break;
            }
        }
        Log.d(TAG, "getMainActivityNameByPackageName: ---> activity name=" + mainActivity);
        return mainActivity;

       /* PackageInfo pi = null;
        try {
            pi = getApplicationContext().getPackageManager().getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return;
        }

        Intent resolveIntent = new Intent(Intent.ACTION_MAIN, null);
        resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        resolveIntent.setPackage(packageName);
        PackageManager pManager = getApplicationContext().getPackageManager();
        List<ResolveInfo> apps = pManager.queryIntentActivities(resolveIntent, 0);

        ResolveInfo ri = apps.iterator().next();
        if (ri != null) {
            String startappName = ri.activityInfo.packageName;
            String className = ri.activityInfo.name;

            Log.d(TAG, "openApp: ---> startappName=" + startappName + " className=" + className);

            *//*Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ComponentName cn = new ComponentName(startappName, className);

            intent.setComponent(cn);
            getApplicationContext().startActivity(intent);*//*
        }*/
    }

    /**
     * 版本名
     */
    public static String getVersionName() {
        String version = null;
        PackageInfo pi = getPackageInfo(App.getInstance());
        if (pi != null) {
            version = pi.versionName;
        }
        return version;
    }

    public static int getVersionCode() {
        PackageInfo pi = getPackageInfo(App.getInstance());
        if (pi != null) {
            Log.d(TAG, "getVersionCode: ------>> code=" + pi.versionCode);
            return pi.versionCode;
        }

        return 100;
    }

    public static class TopActivityInfo {
        public String packageName = "";
        public String topActivityName = "";
    }

    public static TopActivityInfo getTopActivityInfo() {
        ActivityManager manager = ((ActivityManager) App.getInstance().getSystemService(Context.ACTIVITY_SERVICE));
        TopActivityInfo info = new TopActivityInfo();
        if (Build.VERSION.SDK_INT >= 21) {
            List<ActivityManager.RunningAppProcessInfo> pis = manager.getRunningAppProcesses();
            ActivityManager.RunningAppProcessInfo topAppProcess = pis.get(0);
            if (topAppProcess != null && topAppProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                info.packageName = topAppProcess.processName;
                info.topActivityName = "";
            }
        } else {
            List<ActivityManager.RunningTaskInfo> localList = manager.getRunningTasks(1);
            ActivityManager.RunningTaskInfo localRunningTaskInfo = (ActivityManager.RunningTaskInfo) localList.get(0);
            info.packageName = localRunningTaskInfo.topActivity.getPackageName();
            info.topActivityName = localRunningTaskInfo.topActivity.getClassName();
        }
        return info;
    }

    public static String getMyTopActivityPackageName() {
        ActivityManager manager = ((ActivityManager) App.getInstance().getSystemService(Context.ACTIVITY_SERVICE));
        TopActivityInfo info = new TopActivityInfo();
        if (Build.VERSION.SDK_INT >= 21) {
            List<ActivityManager.RunningAppProcessInfo> pis = manager.getRunningAppProcesses();
            ActivityManager.RunningAppProcessInfo topAppProcess = pis.get(0);
            if (topAppProcess != null && topAppProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                info.packageName = topAppProcess.processName;
                info.topActivityName = "";
            }
        } else {
            List<ActivityManager.RunningTaskInfo> localList = manager.getRunningTasks(1);
            ActivityManager.RunningTaskInfo localRunningTaskInfo = (ActivityManager.RunningTaskInfo) localList.get(0);
            info.packageName = localRunningTaskInfo.topActivity.getPackageName();
            info.topActivityName = localRunningTaskInfo.topActivity.getClassName();
        }

        return info.packageName;
    }

    public static String getTopActivity() {
        ActivityManager manager = ((ActivityManager) App.getInstance().getSystemService(Context.ACTIVITY_SERVICE));
        String activity = null;
        if (Build.VERSION.SDK_INT >= 21) {
            List<ActivityManager.RunningAppProcessInfo> pis = manager.getRunningAppProcesses();
            ActivityManager.RunningAppProcessInfo topAppProcess = pis.get(0);
            if (topAppProcess != null && topAppProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                activity = topAppProcess.processName;
            }
        } else {
            List<ActivityManager.RunningTaskInfo> localList = manager.getRunningTasks(1);
            ActivityManager.RunningTaskInfo localRunningTaskInfo = (ActivityManager.RunningTaskInfo) localList.get(0);
            activity = localRunningTaskInfo.topActivity.getClassName();
        }
        Log.d(TAG, "getTopActivity: ---> str=" + activity);
        return activity;
    }

    /**
     * 版本名
     */
    public static String getVersionName(String packageName) {
        String version = null;
        try {
            PackageManager pm = App.getInstance().getPackageManager();
            PackageInfo pi = pm.getPackageInfo(packageName, PackageManager.GET_CONFIGURATIONS);
            if (pi != null) {
                version = pi.versionName;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(TAG, "getVersionName: ---> tag packageName=" + packageName + " version=" + version);
        return version;
    }

    /**
     * 版本名
     */
    public static int getVersionCode(String packageName) {
        int version = 0;
        try {
            PackageManager pm = App.getInstance().getPackageManager();
            PackageInfo pi = pm.getPackageInfo(packageName, PackageManager.GET_CONFIGURATIONS);
            if (pi != null) {
                version = pi.versionCode;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(TAG, "getVersionCode: ---> tag packageName=" + packageName + " versionCode=" + version);
        return version;
    }

    private static PackageInfo getPackageInfo(Context context) {
        PackageInfo pi = null;
        try {
            PackageManager pm = context.getPackageManager();
            pi = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_CONFIGURATIONS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pi;
    }

    /**
     * 判断服务是否在运行
     *
     * @param context
     * @return 服务名称为全路径 例如com.ghost.WidgetUpdateService
     */
    public static boolean isRunService(Context context, String serviceName) {
        try {
            ActivityManager manager = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);
            if (manager != null) {
                for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                    String className = service.service.getClassName();
                    if (TextUtils.equals(serviceName, className)) {
                        Log.d(TAG, "isRunService: ---> is run service =" + serviceName);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "isRunService: ---> exception msg=" + e.getMessage());
            e.printStackTrace();
        }

        Log.d(TAG, "isRunService: ---> is run service false");
        return false;
    }

    /**
     * 启动第三方服务
     *
     * @param context
     */
    public static void launchService(Context context, String packageName, String className) {
        if (context == null || TextUtils.isEmpty(packageName) || TextUtils.isEmpty(className)) {
            return;
        }

        context = context.getApplicationContext();
        if (isInstallApp(context, packageName)) {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(packageName, className));
            context.startService(intent);

            Log.d(TAG, "launchService: ---> intent=" + intent);
        } else {
            Log.d(TAG, "launchService: ---> un install app......");
        }
    }

    /**
     * 检测网络是否可用
     *
     * @return 网络是否可用
     */
    public static boolean isNetworkConnected(Context context) {
        if (context == null)
            return false;

        ConnectivityManager cm = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo[] ni = cm.getAllNetworkInfo();
            for (NetworkInfo info : ni) {
                if (info != null && info.getState() == NetworkInfo.State.CONNECTED) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 检测开机动画是否播放完成
     */
    public static boolean isBootAnimRunning() {
        boolean rst = false;
        String status = getSystemProperties("init.svc.bootvideo");
        Log.d(TAG, "isBootAnimRunning: ---> tag 0-0 status=" + status);
        if (TextUtils.equals("running", status) || TextUtils.equals("start", status)) {
            rst = true;
        }

        if (!rst) {
            status = getSystemProperties("init.svc.bootanim");
            Log.d(TAG, "isBootAnimRunning: ---> tag 0-1 status=" + status);
            if (TextUtils.equals("running", status) || TextUtils.equals("start", status)) {
                rst = true;
            }
        }
        return rst;
    }

    public static String getSystemProperties(String str) {
        String value = "";
        try {
            String className = "android.os.SystemProperties";
            @SuppressLint("PrivateApi") Class<?> c = Class.forName(className);
            Method get = c.getMethod("get", String.class);
            value = (String) get.invoke(c, str);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return value;
    }

    //获取设备序列号
    public static String getDeviceSerialNum() {
        return getSystemProperties("ro.serialno");
    }

    public static String getModelName() {
        return DeviceUtils.getModelName();
    }

    public static String getMacAddress() {
        String strMacAddr = "";
        try {
            NetworkInterface networkInterface = NetworkInterface.getByName("eth0");
            if (networkInterface != null) {
                byte[] b = networkInterface.getHardwareAddress();
                StringBuilder buffer = new StringBuilder();
                for (int i = 0; i < b.length; i++) {
                    if (i != 0) {
                        buffer.append(':');
                    }
                    System.out.println("b:" + (b[i] & 0xFF));
                    String str = Integer.toHexString(b[i] & 0xFF);
                    buffer.append(str.length() == 1 ? 0 + str : str);
                }
                strMacAddr = buffer.toString().toUpperCase();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(TAG, "getEthMacAddress: ---> eth0 mac=" + strMacAddr);

        return strMacAddr;
    }

    public static String getLocalIp() {
        String ip = null;
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        ip = inetAddress.getHostAddress();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ip;
    }

    /**
     * 判断某个Activity 界面是否在前台
     *
     * @param context
     * @param className 某个界面名称 getName()
     * @return
     */
    public static boolean isForeground(Context context, String className) {
        if (context == null || TextUtils.isEmpty(className)) {
            return false;
        }
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(1);
        Log.d(TAG, "isForeground: ---> running task=" + JSON.toJSONString(list));
        if (list != null && list.size() > 0) {
            ComponentName cpn = list.get(0).topActivity;
            Log.d(TAG, "isForeground: ---> component name=" + JSON.toJSONString(cpn));
            return className.equals(cpn.getClassName());
        }
        return false;
    }

    /**
     * 判断程序是否在前台
     *
     * @param context
     * @param packageName
     * @return
     */
    public static boolean isForeground2(Context context, String packageName) {
        if (context == null || TextUtils.isEmpty(packageName)) {
            return false;
        }
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(1);
        if (list != null && list.size() > 0) {
            ComponentName cpn = list.get(0).topActivity;
            if (TextUtils.equals(cpn.getPackageName(), packageName)) {
                return true;
            }
            Log.d(TAG, "isForeground2: packageName-" + packageName + " topActivity " + cpn.getClassName());
        }
        return false;
    }

    /**
     * 检查包是否存在
     *
     * @param packageName
     * @return
     */
    public static boolean isInstallApp(Context context, String packageName) {
        if (context == null || TextUtils.isEmpty(packageName)) {
            Log.d(TAG, "isInstallApp: ---> return....");
            return false;
        }

        PackageInfo packageInfo = null;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(packageName, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        boolean isInstall = packageInfo != null;
        Log.d(TAG, "isInstallApp: ---> packname=" + packageName + " is install=" + isInstall);
        return isInstall;
    }

    /**
     * 启动第三方apk
     * 如果已经启动apk，则直接将apk从后台调到前台运行（类似home键之后再点击apk图标启动），如果未启动apk，则重新启动
     */
    public static void launchAPK2(Context context, String packageName) {
        if (context == null || TextUtils.isEmpty(packageName)) {
            Log.d(TAG, "launchAPK2: ---> is null...");
            return;
        }
        Intent intent = getAppOpenIntentByPackageName(context, packageName);
        Log.d(TAG, "launchAPK2: ---> intent=" + JSON.toJSONString(intent));
        if (intent != null) {
            context.startActivity(intent);
        } else {//没有启动app,则启动
            launchAPK1(context, packageName);
        }
    }


    /**
     * 启动第三方apk
     * 直接打开  每次都会启动到启动界面，每次都会干掉之前的，从新启动
     */
    public static void launchAPK1(Context context, String packageName) {
        PackageManager packageManager = context.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(packageName);
        Log.d(TAG, "launchAPK1: ---> intent=" + JSON.toJSONString(intent));
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    public static Intent getAppOpenIntentByPackageName(Context context, String packageName) {
        String mainAct = null;
        PackageManager pkgMag = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_NEW_TASK);

        @SuppressLint("WrongConstant") List<ResolveInfo> list = pkgMag.queryIntentActivities(intent, PackageManager.GET_ACTIVITIES);
        for (int i = 0; i < list.size(); i++) {
            ResolveInfo info = list.get(i);
            if (info.activityInfo.packageName.equals(packageName)) {
                mainAct = info.activityInfo.name;
                intent.setComponent(new ComponentName(packageName, mainAct));
                Log.d(TAG, "getAppOpenIntentByPackageName: ------ info.activityInfo.name=" + mainAct);
                break;
            }
        }

        if (TextUtils.isEmpty(mainAct)) {
            Log.d(TAG, "getAppOpenIntentByPackageName: ---> mainAct=" + mainAct);
            return null;
        }
        return intent;
    }

    private long mKeyRemappingSendFakeKeyDownTime;

    public static void sendKeyCode(final int keyCode) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Instrumentation inst = new Instrumentation();
                    inst.sendKeyDownUpSync(keyCode);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // 执行命令
    public static String doExec(String cmd) {
        String s = "\n";
        try {
            String[] cmdline = {"sh", "-c", cmd};
            Process p = Runtime.getRuntime().exec(cmdline);
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = null;
            //PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(p.getOutputStream())), true);
            //out.println(cmd);
            while ((line = in.readLine()) != null) {
                s += line + "\n";
            }
            in.close();
//          out.close();
            Log.d(TAG, "do_exec: ---> s=" + s);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // text.setText(s);
        return s;
    }

    /*
     * m命令可以通过adb在shell中执行，同样，我们可以通过代码来执行
     */
    public static String execCommand(String... command) {
        Process process = null;
        InputStream errIs = null;
        InputStream inIs = null;
        String result = "";

        try {
            process = new ProcessBuilder().command(command).start();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            int read = -1;
            errIs = process.getErrorStream();
            while ((read = errIs.read()) != -1) {
                baos.write(read);
            }

            inIs = process.getInputStream();
            while ((read = inIs.read()) != -1) {
                baos.write(read);
            }

            result = new String(baos.toByteArray());
            baos.flush();
            process.waitFor();
        } catch (Exception e) {
            Log.d(TAG, "execCommand: ---> e=" + e.getMessage());
            result = e.getMessage();
        } finally {
            if (inIs != null) {
                try {
                    inIs.close();
                } catch (IOException e) {
                    Log.d(TAG, "execCommand: ---> e2=" + e.getMessage());
                }
            }

            if (errIs != null) {
                try {
                    errIs.close();
                } catch (IOException e) {
                    Log.d(TAG, "execCommand: ---> e3=" + e.getMessage());
                }
            }

            if (process != null) {
                process.destroy();
            }
        }
        return result;
    }

    public static String getAndroidVersionName() {
        return AppUtils.getSystemProperties("ro.build.version.release");
    }

    public static void getSignature() {
        PackageManager manager = App.getInstance().getPackageManager();
        StringBuilder builder = new StringBuilder();
        String pkgname = App.getInstance().getPackageName();
        boolean isEmpty = pkgname.isEmpty();
        if (isEmpty) {
            Log.d(TAG, "getSignature: ---> is null");
        } else {
            try {
                PackageInfo packageInfo = manager.getPackageInfo(pkgname, PackageManager.GET_SIGNATURES);
                Signature[] signatures = packageInfo.signatures;
                Signature sign = signatures[0];
                byte[] signByte = sign.toByteArray();
                Log.d(TAG, "getSignature: ---> signByte=" + bytesToHexString(signByte));
                Log.d(TAG, "getSignature ---> hash=" + bytesToHexString(generateSHA1(signByte)));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public static byte[] generateSHA1(byte[] data) {
        try {
            // 使用getInstance("算法")来获得消息摘要,这里使用SHA-1的160位算法
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            // 开始使用算法
            messageDigest.update(data);
            // 输出算法运算结果
            byte[] hashValue = messageDigest.digest(); // 20位字节
            return hashValue;
        } catch (Exception e) {
            Log.e("generateSHA1", e.getMessage());
        }
        return null;
    }

    public static String bytesToHexString(byte[] bytes) {
        if (bytes == null) {
            return "";
        }
        StringBuilder buff = new StringBuilder();
        for (byte aByte : bytes) {
            if ((aByte & 0xff) < 16) {
                buff.append('0');
            }
            buff.append(Integer.toHexString(aByte & 0xff));
        }
        return buff.toString();
    }

    public static void killProcess(Context context, String pkname) {
        try {
            int pid = getProcessId(context, pkname);
            Log.d(TAG, "killProcess: ---> tag 0-0 pid=" + pid);
            android.os.Process.killProcess(pid);
            Log.d(TAG, "killProcess: ---> tag 0-1 name=" + pkname);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "killProcess: ---> tag 0-e=" + e.getMessage());
        }
    }

    public static int getProcessId(Context context, String pkname) {
        if (!TextUtils.isEmpty(pkname) && context != null) {
            Log.d(TAG, "getProcessId: ---> tag pkname=" + pkname);
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> processInfos = am.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo info : processInfos) {
                if (TextUtils.equals(pkname, info.processName)) {
                    Log.d(TAG, "getProcessId: ---> tag 0-1 pid=" + info.pid);
                    return info.pid;
                }
            }
        }
        return -1;
    }

    public static boolean checkAndRequestPermissions(Activity activity, String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_WIFI_STATE}, requestCode);
            return false;
        }
        return true;
    }

    /**
     * 获取MAC地址
     */
    public static String getMacAddress1() {
        try {
            // 把当前机器上访问网络的接口存入 List集合中
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!"wlan0".equalsIgnoreCase(nif.getName())) {
                    continue;
                }
                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null || macBytes.length == 0) {
                    continue;
                }
                StringBuilder result = new StringBuilder();
                for (byte b : macBytes) {
                    //每隔两个字符加一个:
                    result.append(String.format("%02X:", b));
                }
                if (result.length() > 0) {
                    //删除最后一个:
                    result.deleteCharAt(result.length() - 1);
                }
                return result.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Unknown";
    }

    public static String getMacAddress2() {
        String address = "";
        try {
            // 把当前机器上访问网络的接口存入 Enumeration集合中
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            Log.d("TEST_BUG", " interfaceName = " + interfaces);
            while (interfaces.hasMoreElements()) {
                NetworkInterface netWork = interfaces.nextElement();
                // 如果存在硬件地址并可以使用给定的当前权限访问，则返回该硬件地址（通常是 MAC）。
                byte[] by = netWork.getHardwareAddress();
                if (by == null || by.length == 0) {
                    continue;
                }
                StringBuilder builder = new StringBuilder();
                for (byte b : by) {
                    builder.append(String.format("%02X:", b));
                }
                if (builder.length() > 0) {
                    builder.deleteCharAt(builder.length() - 1);
                }
                String mac = builder.toString();
                Log.d("TEST_BUG", " interfaceName =" + netWork.getName() + ", mac=" + mac);
                // 从路由器上在线设备的MAC地址列表，可以印证设备Wifi的 name 是 wlan0
                if (netWork.getName().equals("wlan0")) {
                    address = mac;
                    Log.d("TEST_BUG", " interfaceName =" + netWork.getName() + ", address=" + address);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return address;
    }

    public static String getMacAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            return wifiManager.getConnectionInfo().getMacAddress();
        }
        return null;
    }

    public static String getDeviceModel() {
        return getSystemProperties("ro.product.model");
    }

    public static String getRomVersion() {
        return getSystemProperties("ro.build.version.incremental");
    }

    public static void setKeyValue(String key, String value) {
        Log.d(TAG, "setKeyValue: ---> tag0 key=" + key);
        KeyValue kv = new KeyValue();
        kv.key = key;
        kv.value = value;
        boolean is = kv.saveOrUpdate("key=?", key);
        Log.d(TAG, "setKeyValue: ---> tag0 is=" + is);
    }

    public static String getKeyValue(String key) {
        Log.d(TAG, "getKeyValue: ---> key=" + key);
        KeyValue keyValue = LitePal.where("key=?", key).findFirst(KeyValue.class);
        Log.d(TAG, "getKeyValue: ---> tag0 key=" + key + " value=" + keyValue);
        if (keyValue != null) {
            return keyValue.value;
        }
        return "";
    }

    public static boolean isFilterModel(String model) {
        for (int i = 0; i < Const.MODELS.length; i++) {
            String item = Const.MODELS[i];
            if (TextUtils.equals(model, item)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 删除重复参数
     *
     * @param url
     * @return
     */
    public static String parseUrlParams(String url) {
        StringBuilder path = new StringBuilder(url);
        if (!TextUtils.isEmpty(url)) {
            Map<String, String> map = new HashMap<>();
            if (url.contains("?")) {
                String[] arr = url.split("\\?");
                if (arr != null && arr.length >= 2) {
                    path = new StringBuilder(arr[0]);
                    for (int i = 1; i < arr.length; i++) {
                        String s = arr[i];
                        if (!TextUtils.isEmpty(s) && s.contains("&")) {
                            String[] arr2 = s.split("&");
                            for (int j = 0; j < arr2.length; j++) {
                                String s2 = arr2[j];
                                if (!TextUtils.isEmpty(s2) && s2.contains("=")) {
                                    String[] arr3 = s2.split("=");
                                    if (arr3 != null && arr3.length == 2) {
                                        map.put(arr3[0], arr3[1]);
                                    }
                                }
                            }
                        }
                    }

                    int index = 0;
                    for (Map.Entry<String, String> item : map.entrySet()) {
                        String key = item.getKey();
                        String val = item.getValue();

                        if (index == 0) {
                            path.append("?").append(key).append("=").append(val);
                        } else {
                            path.append("&").append(key).append("=").append(val);
                        }

                        index++;
                    }
                }
            }
        }

        return path.toString();
    }

    public static String removeParamsFromUrl(String url, String... paramsToRemove) {
        if (TextUtils.isEmpty(url)) {
            return url;
        }

        int questionMarkIndex = url.indexOf('?');
        if (questionMarkIndex == -1) {
            return url;
        } else {
            String queryString = url.substring(questionMarkIndex + 1);
            String baseUrl = url.substring(0, questionMarkIndex);

            String[] pairs = queryString.split("&");
            StringBuilder cleanQuery = new StringBuilder();

            for (String pair : pairs) {
                boolean shouldRemove = false;
                for (String param : paramsToRemove) {
                    if (pair.startsWith(param + "=")) {
                        shouldRemove = true;
                        break;
                    }
                }
                if (!shouldRemove) {
                    if (cleanQuery.length() > 0) cleanQuery.append("&");
                    cleanQuery.append(pair);
                }
            }

            if (cleanQuery.length() > 0) {
                baseUrl += "?" + cleanQuery.toString();
            }

            return baseUrl;
        }
    }


}
