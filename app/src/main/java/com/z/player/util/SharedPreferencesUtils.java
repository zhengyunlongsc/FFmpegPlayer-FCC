package com.z.player.util;

import com.z.player.App;

import java.util.Map;

/**
 * 注释：
 * 作者：liql
 * 时间：2017/10/26 15:51
 */

public class SharedPreferencesUtils {
    /**
     * 保存在手机里面的文件名
     */
    private static final String FILE_NAME = "share_date";


    /**
     * 保存数据的方法，我们需要拿到保存数据的具体类型，然后根据类型调用不同的保存方法
     *
     * @param key
     */
    public static void setParam(String key, String value) {
        Map<String, String> map = App.getInstance().getConfigMap();
        if (map != null) {
            map.put(key, value);
        }
    }


    /**
     * 得到保存数据的方法，我们根据默认值得到保存的数据的具体类型，然后调用相对于的方法获取值
     *
     * @param key
     * @return
     */
    public static String getParam(String key) {
        Map<String, String> map = App.getInstance().getConfigMap();
        if (map != null) {
            return map.get(key);
        }

        return null;
    }
}
