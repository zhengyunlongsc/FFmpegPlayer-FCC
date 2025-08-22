package com.z.player.common;

/**
 * 描述:
 * 作者: zyl on 2020/4/2 11:47
 * 修改人:
 * 修改描述:
 * 修改日期:
 */
public class Const {
    public static final String HOTEL_CODE = "HCe0166cb0a58b48e4b71202e654a49cca";
    public static final String HOST = "http://123.147.117.142:8081";//正式
    public static final String GET_CHANNEL = HOST + "/HotelTV/system/hotel/hotelCode";
    public static final String LOGIN = HOST + "/HotelTV/login";
    public static final String PATTERN_HMS = "HH:mm:ss";
    public static final String PATTERN_YMDHMS = "yyyyMMddHHmmss";
    public static final String TIME_ZONE = "GMT+00:00";
    public static final int STEP_INTERVAL = 15;//秒
    public static final int MAX_TIME_SHIFT = 7200;//秒

    /**
     * 测试组播地址有误的情况下是否走了普通组播流
     */
    public static final String FCC_SERVER_IP = "123.147.111.106";
    //public static final String FCC_SERVER_IP = "123.147.117.148";
    public static final int FCC_SERVER_PORT = 15970;

    public static final String HTML_BACKUP = "file:///android_asset/html/channelCore2.html";
    public static final String[] MODELS = {"M302A_YST_CQOTT"};
    public static final String KEY_DATA = "data";
    public static final String KEY_EVENT = "event";
    public static final String KEY_PRESS_ACTION = "android.intent.action.KEY_PRESS_ACTION";
    public static final String RECEIVE_ACTION_OTT_UNICOM = "com.wohuatv.aidlserver.OTT_UNICOM";
    public static final String KEY = "key";
    public static final String RESULT = "result";
    public static final String DEFAULT_HOME_ADDRESS = "http://123.147.117.142:8081/resource/bsepgpage/guangdian/unicom/unicom_otv/page/home.html";
    public static final boolean KEY_LOG_ENABLE = true;
}
