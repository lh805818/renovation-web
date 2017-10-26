package com.szw.web.controller;


import com.szw.web.constant.CommonConstant;
import net.sf.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by j.tommy on 2017/7/19.
 */
public class BaseController {
    protected Map<String,Object> error(Object errmsg) {
        Map<String,Object> result = new HashMap<>(2);
        result.put(CommonConstant.RETURN_CODE,CommonConstant.ERROR_CODE);
        result.put(CommonConstant.ERROR_MSG,errmsg);
        return result;
    }

    protected Map<String,Object> success() {
        Map<String,Object> result = new HashMap<>(1);
        result.put(CommonConstant.RETURN_CODE,CommonConstant.SUCCESS_CODE);
        return result;
    }

    protected Map<String,Object> success(Object data) {
        Map<String,Object> result = new HashMap<>(2);
        result.put(CommonConstant.RETURN_CODE,CommonConstant.SUCCESS_CODE);
        result.put(CommonConstant.RETURN_DATA,data);
        return result;
    }

    protected boolean VerificationTimestamp(JSONObject object,long timestamp) {
        if((System.currentTimeMillis()-timestamp)>10*60*1000){//时间戳超过10分钟
            object.put("errCode","07");
            object.put("errMsg","接口访问超时");
            return false;
        }
        return true;
    }


}
