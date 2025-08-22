package com.z.player.http;

import android.util.Log;

/**
 * 作者: zyl on 2019/4/28 19:14
 * 描述: ${DESCRIPTION}
 * 修改人:
 * 修改描述:
 * 修改日期:
 */
public class SimpleResponseCallback<T> extends ResponseCallback<T> {
    private static final String TAG = SimpleResponseCallback.class.getName();

    @Override
    public void onSucceed(T t) {
    }

    @Override
    public void onFailed(int code, String msg) {
        Log.d(TAG, "onFailed: ---> code=" + code + " msg=" + msg);
    }

    @Override
    public void onError(Exception e, int id) {
        Log.d(TAG, "onError: ---> exception=" + e.getMessage());
    }
}
