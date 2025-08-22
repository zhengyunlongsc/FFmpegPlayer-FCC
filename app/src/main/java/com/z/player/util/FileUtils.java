package com.z.player.util;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

public abstract class FileUtils {
    private static final String TAG = FileUtils.class.getName();

    /**
     * 删除文件或目录
     *
     * @param sPath 需要删除的文件路径
     * @return
     */
    public static boolean deleteFolder(String sPath) {
        boolean flag = false;
        File file = new File(sPath);
        // 判断目录或文件是否存在
        if (!file.exists()) { // 不存在返回 false
            return flag;
        } else {
            // 判断是否为文件
            if (file.isFile()) { // 为文件时调用删除文件方法
                return deleteFile(sPath);
            } else { // 为目录时调用删除目录方法
                return deleteDirectory(sPath);
            }
        }
    }

    /**
     * 删除文件
     *
     * @param sPath 需要删除文件的路径
     * @return
     */
    public static boolean deleteFile(String sPath) {
        boolean flag = false;
        try {
            File file = new File(sPath);
            if (file.isFile() && file.exists()) {
                flag = file.delete();
                Log.d(TAG, "deleteFile: ------>> flag1=" + flag);
            } else {
                Log.d(TAG, "deleteFile: ------>> flag2=" + flag);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    public static boolean deleteDir(File f) {
        if (f != null && f.isDirectory() && f.exists()) {
            return f.delete();
        } else {
            Log.d(TAG, "deleteFile: not exists");
        }
        return false;
    }

    /**
     * 删除目录
     *
     * @param sPath 需要删除的文件的路径
     * @return
     */
    public static boolean deleteDirectory(String sPath) {
        boolean flag = false;

        // 如果sPath不以文件分隔符结尾，自动添加文件分隔符
        if (!sPath.endsWith(File.separator)) {
            sPath = sPath + File.separator;
        }

        File dirFile = new File(sPath);
        // 如果dir对应的文件不存在，或者不是一个目录，则退出
        if (!dirFile.exists() || !dirFile.isDirectory()) {
            return false;
        }

        flag = true;
        // 删除文件夹下的所有文件(包括子目录)
        File[] files = dirFile.listFiles();
        for (int i = 0; i < files.length; i++) {
            // 删除子文件
            if (files[i].isFile() && !files[i].getName().endsWith(".zip")) {
                flag = deleteFile(files[i].getAbsolutePath());
                if (!flag)
                    break;
            } // 删除子目录
            else {
                flag = deleteDirectory(files[i].getAbsolutePath());
                if (!flag)
                    break;
            }
        }

        if (!flag)
            return false;
        // 删除当前目录
        if (dirFile.delete()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 根据传过来url创建文件
     */
    public static File getApkDownloadDir() {
        //必须在/cache目录或/data目录下，才能升级
        File file = Environment.getExternalStorageDirectory().getAbsoluteFile();
        Log.d(TAG, "getApkDownloadPath: ------>> file=" + file.getAbsolutePath());
        return file;
    }

    public static String getFileNameByUrl(String url) {
        return url.substring(url.lastIndexOf("/") + 1);
    }

    /**
     * 获取单个文件的MD5值！
     *
     * @param file
     * @return
     */
    public static String getFileMD5(File file) {
        if (file == null || !file.exists()) {
            return null;
        }

        StringBuilder result = new StringBuilder();
        byte[] buffer = new byte[1024 * 1024];
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            MessageDigest hash = MessageDigest.getInstance("MD5");

            int len;
            while ((len = in.read(buffer)) != -1) {
                hash.update(buffer, 0, len);
            }

            byte[] bytes = hash.digest();
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1) {
                    temp = "0" + temp;
                }
                result.append(temp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != in) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result.toString();
    }

    /**
     * 获取单个文件的MD5值！
     *
     * @param file
     * @return
     */
    public static String getFileMD5(File file, boolean isClose) {
        if (file == null || !file.exists()) {
            return null;
        }

        StringBuilder result = new StringBuilder();
        byte[] buffer = new byte[1024 * 1024];
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            MessageDigest hash = MessageDigest.getInstance("MD5");

            int len;
            while ((len = in.read(buffer)) != -1) {
                hash.update(buffer, 0, len);
            }

            byte[] bytes = hash.digest();
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1) {
                    temp = "0" + temp;
                }
                result.append(temp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (isClose) {
                if (null != in) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return result.toString();
    }

    /**
     * 获取流的MD5值！
     *
     * @return
     */
    public static String getFileMD5(InputStream in, boolean isClose) {
        if (in == null) {
            return null;
        }

        StringBuilder result = new StringBuilder();
        byte[] buffer = new byte[1024 * 1024];
        try {
            MessageDigest hash = MessageDigest.getInstance("MD5");

            int len;
            while ((len = in.read(buffer)) != -1) {
                hash.update(buffer, 0, len);
            }

            byte[] bytes = hash.digest();
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1) {
                    temp = "0" + temp;
                }
                result.append(temp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (isClose) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result.toString();
    }

    public static String readFile(String path) {
        Log.d(TAG, "readFile: ------>> path=" + path);
        StringBuffer sb = new StringBuffer();
        BufferedReader bf = null;

        try {
            bf = new BufferedReader(new FileReader(path));

            String str;
            while (!TextUtils.isEmpty(str = bf.readLine())) {
                sb.append(str);
            }
            bf.close();
        } catch (Exception e) {
            Log.d(TAG, "readFile: ------>> exception=" + e.getMessage());
            //e.printStackTrace();
        } finally {
            try {
                if (null != bf) {
                    bf.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String str = sb.toString();
        Log.d(TAG, "readFile: ------>> str=" + str);
        return str;
    }

    /*public static void writeFile(final String path, final String str, final OnFileWriteListener listener) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "writeFile: ------>> path=" + path + " str=\n" + str);
                BufferedWriter bw = null;
                File file = null;
                try {
                    file = new File(path);
                    if (!file.exists()) {
                        boolean create = file.createNewFile();
                        Log.d(TAG, "writeFile: ------>> create success....");
                    }
                    bw = new BufferedWriter(new FileWriter(path));
                    bw.write(str);
                    bw.flush();
                    bw.close();
                    Log.d(TAG, "writeFile: ------->> success......");
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (null != bw) {
                            bw.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (listener != null) {
                    listener.onComplete(path);
                }
            }
        }).start();
    }*/

}
