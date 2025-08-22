package com.z.player.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


/**
 * 接受开机启动的消息
 *
 * @author Administrator
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = BootReceiver.class.getName();

    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.d(TAG, "onReceive: ---> 开机启动...");
        }
    }
}
