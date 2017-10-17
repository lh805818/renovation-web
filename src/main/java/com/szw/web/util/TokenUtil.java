package com.szw.web.util;

import sun.applet.Main;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by Administrator on 2017\10\16 0016.
 */
public class TokenUtil {

    public static String createSign(Map<String, String> params)
            throws UnsupportedEncodingException {
        Set<String> keysSet = params.keySet();
        Object[] keys = keysSet.toArray();
        Arrays.sort(keys);
        StringBuffer temp = new StringBuffer();
        boolean first = true;
        for (Object key : keys) {
            if (first) {
                first = false;
            } else {
                temp.append("&");
            }
            temp.append(key).append("=");
            Object value = params.get(key);
            String valueString = "";
            if (null != value) {
                valueString = String.valueOf(value);
            }
            temp.append(valueString);
        }
        return MD5.getMD5(temp.toString()).toUpperCase();
    }

    public static void main(String[] args) throws UnsupportedEncodingException {
        Map<String, String> map =new HashMap<>();
        map.put("company","1");
        map.put("businessLicense","2");
        map.put("idNumber","3");
        System.out.println(TokenUtil.createSign(map));
    }
}
