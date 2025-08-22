package com.z.player.js;

import android.text.TextUtils;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.alibaba.fastjson.JSON;
import com.z.player.bean.Channel;
import com.z.player.util.DeviceUtils;
import com.z.player.util.SharedPreferencesUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 注释：JS接口注入类
 * 作者：liql
 * 时间：2017/9/19 14:37
 */
public class Authentication {
    private static final String TAG = Authentication.class.getName();
    private List<Map<String, String>> list = new ArrayList<>();
    private List<String> lists = new ArrayList<>();
    private Map<String, Channel> channelMap;

    public Authentication() {
        SharedPreferencesUtils.setParam("STBType", DeviceUtils.getModelName());
    }

    public Map<String, Channel> getChannelMap() {
        return channelMap;
    }

    @JavascriptInterface
    public String CTCGetConfig(String key) {
        String value = SharedPreferencesUtils.getParam(key);
        Log.d(TAG, "CTCGetConfig: ---> key=" + key + " value=" + value);
        return value;
    }

    @JavascriptInterface
    public void CUSetConfig(String key, String value) {
//        Toast.makeText(context,"调用 CTCSetConfig",Toast.LENGTH_SHORT).show();
        Log.d(TAG, "CUSetConfig: ---> key=" + key + " value=" + value);
        if (!key.equalsIgnoreCase("Channel")) {
            SharedPreferencesUtils.setParam(key, value);
            //SharedPreferencesUtils.setParam(context, "SupportHD", "1");
            //SharedPreferencesUtils.setParam(context, "dvbservicemode", "2");

            // EC6108V9 EC6108 ec6108  EC6108V9 ec6108v9 ec6108V9  EC6108V9U_pub_cqydx
            //SharedPreferencesUtils.setParam(context, "STBType", "EC6108V9U_pub_cqydx");
        } else {
            try {
//                list.add(splitStringToMap(value));
                lists.add(value);
                Channel channelInfo = JSON.parseObject(value, Channel.class);
                Log.d(TAG, "CUSetConfig: ---> channel=" + JSON.toJSONString(channelInfo));
                channelMap.put(channelInfo.channelNumber, channelInfo);
            } catch (Exception e) {
                Log.d(TAG, "CUSetConfig : Analayze Channel String Failed");
                e.printStackTrace();
            }
        }
    }

    @JavascriptInterface
    public String CUGetConfig(String key) {
        Log.d(TAG, "CUGetConfig: ---> key=" + key);
        String value = SharedPreferencesUtils.getParam(key);
        return value;
    }


    @JavascriptInterface
    public String GetConfig(String key) {
//        Toast.makeText(context,"调用 CTCGetConfig",Toast.LENGTH_SHORT).show();
        Log.d(TAG, "CTCGetConfig : key=" + key);
        String value = SharedPreferencesUtils.getParam(key);
        Log.d(TAG, "CTCGetConfig : value=" + value);
        return value;
    }

    @JavascriptInterface
    public String CUGetAuthInfo(String key) {
        Log.d(TAG, "CUGetAuthInfo: ---> key=" + key);

        return null;
    }

    @JavascriptInterface
    public String CUGetAuthInfo2(String encryptToken, boolean mode) {
        Log.d(TAG, "CUGetAuthInfo2: ---> encryptToken=" + encryptToken + " mode=" + mode);

        return "";
    }

    @JavascriptInterface
    public void CTCStartUpdate() {
    }

    @JavascriptInterface
    public int getChannelListSize() {
        Log.d(TAG, "getChannelListSize: ---> channel list size");
        if (lists != null) {
            Log.d(TAG, "getChannelListSize: ---> channel list size=" + lists.size());
            return lists.size();
        }
        Log.d(TAG, "getChannelListSize: ---> channel list size=0");
        return 0;
    }


    @JavascriptInterface
    public void CTCSetConfig(String key, String value) {
        Log.d(TAG, "CTCSetConfig: key=" + key + " value=" + value);
        if (!key.equalsIgnoreCase("Channel")) {
            SharedPreferencesUtils.setParam(key, value);
        } else {
            try {
                lists.add(value);
            } catch (Exception e) {
                Log.d(TAG, "CTCSetConfig : Analayze Channel String Failed");
            }
        }
    }

    public String GetChannelURLByUserChannelID(String UserChannelID) {
        Log.d(TAG, "GetChannelURLByUserChannelID: UserChannelID = " + UserChannelID);
        String url = "";
        try {
            Log.d(TAG, "GetChannelURLByUserChannelID: list.size() = " + list.size());
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).get("UserChannelID").equals(UserChannelID)) {
                    url = list.get(i).get("ChannelURL");
                    //url = url.replace("igmp", "rtp");//TODO
                    break;
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "GetChannelURLByUserChannelID: Search ChannelList Failed");
            e.printStackTrace();
        }
        if (url.length() == 0) {
            Log.d(TAG, "GetChannelURLByUserChannelID: Channel not found");
        }
        //url="rtp://235.254.198.51:1480";

        return url;
    }

    public String GetChannelTimeShiftURLByUserChannelID(String UserChannelID) {
        Log.d(TAG, "GetChannelTimeShiftURLByUserChannelID: UserChannelID = " + UserChannelID);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).get("UserChannelID").equals(UserChannelID)) {
                return list.get(i).get("TimeShiftURL");
            }
        }
        Log.d(TAG, "GetChannelURLByUserChannelID: Channel not found");
        return "";
    }


    //重庆联通测试
    public String GetChannelURLByChannelNumber(String ChannelNumber) {
        Log.d(TAG, "GetChannelURLByChannelNumber: ChannelNumber = " + ChannelNumber);
        //return "igmp://235.254.196.199:7980";
        String url = "";
        try {
            Log.d(TAG, "GetChannelURLByChannelNumber: list.size() = " + list.size());
            //旧版本，已屏蔽
            for (int i = 0; i < list.size(); i++) {
//                Log.d(TAG, "GetChannelURLByChannelNumber: list["+i+"] UserChannelID ="+ list.get(i).get("contentID") + " ,ChannelURL = " + list.get(i).get("playUrl") + " ,ChannelName = " + list.get(i).get("name"));
                String temp = list.get(i).get("ChannelNumber");
//                Log.d(TAG, "GetChannelURLByUserChannelID: temp="+ temp + "AAAUserChannelID=" + UserChannelID+"BBB");
//                Log.d(TAG, "GetChannelURLByUserChannelID: temp.length="+ temp.length() + "AAAUserChannelID.length=" + UserChannelID.length()+"BBB");
//                if (list.get(i).get("ChannelNumber").equals(ChannelNumber)) {
                if (list.get(i).get("num").equals(ChannelNumber)) {
                    //if(temp.equals(UserChannelID)){
                    url = list.get(i).get("multiCastUrl");
                    //url = url.replace("igmp", "rtp");//TODO
                    break;
                }
            }

            //按照王勇要求格式，提供新版
//            for(int i=0;i < channel_list.size();i++){
//                if(channel_list.get(i).getChannelNumber().equals(ChannelNumber+"")){
//                    url = channel_list.get(i).getMultiCastUrl();
//                    url = url.replace("igmp","rtp");
//                    break;
//                }
//            }
        } catch (Exception e) {
            Log.d(TAG, "GetChannelURLByChannelNumber: Search ChannelList Failed");
            e.printStackTrace();
        }
        if (url.length() == 0) {
            Log.d(TAG, "GetChannelURLByUserChannelID: Channel not found");
        }
        //url="rtp://235.254.198.51:1480";
        return url;
    }

    public String GetChannelURLByChannelNumber2(String channelNumber) {
        Log.d(TAG, "GetChannelURLByChannelNumber2: ChannelNumber = " + channelNumber);
        String url = "";
        try {
            if (channelMap != null) {
                Log.d(TAG, "GetChannelURLByChannelNumber2: ---> tag0 size=" + channelMap.size());
                Channel c = channelMap.get(channelNumber);
                if (c != null) {
                    url = c.playUrl;
                }
            }
            Log.d(TAG, "GetChannelURLByChannelNumber2: tag 7");
        } catch (Exception e) {
            Log.d(TAG, "GetChannelURLByChannelNumber: Search ChannelList Failed");
            e.printStackTrace();
        }
        if (url.length() == 0) {
            Log.d(TAG, "GetChannelURLByUserChannelID: Channel not found");
        }
        //url="rtp://235.254.198.51:1480";
        Log.d(TAG, "GetChannelURLByChannelNumber2: tag 7 url=" + url);
        return url;
    }

    public String GetChannelTimeShiftURLByChannelNumber2(String channelNumber) {
        Log.d(TAG, "GetChannelTimeShiftURLByChannelNumber2: ChannelNumber = " + channelNumber);
        //return "igmp://235.254.196.199:7980";
        String url = "";
        try {
            Log.d(TAG, "GetChannelTimeShiftURLByChannelNumber2: size = " + channelMap.size());
            Channel c = channelMap.get(channelNumber);
            if (c != null) {
                url = c.playUrl;
                Log.d(TAG, "GetChannelTimeShiftURLByChannelNumber2: --->1 url=" + url);

                if (!TextUtils.isEmpty(url)) {
                    url = url.split(";")[0];
                    url = url.replace("\"", "");
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "GetChannelTimeShiftURLByChannelNumber2: Search ChannelList Failed");
            e.printStackTrace();
        }
        if (url.length() == 0) {
            Log.d(TAG, "GetChannelTimeShiftURLByChannelNumber2: Channel not found");
        }
        //url="rtp://235.254.198.51:1480";
        Log.d(TAG, "GetChannelTimeShiftURLByChannelNumber2: --->2 url=" + url);
        return url;
    }

    public static Map<String, String> splitStringToMap(String s) {
        HashMap<String, String> map = new HashMap<String, String>();
        String[] temp = s.split(","); //通过逗号进行分割
        for (int i = 0; i < temp.length; i++) {
            String key, value;
            int pos = temp[i].indexOf("=\"");
            key = temp[i].substring(0, pos);
            value = temp[i].substring(pos + 2, temp[i].length() - 1);
            map.put(key, value);
        }
        return map;
    }

    public static Map<String, String> splitStringToMap2(String s) {
        HashMap<String, String> map = new HashMap<String, String>();
        String[] temp = s.split(","); //通过逗号进行分割
        for (int i = 0; i < temp.length; i++) {
            String key, value;
            int pos = temp[i].indexOf("\":");
//            key = temp[i].substring(0, pos);
            key = temp[i].substring(1, pos);
            value = temp[i].substring(pos + 2, temp[i].length());
            value = value.replace("\"", "");
            map.put(key, value);
        }

        return map;
    }

    public static Map<String, String> splitStringToMap3(String s) {
        HashMap<String, String> map = new HashMap<String, String>();
        String[] temp = s.split(","); //通过逗号进行分割
        for (int i = 0; i < temp.length; i++) {
            String key, value;
//            int pos = temp[i].indexOf("\":");
            int pos = temp[i].indexOf("\':");
            key = temp[i].substring(1, pos);
            value = temp[i].substring(pos + 2, temp[i].length());
//            value = value.replace("\"", "");
            value = value.replace("\'", "");
            map.put(key, value);
        }

        return map;
    }
}
