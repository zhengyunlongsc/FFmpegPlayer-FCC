package com.z.player.bean;

import java.io.Serializable;
import java.util.List;

/**
 * 作者: zyl on 2019/6/5 9:49
 * 描述: ${DESCRIPTION}
 * 修改人:
 * 修改描述:
 * 修改日期:
 */
public class ChannelInfo implements Serializable {
    public List<ChannelZT> allChannelList;
    public List<ChannelZT> hdChannelList;
    public List<ChannelZT> cntvChannelList;
    public List<ChannelZT> localChannelList;
    public List<ChannelZT> psChannelList;
    public List<ChannelZT> specialChannelList;
    public List<ChannelZT> collectionChanneList;
}
