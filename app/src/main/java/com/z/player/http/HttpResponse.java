package com.z.player.http;

import java.io.Serializable;

/**
 * 作者: A on 2019/4/28 19:11
 * 描述: ${DESCRIPTION}
 * 修改人:
 * 修改描述:
 * 修改日期:
 */
public class HttpResponse<T> implements Serializable {
    public T data;
    public MetaBean meta;
    public int code;
    public String message;
    public String msg;
    public String token;
    public String encode;
    public String hotelName;
    public int communicationMode;

    public static class MetaBean implements Serializable {
        public String message;
        public boolean success;
    }
}
