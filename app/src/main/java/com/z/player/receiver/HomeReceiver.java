package com.z.player.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


/**
 * 作者: zyl on 2019/5/7 22:27
 * 描述: ${DESCRIPTION}
 * 修改人:
 * 修改描述:
 * 修改日期:
 */
public class HomeReceiver extends BroadcastReceiver {
    private final static String TAG = HomeReceiver.class.getName();

    public HomeReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: ---> tag 0-0");
    }
}
