package com.szw.web.controller;


import com.szw.web.model.Tuser;
import com.szw.web.service.IUserInfoService;
import com.szw.web.util.MD5;
import com.szw.web.util.TokenUtil;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import net.sf.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;


@Controller
@RequestMapping("/login")
public class LoginController  {

    protected static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Value("${key}")
    private String key;


    @Autowired
    private IUserInfoService userInfoService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    /**
     * 获取验证码
     * @param mobile
     * @return
     */
    @ApiOperation(value="获取验证码", notes="根据手机号获取验证码")
    @ApiImplicitParam(name = "mobile", value = "手机号", required = true, dataType = "String",paramType = "path")
    @RequestMapping(value = "/getVcode/{mobile}",method = RequestMethod.GET)
    @ResponseBody
    public JSONObject getVcode(@PathVariable String mobile ) {
        JSONObject object=new JSONObject();
        if(StringUtils.isEmpty(mobile)){
            object.put("errCode","06");
            object.put("errMsg","参数为空");
            return object;
        }
        object.put("code","666666");
        stringRedisTemplate.opsForValue().set(mobile,"666666",3, TimeUnit.MINUTES);
        return object;
    }


    /**
     * 手机号码登录
     * @param mobile
     * @param password
     * @param certificate  手机号+密码+key 生成加密文
     * @return
     */
    @ApiOperation(value="手机号码登录", notes="根据手机号密码登录")
    @ApiImplicitParams({
        @ApiImplicitParam(name = "mobile", value = "手机号", required = true, dataType = "String",paramType = "query"),
        @ApiImplicitParam(name = "password", value = "密码", required = true, dataType = "String",paramType = "query"),
        @ApiImplicitParam(name = "certificate", value = "调用接口凭证", required = true, dataType = "String",paramType = "query")
    })
    @RequestMapping(value = "/mobileLogin",method = RequestMethod.POST)
    @ResponseBody
    public JSONObject getMobileLogin(@RequestParam String mobile, @RequestParam String password, @RequestParam String certificate) {
        JSONObject object=new JSONObject();
        if(StringUtils.isEmpty(mobile) || StringUtils.isEmpty(password) ||StringUtils.isEmpty(certificate)){
            object.put("errCode","06");
            object.put("errMsg","参数为空");
            return object;
        }

        StringBuilder str=new StringBuilder(mobile);
        try {
            String content=str.append("&").append(password).append("&").append(key).toString();
            if(!MD5.getMD5(content).equals(certificate)){
                object.put("errCode","01");
                object.put("errMsg","非法请求");
            }else{
               Tuser tuser= userInfoService.selectByMobile(mobile);
                if(tuser==null || !tuser.getPassword().equals(MD5.getMD5(password))){
                    object.put("errCode","02");
                    object.put("errMsg","很抱歉，用户不存在或密码错误！");
                }else{
                    String tokenStr=new StringBuilder(String.valueOf(System.currentTimeMillis())).append(mobile).toString();
                    stringRedisTemplate.opsForValue().set(tuser.getUserId()+":",tokenStr);
                    userInfoService.updateByPrimaryKeySelective(tuser);
                    object=JSONObject.fromObject(tuser);
                }
                object.put("errCode","00");
            }

        } catch (NumberFormatException e) {
            object.put("errCode","03");
            object.put("errMsg","请求发生错误");
            e.printStackTrace();
        }
        return object;
    }


    /**
     * 手机号码注册
     * @param mobile
     * @param password
     * @param vcode
     * @param certificate
     * @return
     */
    @ApiOperation(value="手机号码注册", notes="根据手机号密码验证码注册")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "mobile", value = "手机号", required = true, dataType = "String",paramType = "query"),
            @ApiImplicitParam(name = "password", value = "密码", required = true, dataType = "String",paramType = "query"),
            @ApiImplicitParam(name = "vcode", value = "短信验证码", required = true, dataType = "String",paramType = "query"),
            @ApiImplicitParam(name = "sign", value = "调用接口凭证", required = true, dataType = "String",paramType = "query")
    })
    @RequestMapping(value = "/mobileRegister",method = RequestMethod.POST)
    @ResponseBody
    public JSONObject getMobileRegister(@RequestParam String mobile, @RequestParam String password,@RequestParam String vcode,@RequestParam String sign) {
        JSONObject object=new JSONObject();
        if(StringUtils.isEmpty(mobile) || StringUtils.isEmpty(password) ||StringUtils.isEmpty(sign) ||StringUtils.isEmpty(vcode)){
            object.put("errCode","06");
            object.put("errMsg","参数为空");
            return object;
        }
        try {
            Map<String, String> map =new HashMap<>();
            map.put("mobile",mobile);
            map.put("password",password);
            map.put("vcode",vcode);
            map.put("key",key);
            String signStr= TokenUtil.createSign(map);
            if(!signStr.equals(sign)){
                object.put("errCode","01");
                object.put("errMsg","非法请求");
            }else{
                //String code=stringRedisTemplate.opsForValue().get(mobile);
                String code=vcode;
                if(!StringUtils.isEmpty(code) && code.equals(vcode)){
                    Tuser tuser= new Tuser();
                    String userId=UUID.randomUUID().toString();
                    String tokenStr=new StringBuilder(String.valueOf(System.currentTimeMillis())).append(userId).toString();
                    tuser.setUserId(userId);
                    tuser.setMobile(mobile);
                    tuser.setPassword(MD5.getMD5(password));
                    userInfoService.insertSelective(tuser);
                    object=JSONObject.fromObject(tuser);
                    object.put("errCode","00");
                    object.put("token",tokenStr);
                }else {
                    object.put("errCode","04");
                    object.put("errMsg","验证码错误");
                }
            }
        } catch (Exception e) {
            if(e.getMessage().contains("Duplicate entry")){
                object.put("errCode","05");
                object.put("errMsg","该手机号码已经注册");
            }else{
                object.put("errCode","03");
                object.put("errMsg","请求发生错误");
            }
            logger.info(e.getMessage());
        }
        return object;
    }

    /**
     * 忘记密码
     * @param mobile
     * @param password
     * @param certificate  手机号+密码+key 生成加密文
     * @return
     */
    @ApiOperation(value="忘记密码", notes="根据手机号验证码重新设置密码")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "mobile", value = "手机号", required = true, dataType = "String",paramType = "query"),
            @ApiImplicitParam(name = "password", value = "密码", required = true, dataType = "String",paramType = "query"),
            @ApiImplicitParam(name = "vcode", value = "短信验证码", required = true, dataType = "String",paramType = "query"),
            @ApiImplicitParam(name = "certificate", value = "调用接口凭证", required = true, dataType = "String",paramType = "query")
    })
    @RequestMapping(value = "/forgetPassword/{mobile}/{password}/{vcode}/{certificate}",method = RequestMethod.POST)
    @ResponseBody
    public JSONObject getForgetPassword(@RequestParam String mobile, @RequestParam String password,
                                        @RequestParam String certificate,@RequestParam String vcode) {
        JSONObject object=new JSONObject();
        if(StringUtils.isEmpty(mobile) || StringUtils.isEmpty(password) ||StringUtils.isEmpty(certificate) ||StringUtils.isEmpty(vcode)){
            object.put("errCode","06");
            object.put("errMsg","参数为空");
            return object;
        }
        StringBuilder str=new StringBuilder(mobile);
        try {
            String content=str.append("&").append(password).append("&").append("vocde").append("&").append(key).toString();
            if(!MD5.getMD5(content).equals(certificate)){
                object.put("errCode","01");
                object.put("errMsg","非法请求");
            }else{
                String code=stringRedisTemplate.opsForValue().get(mobile);
                if(!StringUtils.isEmpty(code) && code.equals(vcode)){
                    Tuser tuser= userInfoService.selectByMobile(mobile);
                    tuser.setPassword(password);
                    userInfoService.updateByPrimaryKeySelective(tuser);
                    object.put("errCode","00");
                }else{
                    object.put("errCode","04");
                    object.put("errMsg","验证码错误");
                }
            }
        } catch (NumberFormatException e) {
            object.put("errCode","03");
            object.put("errMsg","请求发生错误");
            e.printStackTrace();
        }
        return object;
    }


}
