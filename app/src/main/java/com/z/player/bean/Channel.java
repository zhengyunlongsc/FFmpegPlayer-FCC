package com.z.player.bean;

import java.io.Serializable;
import java.util.List;

public class Channel implements Serializable {
    public List<Channel> allChannelList;
    public List<Channel> hdChannelList;
    public List<Channel> cntvChannelList;
    public List<Channel> localChannelList;
    public List<Channel> psChannelList;
    public List<Channel> specialChannelList;
    public List<Channel> collectionChanneList;
    public String callSign;
    public String channelNumber;
    public String contentID;
    public String description;
    public Integer id;
    public String isCollect;
    public String isSchedule;
    public String logo;
    public String multiCastUrl;
    public String name;
    public Integer num;
    public String playUrl;
    public String pushURL;
    public String status;
    public String timeShift;
    public String timeShiftUrl;
    public String total;

    /**
     * callSign : 1高清
     * channelNumber : 1
     * contentID : 00000001000000050000000000000152
     * description : CCTV高清综合频道
     * id : 1
     * isCollect : 0
     * isSchedule : 1
     * logo : http://123.147.112.72:8081/2015/01/01/1423042606507.jpg
     * multiCastUrl : rtp://225.0.4.74:7980
     * name : CCTV-1高清
     * num : 1
     * playUrl : rtsp://123.147.112.17:8089/04000001/01000000004000000000000000000231?AuthInfo=xxx&userid=gf001&userid=gf001;igmp://225.0.4.74:7980
     * pushURL : /hls/CCTV-1高清.m3u8
     * status : 1
     * timeShift : 0
     * timeShiftUrl : rtsp://123.147.112.17:8089/04000001/01000000004000000000000000000231?AuthInfo=xxx&userid=gf001&userid=gf001
     * total : 128
     */
}
