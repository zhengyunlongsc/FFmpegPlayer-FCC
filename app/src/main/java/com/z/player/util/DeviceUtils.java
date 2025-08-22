package com.z.player.util;

import android.annotation.SuppressLint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;

/**
 * 描述:
 * 作者: zyl on 2020/4/1 10:56
 * 修改人:
 * 修改描述:
 * 修改日期:
 */
public class DeviceUtils {
    private static final String TAG = DeviceUtils.class.getName();

    private DeviceUtils() {
    }

    @SuppressLint("PrivateApi")
    public static String getSystemProperties(String str) {
        String value = null;
        try {
            String className = "android.os.SystemProperties";
            Class<?> c = Class.forName(className);
            Method get = c.getMethod("get", String.class);
            value = (String) get.invoke(c, str);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return value;
    }

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

    public static String getRomVersion() {
        return getSystemProperties("ro.build.id");
    }

    /**
     * 盒端制造商
     *
     * @return
     */
    public static String getManufacturer() {
        return getSystemProperties("ro.product.manufacturer");
    }

    /**
     * 机顶盒制造商OUI
     *
     * @return
     */
    public static String getManufacturerOUI() {
        return getSystemProperties("ro.product.manufacturer");
    }

    public static String getConnectType() {
        return "LAN";
    }

    /**
     * 机顶盒型号 ID
     *
     * @return
     */
    public static String getModelID() {
        return getSystemProperties("ro.product.model");
    }

    /**
     * 机顶盒型号名称（与ModelID一致）
     *
     * @return
     */
    public static String getModelName() {
        return getSystemProperties("ro.product.model");
    }

    /**
     * 硬件版本号
     *
     * @return
     */
    public static String getHardwareVersion() {
        return getSystemProperties("ro.product.info.hardware");
    }

   /* public String getSWVersion() {
        Log.d(TAG, "getSWVersion: ------>> version=" + getSystemProperties("ro.build.version.incremental"));
        return HardwareUtils.getSystemSettingsAdapter(App.getInstance()).getDisplayVersion();
    }*/

    /**
     * 软件版本号
     *
     * @return
     */
    public static String getSoftwareVersion() {
        return getSystemProperties("ro.build.version.incremental");
    }


    /**
     * 终端序列号
     *
     * @return
     */
    public static String getSerialNumber() {
        return getSystemProperties("ro.serialno");
    }

    public static String getEPGServerPoint() {
        return "8081";//要求写死
    }

    /**
     * 给网络接口分配地址的方法，枚举类型：“IPoE” ，“PPPoE”,”DHCP”
     *
     * @return
     */
    public static String getAddressingType() {
        return getSystemProperties("ro.serialno");
    }

    /**
     * 当前分配给网络接口的IP地址
     *
     * @return
     */
    public static String getIPAddress() {
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
     * MAC地址
     *
     * @return
     */
    public static String getMACAddress() {
        String result = "";
        String Mac = "";
        try {
            result = callCmd("busybox ifconfig", "HWaddr");
            if (result == null) {
                result = getEthMacAddress();
                return result;
            }
            // 对该行数据进行解析
            if (result.length() > 0 && result.contains("HWaddr") == true) {
                Mac = result.substring(result.indexOf("HWaddr") + 6, result.length() - 1);
                if (Mac.length() > 1) {
                    return result = Mac.replaceAll(" ", "");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String getEthMacAddress() {
        String macSerial = null;
        String str = "";
        try {
            Process pp = Runtime.getRuntime().exec("cat /sys/class/net/eth0/address");
            InputStreamReader ir = new InputStreamReader(pp.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);

            for (; null != str; ) {
                str = input.readLine();
                if (str != null) {
                    macSerial = str.trim();// 去空格
                    break;
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return macSerial.toUpperCase();
    }

    public static String callCmd(String s, String s1) {
        boolean flag;
        String s2;
        try {
            BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec(s).getInputStream()));
            do {
                s2 = bufferedreader.readLine();
                if (s2 == null)
                    break; /* Loop/switch isn't completed */
                try {
                    flag = s2.contains(s1);
                } catch (Exception exception) {
                    exception.getStackTrace();
                    return "";
                }
            } while (!flag);
        } catch (IOException exception) {
            exception.printStackTrace();
            return null;
        }

        return s2;
    }

    public static String getFomatTime() {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINESE);
        return format.format(new Date()).replace(" ", "T");
    }
}
