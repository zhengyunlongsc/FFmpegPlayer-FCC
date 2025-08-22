package com.z.player.http;

import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.zhy.http.okhttp.callback.Callback;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import okhttp3.Call;
import okhttp3.Response;

/**
 * 作者: zyl on 2019/4/28 19:09
 * 描述: ${DESCRIPTION}
 * 修改人:
 * 修改描述:
 * 修改日期:
 */
public abstract class AjaxCallback<T> extends Callback<T> {
    private static final String TAG = AjaxCallback.class.getName();
    public Type[] mTypes;

    public AjaxCallback() {
        try {
            Type type = getClass().getGenericSuperclass();
            ParameterizedType pt = ((ParameterizedType) type);
            if (pt != null) {
                mTypes = pt.getActualTypeArguments();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public T parseNetworkResponse(Response response, int id) throws Exception {
        if (response.isSuccessful()) {
            if (response.body() != null) {
                String str = response.body().string();
                Log.d(TAG, "parseNetworkResponse: ---> url=" + response.request().url().toString());
                Log.d(TAG, "parseNetworkResponse: ---> str=" + str);
                try {
                    if (!TextUtils.isEmpty(str)) {
                        if (str.contains("PUT_OK")) {
                            return (T) str;
                        }

                        if (mTypes != null && mTypes.length > 0) {
                            T t1 = JSON.parseObject(str, mTypes[0]);
                            Log.d(TAG, "parseNetworkResponse: ---> t1");
                            return t1;
                        }

                        return (T) str;
                    }
                } catch (Exception e) {
                    Log.d(TAG, "response: ---> url=" + response.request().url().toString());
                    Log.d(TAG, "response: ---> err=" + e.getMessage());
                }
            }
        }
        return null;
    }

    @Override
    public void onError(Call call, Exception e, int id) {
        Log.d(TAG, "onError: ---> url=" + call.request().url().toString());
        Log.d(TAG, "onError: ---> exception=" + e.getMessage());
    }
}

