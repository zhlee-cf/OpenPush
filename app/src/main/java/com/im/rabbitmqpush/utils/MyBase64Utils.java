package com.im.rabbitmqpush.utils;

import android.util.Base64;

/**
 * Base64编码与解码工具类
 * Created by Administrator on 2016/4/7.
 */
public class MyBase64Utils {
    /**
     * 方法 编码
     * @param str
     * @return
     */
    public static String encodeToString(String str) {
        return Base64.encodeToString(str.getBytes(), Base64.DEFAULT);
    }
    /**
     * 方法 解码
     * @param str
     * @return
     */
    public static String decode(String str) {
        return new String(Base64.decode(str, Base64.DEFAULT));
    }
}
