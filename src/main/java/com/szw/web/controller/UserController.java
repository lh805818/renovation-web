package com.szw.web.controller;


import com.szw.web.model.Tuser;
import com.szw.web.service.IUserInfoService;
import com.szw.web.util.DateUtil;
import com.szw.web.util.MD5;
import com.szw.web.util.StringUtil;
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

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Controller
@RequestMapping("/userInfo")
public class UserController {

    protected static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${key}")
    private String key;


    @Autowired
    private IUserInfoService userInfoService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    /**
     * 完善资料
     * @param user
     * @return
     */
    @ApiOperation(value="完善资料", notes="完善公司资料")
    @ApiImplicitParam(name = "user", value = "用户信息", required = true, dataType = "json",paramType = "body")
    @RequestMapping(value = "/inputData",method = RequestMethod.POST)
    @ResponseBody
    public JSONObject getMobileLogin(@RequestBody Tuser  user,@RequestParam long timestamp,@RequestParam String token,@RequestParam String sign) {
        JSONObject object=new JSONObject();
        try {
            if(StringUtils.isEmpty(token) || StringUtils.isEmpty(sign) ||StringUtils.isEmpty(user.getCompany())){
                object.put("errCode","06");
                object.put("errMsg","参数为空");
                return object;
            }

            if((System.currentTimeMillis()-timestamp)>2*60*1000){//时间戳超过2分钟
                object.put("errCode","07");
                object.put("errMsg","接口访问超时");
                return object;
            }
            Tuser tuser= userInfoService.selectByMobile(user.getMobile());
            String redisToken=stringRedisTemplate.opsForValue().get(tuser.getUserId()+":token");
            if(StringUtil.isBlank(redisToken)||!redisToken.equals(token)){
                object.put("errCode","08");
                object.put("errMsg","token令牌失效或者令牌不对");
                return object;
            }
            Map<String, String> map =new HashMap<>();
            map.put("company",user.getCompany());
            map.put("businessLicense",user.getBusinessLicense());
            map.put("idNumber",user.getIdNumber());
            map.put("otherQualifications",user.getOtherQualifications());
            map.put("employeesNumber",String.valueOf(user.getEmployeesNumber()));
            map.put("companyArea",String.valueOf(user.getCompanyArea()));
            map.put("demandOrders",String.valueOf(user.getDemandOrders()));
            map.put("key",key);
            String signStr=TokenUtil.createSign(map);
            if(!signStr.equals(sign)){
                object.put("errCode","0601");
                object.put("errMsg","非法请求");
                return object;
            }else{
                try {
                    userInfoService.updateByMobile(user);
                    object.put("errCode","00");
                } catch (NumberFormatException e) {
                    object.put("errCode","03");
                    object.put("errMsg","请求发生错误");
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            logger.info(e.getMessage());
        }
        return object;
    }


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
        stringRedisTemplate.opsForValue().set(mobile+":vcode","666666",3, TimeUnit.MINUTES);
        return object;
    }


    /**
     * 手机号码登录
     * @param mobile
     * @param password
     * @param sign  手机号+密码+key 生成加密文
     * @return
     */
    @ApiOperation(value="手机号码登录", notes="根据手机号密码登录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "mobile", value = "手机号", required = true, dataType = "String",paramType = "query"),
            @ApiImplicitParam(name = "password", value = "密码", required = true, dataType = "String",paramType = "query"),
            @ApiImplicitParam(name = "sign", value = "调用接口凭证", required = true, dataType = "String",paramType = "query")
    })
    @RequestMapping(value = "/mobileLogin",method = RequestMethod.POST)
    @ResponseBody
    public JSONObject getMobileLogin(@RequestParam String mobile, @RequestParam String password, @RequestParam String sign) {
        JSONObject object=new JSONObject();
        if(StringUtils.isEmpty(mobile) || StringUtils.isEmpty(password) ||StringUtils.isEmpty(sign)){
            object.put("errCode","06");
            object.put("errMsg","参数为空");
            return object;
        }

        try {
            Map<String, String> map =new HashMap<>();
            map.put("mobile",mobile);
            map.put("password",password);
            map.put("key",key);
            String signStr= TokenUtil.createSign(map);

            if(!signStr.equals(sign)){
                object.put("errCode","01");
                object.put("errMsg","非法请求");
            }else{
                Tuser tuser= userInfoService.selectByMobile(mobile);
                if(tuser==null || !tuser.getPassword().equals(MD5.getMD5(password))){
                    object.put("errCode","02");
                    object.put("errMsg","很抱歉，用户不存在或密码错误！");
                }else{
                    String tokenStr=new StringBuilder(String.valueOf(System.currentTimeMillis())).append(tuser.getUserId()).toString();
                    stringRedisTemplate.opsForValue().set(tuser.getUserId()+":token",tokenStr);
                    tuser.setLoginStatus("1");
                    userInfoService.updateByPrimaryKeySelective(tuser);
                    object.put("errCode","00");
                    object.put("user", tuser);
                    object.put("token", tokenStr);
                }

            }

        } catch (Exception e) {
            object.put("errCode","03");
            object.put("errMsg","请求发生错误");
            logger.info(e.getMessage());

        }
        return object;
    }

    /**
     * 退出登录
     * @param mobile
     * @param password
     * @param sign  手机号+密码+key 生成加密文
     * @return
     */
    @ApiOperation(value="手机号码登录", notes="根据手机号密码登录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "mobile", value = "手机号", required = true, dataType = "String",paramType = "query"),
            @ApiImplicitParam(name = "password", value = "密码", required = true, dataType = "String",paramType = "query"),
            @ApiImplicitParam(name = "sign", value = "调用接口凭证", required = true, dataType = "String",paramType = "query")
    })
    @RequestMapping(value = "/mobileLogin",method = RequestMethod.POST)
    @ResponseBody
    public JSONObject getLogout(@RequestParam String mobile, @RequestParam String password, @RequestParam String sign) {
        JSONObject object=new JSONObject();
        if(StringUtils.isEmpty(mobile) || StringUtils.isEmpty(password) ||StringUtils.isEmpty(sign)){
            object.put("errCode","06");
            object.put("errMsg","参数为空");
            return object;
        }

        try {
            Map<String, String> map =new HashMap<>();
            map.put("mobile",mobile);
            map.put("password",password);
            map.put("key",key);
            String signStr= TokenUtil.createSign(map);

            if(!signStr.equals(sign)){
                object.put("errCode","01");
                object.put("errMsg","非法请求");
            }else{
                Tuser tuser= userInfoService.selectByMobile(mobile);
                if(tuser==null || !tuser.getPassword().equals(MD5.getMD5(password))){
                    object.put("errCode","02");
                    object.put("errMsg","很抱歉，用户不存在或密码错误！");
                }else{
                    String tokenStr=new StringBuilder(String.valueOf(System.currentTimeMillis())).append(tuser.getUserId()).toString();
                    stringRedisTemplate.opsForValue().set(tuser.getUserId()+":token",tokenStr);
                    tuser.setLoginStatus("1");
                    userInfoService.updateByPrimaryKeySelective(tuser);
                    object.put("errCode","00");
                    object.put("user", tuser);
                    object.put("token", tokenStr);
                }

            }

        } catch (Exception e) {
            object.put("errCode","03");
            object.put("errMsg","请求发生错误");
            logger.info(e.getMessage());

        }
        return object;
    }


    /**
     * 手机号码注册
     * @param mobile
     * @param password
     * @param vcode
     * @param sign
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
                    String userId= UUID.randomUUID().toString();
                    String tokenStr=new StringBuilder(String.valueOf(System.currentTimeMillis())).append(userId).toString();
                    stringRedisTemplate.opsForValue().set(tuser.getUserId()+":token",tokenStr);
                    tuser.setUserId(userId);
                    tuser.setMobile(mobile);
                    tuser.setLoginStatus("1");//注册自动登录
                    tuser.setPassword(MD5.getMD5(password));
                    userInfoService.insertSelective(tuser);
                    object=JSONObject.fromObject(tuser);
                    object.put("errCode","00");
                    object.put("user", tuser);
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
     * @param sign  手机号+密码+key 生成加密文
     * @return
     */
    @ApiOperation(value="忘记密码", notes="根据手机号验证码重新设置密码")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "mobile", value = "手机号", required = true, dataType = "String",paramType = "query"),
            @ApiImplicitParam(name = "password", value = "密码", required = true, dataType = "String",paramType = "query"),
            @ApiImplicitParam(name = "vcode", value = "短信验证码", required = true, dataType = "String",paramType = "query"),
            @ApiImplicitParam(name = "sign", value = "调用接口凭证", required = true, dataType = "String",paramType = "query")
    })
    @RequestMapping(value = "/forgetPassword/",method = RequestMethod.POST)
    @ResponseBody
    public JSONObject getForgetPassword(@RequestParam String mobile, @RequestParam String password,
                                        @RequestParam String sign,@RequestParam String vcode) {
        JSONObject object=new JSONObject();
        if(StringUtils.isEmpty(mobile) || StringUtils.isEmpty(password) ||StringUtils.isEmpty(sign) ||StringUtils.isEmpty(vcode)){
            object.put("errCode","06");
            object.put("errMsg","参数为空");
            return object;
        }
        StringBuilder str=new StringBuilder(mobile);
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
                String code=stringRedisTemplate.opsForValue().get(mobile+":"+"vcode");
                if(!StringUtils.isEmpty(code) && code.equals(vcode)){
                    Tuser tuser= userInfoService.selectByMobile(mobile);
                    tuser.setPassword(MD5.getMD5(password));
                    userInfoService.updateByPrimaryKeySelective(tuser);
                    object.put("errCode","00");
                }else{
                    object.put("errCode","04");
                    object.put("errMsg","验证码错误");
                }
            }
        } catch (Exception e) {
            object.put("errCode","03");
            object.put("errMsg","请求发生错误");
            e.printStackTrace();
        }
        return object;
    }





}
