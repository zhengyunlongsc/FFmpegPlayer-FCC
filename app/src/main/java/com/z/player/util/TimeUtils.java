package com.z.player.util;

import android.os.CountDownTimer;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * 作者: zyl on 2020/6/22 11:19
 * 修改人:
 * 修改描述:
 * 修改日期:
 */
public class TimeUtils {
    private static final String TAG = TimeUtils.class.getName();
    private static Map<Integer, Long> mMap;
    private static CountDownTimer mCountDownTimer;

    private TimeUtils() {
    }

    public static boolean isIntervalExecutable(int id, long interval) {
        if (mMap == null) {
            mMap = new HashMap<>();
        }

        Long millis = mMap.get(id);
        if (System.currentTimeMillis() - (millis == null ? 0 : millis) > interval) {
            mMap.put(id, System.currentTimeMillis());
            Log.d(TAG, "---> isIntervalExecutable: ---> true");
            return true;
        }
        Log.d(TAG, "---> isIntervalExecutable: ---> false");
        return false;
    }

    public static CountDownTimer setTimeout(long ms, OnTimeoutListener listener) {
        if (ms >= 0) {
            if (mCountDownTimer != null) {
                mCountDownTimer.cancel();
                mCountDownTimer = null;
            }

            mCountDownTimer = new CountDownTimer(ms, 1000) {
                @Override
                public void onTick(long l) {

                }

                @Override
                public void onFinish() {
                    if (listener != null) {
                        listener.onTimeout();
                    }
                }
            }.start();
        }

        return mCountDownTimer;
    }

    public interface OnTimeoutListener {
        void onTimeout();
    }
}


