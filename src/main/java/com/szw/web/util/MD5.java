package com.szw.web.util;

import net.sf.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.UUID;

/**
 * Created by j.tommy on 2017/7/14.
 */
public class MD5 {
    public static String getMD5(String key) {
        byte[] source = new byte[0];
        try {
            source = key.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String s = null;
        char hexDigits[] = { // 用来将字节转换成 16 进制表示的字符
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd',
                'e', 'f' };
        try {
            java.security.MessageDigest md = java.security.MessageDigest
                    .getInstance("MD5");
            md.update(source);
            byte tmp[] = md.digest();
            char str[] = new char[16 * 2];  // 所以表示成 16 进制需要 32 个字符
            int k = 0; // 表示转换结果中对应的字符位置
            for (int i = 0; i < 16; i++) { // 从第一个字节开始，对 MD5 的每一个字节	// 转换成 16 进制字符的转换
                byte byte0 = tmp[i]; // 取第 i 个字节
                str[k++] = hexDigits[byte0 >>> 4 & 0xf]; // 取字节中高 4 位的数字转换, // 为逻辑右移，将符号位一起右移
                str[k++] = hexDigits[byte0 & 0xf]; // 取字节中低 4 位的数字转换
            }
            s = new String(str); // 换后的结果转换为字符串
        } catch (Exception e) {
            e.printStackTrace();
        }
        return s.substring(8,24);
    }


    public static void main(String[] args){
        //4b2332c3d211823914c3abf82c5e5087
        //        d211823914c3abf8
        StringBuilder str=new StringBuilder("12");
        String content=str.append("&").append("34").append("&").append("56").append("&").append("45665465").toString();
        System.out.println(MD5.getMD5(content));

        System.out.println( UUID.randomUUID().toString());
    }
}