package com.z.player.bean;

import org.litepal.crud.LitePalSupport;

import java.io.Serializable;

/**
 * 作者: zyl on 2023/7/24 11:24
 * 描述:
 * 修改人:
 * 修改描述:
 * 修改日期:
 */
public class KeyValue extends LitePalSupport implements Serializable {
    public String key;
    public String value;
}
