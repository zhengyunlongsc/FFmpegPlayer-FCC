package com.z.player.help;

import android.util.Log;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class MulHelper {
    private static final String TAG = MulHelper.class.getName();
    private boolean isMulEnable;
    private int mStreamMode;

    public static MulHelper getInstance() {
        return Holder.getInstance();
    }

    private static class Holder {
        public static MulHelper INSTANCE;

        public static MulHelper getInstance() {
            if (INSTANCE == null) {
                INSTANCE = new MulHelper();
            }
            return INSTANCE;
        }
    }

    public boolean isMulEnable() {
        Log.d(TAG, "isMulEnable: ---> mul enable=" + isMulEnable);
        return this.isMulEnable;
    }

    public void setMulEnable(boolean b) {
        Log.d(TAG, "setMulEnable: ---> enable=" + b);
        this.isMulEnable = b;
    }

    public void setStreamMode(int mode) {
        Log.d(TAG, "setStreamMode: ---> mode=" + mode);
        this.mStreamMode = mode;
    }

    public int getStreamMode() {
        Log.d(TAG, "getStreamMode: ---> mode=" + mStreamMode);
        return mStreamMode;
    }

    public void checkMulEnable(final String host, final int port, OnMulCallback callback, int timeout) {
        Log.d(TAG, "checkMulEnable: ---> tag host=" + host + " port=" + port);

        new Thread(new Runnable() {
            @Override
            public void run() {
                MulticastSocket socket = null;
                InetAddress addr = null;
                boolean enable = false;

                try {
                    addr = InetAddress.getByName(host);
                    socket = new MulticastSocket(port);
                    socket.joinGroup(addr);
                    socket.setSoTimeout(timeout);
                    Log.d(TAG, "checkMulEnable: ---> tag 0-2");

                    long startTime = System.currentTimeMillis();  // 记录开始时间
                    byte[] buffer = new byte[1024 * 10];
                    while (!enable) {
                        DatagramPacket dp = new DatagramPacket(buffer, buffer.length);
                        socket.receive(dp);  // 等待接收数据包

                        enable = dp.getLength() > 0;  // 判断是否有数据包接收到
                        Log.d(TAG, "checkMulEnable: ---> tag 0-3 enable=" + enable);

                        if (System.currentTimeMillis() - startTime > 200) {
                            Log.d(TAG, "checkMulEnable: ---> Timeout reached, no multicast data received.");
                            break;
                        }
                    }
                } catch (Exception e) {
                    Log.d(TAG, "checkMulEnable: ---> err msg=" + e.getMessage());
                } finally {
                    try {
                        if (socket != null) {
                            socket.leaveGroup(addr); // 离开组播组
                            socket.close(); // 关闭套接字
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "checkMulEnable: ---> err2 msg=" + e.getMessage());
                    }
                }

                isMulEnable = enable;
                if (callback != null) {
                    callback.onCallback(enable);
                }

                Log.d(TAG, "checkMulEnable: ---> isMulEnable=" + enable);
            }
        }).start();
    }

    public interface OnMulCallback {
        void onCallback(boolean b);
    }
}
