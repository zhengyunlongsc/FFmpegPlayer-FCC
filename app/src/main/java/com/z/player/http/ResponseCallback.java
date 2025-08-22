package com.z.player.http;

import android.text.TextUtils;
import android.util.Log;

import okhttp3.Call;

public abstract class ResponseCallback<T> extends AjaxCallback<T> {
    private static final String TAG = ResponseCallback.class.getName();

    @Override
    public void onResponse(T t, int id) {
        Log.d(TAG, "onResponse: ---> id=" + id);
        try {
            if (t != null) {
                if (t instanceof HttpResponse) {
                    HttpResponse hr = (HttpResponse) t;
                    if (HttpUtils.isSuccess(hr)) {
                        onSucceed(t);
                    } else {
                        String msg = "";
                        HttpResponse.MetaBean metaBean = hr.meta;
                        if (metaBean != null) {
                            msg = metaBean.message;
                        } else {
                            msg = hr.message;
                            if (TextUtils.isEmpty(msg)) {
                                msg = hr.msg;
                            }
                        }
                        onFailed(hr.code, msg);
                    }
                } else {
                    Log.d(TAG, "onResponse: ---> tag1");
                    onSucceed(t);
                }
            } else {
                onFailed(-1, null);//有响应，但HttpResponse null
            }
        } catch (Exception e) {
            e.printStackTrace();
            onFailed(-2, null);//-2 异常
        }
    }

    @Override
    public void onError(Call call, Exception e, int id) {
        super.onError(call, e, id);
        onError(e, id);
    }

    public abstract void onSucceed(T t);

    public abstract void onFailed(int code, String msg);

    public abstract void onError(Exception e, int id);


}
