package com.z.player.http;


import android.content.Context;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.builder.PostFormBuilder;
import com.zhy.http.okhttp.callback.FileCallBack;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;

/**
 * 作者: zyl on 2020/4/28 15:18
 * 修改人:
 * 修改描述:
 * 修改日期:
 */
public class HttpUtils {
    private final static String TAG = HttpUtils.class.getName();
    private static String mToken;

    private HttpUtils() {
    }

    /*private static class LazyUtils {
        private static final HttpUtils INSTANCE = new HttpUtils();
    }
    public static HttpUtils getInstance() {
        return LazyUtils.INSTANCE;
    }*/

    public static void setToken(String token) {
        mToken = token;
    }


    public static void get(String url, Map<String, String> params
            , AjaxCallback callback) {
        try {
            Log.d(TAG, "get: ---> url=" + url);
            Log.d(TAG, "get: ---> params=" + JSON.toJSONString(params));
            Log.d(TAG, "get: ---> token=" + mToken);

            OkHttpUtils.get().addHeader("token", mToken)
                    .url(url)
                    .params(setCommonParams(params))
                    .build().execute(callback);
        } catch (Exception e) {
            Log.d(TAG, "get: ---> msg=" + e.getMessage());
        }
    }

    public static void post(String url, Map<String, String> params, AjaxCallback callback) {
        try {
            if (params != null) {
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    String value = entry.getValue();
                    if (value == null) {
                        entry.setValue("");
                    }
                }
            }
            Log.d(TAG, "post: ---> url=" + url);
            Log.d(TAG, "post: ---> params=" + JSON.toJSONString(params));
            PostFormBuilder builder = OkHttpUtils.post()
                    .url(url);
            builder.params(setCommonParams(params));
            builder.build().execute(callback);
        } catch (Exception e) {
            Log.d(TAG, "post: ---> msg=" + e.getMessage());
        }
    }


    public static void post(String url, String josnParam, AjaxCallback callback) {
        try {
            Log.d(TAG, "post: ---> url=" + url);
            Log.d(TAG, "post: ---> params=" + josnParam);
            OkHttpUtils.postString()
                    .url(url).content(josnParam)
                    .mediaType(MediaType.parse("application/json; charset=utf-8"))
                    .build().execute(callback);
        } catch (Exception e) {
            Log.d(TAG, "post: ---> msg=" + e.getMessage());
        }
    }

    /**
     * 上传单个文件
     *
     * @param context
     * @param url
     * @param file
     * @param callback
     */
    public static void post(Context context, String url, File file, AjaxCallback callback) {
        try {
            Log.d(TAG, "post: ---> url=" + url);
            Log.d(TAG, "post: ---> file=" + JSON.toJSONString(file));
            if (file != null && file.exists()) {
                PostFormBuilder builder = OkHttpUtils.post()
                        .url(url);
                builder.params(setCommonParams(null));
                builder.addFile("file", file.getName(), file);
                builder.build().execute(callback);
            }
        } catch (Exception e) {
            Log.d(TAG, "post: ---> msg=" + e.getMessage());
        }
    }

    public static boolean isSuccess(HttpResponse response) {
        if (response != null) {
            Log.d(TAG, "isSuccess: ---> response=" + JSON.toJSONString(response));
            if (response.meta != null) {
                Log.d(TAG, "isSuccess: ---> response.meta.success=" + response.meta.success);
                return response.meta.success;
            } else {
                boolean b = response.code == 200 || response.code == 0;
                Log.d(TAG, "isSuccess: ---> isSuccess=" + b);
                return b;
            }
        }
        return false;
    }

    /**
     * 设置通用参数
     */
    public static Map<String, String> setCommonParams(Map<String, String> params) {
        if (params == null) {
            params = new HashMap<>();
        }

        Log.d(TAG, "setCommonParams: ---> params=" + JSON.toJSONString(params));

        /*String token = LoginHelper.getInstance().getToken(context);
        params.put("appcode", "5RU8H3D6kmnqc8X8FUM8wt5R2NeeWUwl");
        params.put("accessToken", TextUtils.isEmpty(token) ? "" : token);*/
        return params;
    }

    public static void downFile(String url, FileCallBack fileCallBack) {
        Log.d(TAG, "downFile: ---> url=" + url);
        OkHttpUtils.get()
                .url(url).build().execute(fileCallBack);
    }
}

