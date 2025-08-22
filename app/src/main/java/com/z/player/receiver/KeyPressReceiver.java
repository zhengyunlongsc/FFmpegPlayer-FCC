package com.z.player.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 作者: zyl on 2021/8/13 17:07
 * 修改人:
 * 修改描述:
 * 修改日期:
 */
public class KeyPressReceiver extends BroadcastReceiver {
    private static final String TAG = KeyPressReceiver.class.getName();

    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.d(TAG, "---> onReceive: ---> action=" + intent.getAction());
    }
}
