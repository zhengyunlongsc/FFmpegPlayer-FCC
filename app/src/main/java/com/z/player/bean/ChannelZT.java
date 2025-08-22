package com.z.player.bean;

import java.io.Serializable;

/**
 * 注释：
 * 作者：ndy
 * 时间：2019/3/28 22:54
 */
public class ChannelZT implements Serializable {
    public int id;
    public int num=-1;
    public String isCollect;
    public String callSign;
    public String channelNumber;
    public String contentID;
    public String description;
    public String isSchedule;
    public String logo;
    public String multiCastUrl;
    public String name;
    public String playUrl;
    public String pushURL;
    public String status;
    public String timeShift;
    public String timeShiftUrl;
    public String total;
    public String multicastUrl;
    public String physicalchannelId;
    public String productID;
    public String productTargetAddr;
    public int isPaid;
    public boolean isCollected;
    /**
     * createBy : null
     * createTime : null
     * updateBy : null
     * updateTime : null
     * remark : null
     * channelCode : 00000001000000050000000000000152
     * channelName : CCTV-1高清
     * unicastUrl : rtsp://123.147.112.17:8089/04000001/01000000004000000000000000000231?AuthInfo=xxx&userid=gf001&userid=gf001;igmp://225.0.4.74:7980
     */
    public String channelCode;
    public String channelName;
    public String unicastUrl;
}
