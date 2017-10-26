package com.szw.web.controller.remote;


import com.szw.web.constant.CommonConstant;
import com.szw.web.controller.BaseController;
import com.szw.web.model.Tuser;
import com.szw.web.service.IUserInfoService;
import com.szw.web.util.*;
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
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


@Controller
@RequestMapping("/api/user")
public class UserController extends BaseController {

    protected static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${key}")
    private String key;

    @Value("${file.upload.path}")
    private String uploadPath;

    @Value("${file.server.url}")
    private String fileServerUrl;

    @Value("${sms.send.url}")
    private String smsSendUrl;

    @Value("${sms.account}")
    private String smsAccount;

    @Value("${sms.password}")
    private String smsPassword;



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
    @ApiImplicitParams({
            @ApiImplicitParam(name = "mobile", value = "手机号", required = true, dataType = "String",paramType = "query"),
            @ApiImplicitParam(name = "timestamp", value = "时间戳", required = true, dataType = "long",paramType = "query"),
            @ApiImplicitParam(name = "sign", value = "调用接口加签密文", required = true, dataType = "String",paramType = "query")
    })
    @RequestMapping(value = "/getVcode/v1",method = RequestMethod.POST)
    @ResponseBody
    public JSONObject getVcode(@RequestParam String mobile, @RequestParam long timestamp, @RequestParam String sign) {
        JSONObject object=new JSONObject();
        if(StringUtils.isEmpty(mobile) || StringUtils.isEmpty(sign) ){
            object.put("errCode","06");
            object.put("errMsg","参数为空");
            return object;
        }
        try {
            if(VerificationTimestamp(object,timestamp)){
                Map<String, String> map =new HashMap<>();
                map.put("mobile",mobile);
                map.put("timestamp",String.valueOf(timestamp));
                map.put("key",key);
                String signStr=TokenUtil.createSign(map);
                if(!signStr.equals(sign)){
                    object.put("errCode","01");
                    object.put("errMsg","非法请求");
                    return object;
                }
                JSONObject jsonObject=new JSONObject();
                jsonObject.put("account",smsAccount);
                jsonObject.put("password",smsPassword);
                String vcode=StringUtil.getFourRandom();
                String msg=CommonConstant.smsContent.replace("company",CommonConstant.smsSignString).replace("vcode",vcode);
                jsonObject.put("msg",msg);
                jsonObject.put("phone",mobile);
                jsonObject.put("sendtime","");
                jsonObject.put("report","false");
                jsonObject.put("extend","");
                jsonObject.put("uid","");
                String json=jsonObject.toString();
                String result= HttpClientUtil.doPost(smsSendUrl,json,"utf-8");
                Map<String,String> obj= JsonUtil.jsonToMap(result);
                String code=obj.get("code");
                if(!StringUtil.isBlank(code) && code.equals("0")){
                    object.put("errCode","00");
                    object.put("vcode",vcode);
                }else{
                    object.put("errCode","04");
                    object.put("errMsg",obj.get("errorMsg"));
                }
                stringRedisTemplate.opsForValue().set(mobile+":vcode",vcode,3, TimeUnit.MINUTES);
            }else{
                object.put("errCode","09");
                object.put("errMsg","请求过期");
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
            @ApiImplicitParam(name = "timestamp", value = "时间戳", required = true, dataType = "long",paramType = "query"),
            @ApiImplicitParam(name = "sign", value = "调用接口加签密文", required = true, dataType = "String",paramType = "query")
    })
    @RequestMapping(value = "/mobileRegister/v1",method = RequestMethod.POST)
    @ResponseBody
    public JSONObject getMobileRegister(@RequestParam String mobile, @RequestParam String password,@RequestParam String vcode,@RequestParam long timestamp,@RequestParam String sign) {
        JSONObject object=new JSONObject();
        if(StringUtils.isEmpty(mobile) || StringUtils.isEmpty(password) ||StringUtils.isEmpty(sign) ||StringUtils.isEmpty(vcode)){
            object.put("errCode","06");
            object.put("errMsg","参数为空");
            return object;
        }
        String userId= UUID.randomUUID().toString();
        try {
            if(VerificationTimestamp(object,timestamp)){
                Map<String, String> map =new HashMap<>();
                map.put("mobile",mobile);
                map.put("password",password);
                map.put("vcode",vcode);
                map.put("timestamp",String.valueOf(timestamp));
                map.put("key",key);
                String signStr= TokenUtil.createSign(map);
                if(!signStr.equals(sign)){
                    object.put("errCode","01");
                    object.put("errMsg","非法请求");
                }else{
                    //String code=stringRedisTemplate.opsForValue().get(mobile+":vcode");
                    String code=vcode;
                    if(!StringUtils.isEmpty(code) && code.equals(vcode)){
                        Tuser tuser= new Tuser();
                        String tokenStr=MD5.getMD5(new StringBuilder(String.valueOf(System.currentTimeMillis())).append(userId).toString());
                        stringRedisTemplate.opsForValue().set(userId+":token",tokenStr);
                        tuser.setUserId(userId);
                        tuser.setMobile(mobile);
                        tuser.setLoginStatus("1");//注册自动登录
                        tuser.setPassword(MD5.getMD5(password));
                        userInfoService.insertSelective(tuser);
                        object.put("errCode","00");
                        JSONObject user=new JSONObject();
                        user.put("userId",tuser.getUserId());
                        object.put("userId", user);
                        object.put("token",tokenStr);
                    }else {
                        object.put("errCode","04");
                        object.put("errMsg","验证码错误");
                    }
                }
            }else{
                object.put("errCode","09");
                object.put("errMsg","请求过期");
            }
        } catch (Exception e) {
            stringRedisTemplate.delete(userId+":token");
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
            @ApiImplicitParam(name = "timestamp", value = "时间戳", required = true, dataType = "long",paramType = "query"),
            @ApiImplicitParam(name = "sign", value = "调用接口加签密文", required = true, dataType = "String",paramType = "query")
    })
    @RequestMapping(value = "/mobileLogin/v1",method = RequestMethod.POST)
    @ResponseBody
    public JSONObject getMobileLogin(@RequestParam String mobile, @RequestParam String password,@RequestParam long timestamp, @RequestParam String sign) {
        JSONObject object=new JSONObject();
        if(StringUtils.isEmpty(mobile) || StringUtils.isEmpty(password) ||StringUtils.isEmpty(sign)){
            object.put("errCode","06");
            object.put("errMsg","参数为空");
            return object;
        }

        try {
            if(VerificationTimestamp(object,timestamp)){
                Map<String, String> map =new HashMap<>();
                map.put("mobile",mobile);
                map.put("password",password);
                map.put("timestamp",String.valueOf(timestamp));
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
                        String tokenStr=MD5.getMD5(new StringBuilder(String.valueOf(System.currentTimeMillis())).append(tuser.getUserId()).toString());
                        stringRedisTemplate.opsForValue().set(tuser.getUserId()+":token",tokenStr);
                        tuser.setLoginStatus("1");
                        userInfoService.updateByPrimaryKeySelective(tuser);
                        object.put("errCode","00");
                        JSONObject user=new JSONObject();
                        user.put("userId",tuser.getUserId());
                        user.put("createTime",tuser.getCreateTime());
                        user.put("loginStatus",tuser.getLoginStatus());
                        user.put("openid",tuser.getOpenid());
                        user.put("mobile",tuser.getMobile());
                        user.put("businessLicense",fileServerUrl+tuser.getBusinessLicense());
                        user.put("idNumber",fileServerUrl+tuser.getIdNumber());
                        user.put("otherQualifications",fileServerUrl+tuser.getOtherQualifications());
                        user.put("employeesNumber",tuser.getEmployeesNumber());
                        user.put("companyArea",tuser.getCompanyArea());
                        user.put("demandOrders",tuser.getDemandOrders());
                        object.put("user", user);
                        object.put("token", tokenStr);
                    }

                }
            }else{
                object.put("errCode","09");
                object.put("errMsg","请求过期");
            }
        } catch (Exception e) {
            object.put("errCode","03");
            object.put("errMsg","请求发生错误");
            logger.info(e.getMessage());

        }
        return object;
    }


    /**
     * 开放平台登录
     * @param openId
     * @param sign  手机号+密码+key 生成加密文
     * @return
     */
    @ApiOperation(value="开放平台登录", notes="根据手机号密码登录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "openId", value = "手机号", required = true, dataType = "String",paramType = "query"),
            @ApiImplicitParam(name = "timestamp", value = "时间戳", required = true, dataType = "long",paramType = "query"),
            @ApiImplicitParam(name = "sign", value = "调用接口加签密文", required = true, dataType = "String",paramType = "query")
    })
    @RequestMapping(value = "/openLogin/v1",method = RequestMethod.POST)
    @ResponseBody
    public JSONObject getOpenLogin(@RequestParam String openId,@RequestParam long timestamp, @RequestParam String sign) {
        JSONObject object=new JSONObject();
        if(!StringUtil.isBlank(openId)){
            String userId= UUID.randomUUID().toString();
            try {
                Tuser tuser=userInfoService.selectByOpenId(openId);
                String tokenStr=MD5.getMD5(new StringBuilder(String.valueOf(System.currentTimeMillis())).append(userId).toString());
                stringRedisTemplate.opsForValue().set(userId+":token",tokenStr);
                if(tuser==null){//数据库不存在 就存一条记录
                    tuser=new Tuser();
                    tuser.setUserId(userId);
                    tuser.setNickname("xx");
                    tuser.setLoginStatus("1");
                    userInfoService.insertSelective(tuser);
                }else{
                    tuser.setLoginStatus("1");
                    userInfoService.updateByPrimaryKeySelective(tuser);
                    object.put("errCode","00");
                    JSONObject user=new JSONObject();
                    user.put("userId",tuser.getUserId());
                    user.put("createTime",tuser.getCreateTime());
                    user.put("loginStatus",tuser.getLoginStatus());
                    user.put("openid",tuser.getOpenid());
                    user.put("mobile",tuser.getMobile());
                    user.put("businessLicense",fileServerUrl+tuser.getBusinessLicense());
                    user.put("idNumber",fileServerUrl+tuser.getIdNumber());
                    user.put("otherQualifications",fileServerUrl+tuser.getOtherQualifications());
                    user.put("employeesNumber",tuser.getEmployeesNumber());
                    user.put("companyArea",tuser.getCompanyArea());
                    user.put("demandOrders",tuser.getDemandOrders());
                    object.put("user", user);
                    object.put("token", tokenStr);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return object;
    }

    /**
     * 绑定手机号码
     * @param mobile
     * @param sign  手机号+密码+key 生成加密文
     * @return
     */
    @ApiOperation(value="绑定手机号码", notes="第三方登录需要绑定手机号码")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "openId", value = "手机号", required = true, dataType = "String",paramType = "query"),
            @ApiImplicitParam(name = "mobile", value = "手机号", required = true, dataType = "String",paramType = "query"),
            @ApiImplicitParam(name = "timestamp", value = "时间戳", required = true, dataType = "long",paramType = "query"),
            @ApiImplicitParam(name = "token", value = "token令牌", required = true, dataType = "String",paramType = "query"),
            @ApiImplicitParam(name = "sign", value = "调用接口加签密文", required = true, dataType = "String",paramType = "query")
    })
    @RequestMapping(value = "/bindMobile/v1",method = RequestMethod.POST)
    @ResponseBody
    public JSONObject getBindMobile(@RequestParam String openId,@RequestParam String mobile,@RequestParam long timestamp,@RequestParam String token, @RequestParam String sign) {
        JSONObject object=new JSONObject();
        if(StringUtils.isEmpty(mobile) ||StringUtils.isEmpty(sign)){
            object.put("errCode","06");
            object.put("errMsg","参数为空");
            return object;
        }
        try {
            if(VerificationTimestamp(object,timestamp)){
                Map<String, String> map =new HashMap<>();
                map.put("mobile",mobile);
                map.put("timestamp",String.valueOf(timestamp));
                map.put("token",token);
                map.put("key",key);
                String signStr= TokenUtil.createSign(map);
                if(!signStr.equals(sign)){
                    object.put("errCode","01");
                    object.put("errMsg","非法请求");
                }else{
                    Tuser tuser= userInfoService.selectByOpenId(openId);
                    if(tuser==null){
                        object.put("errCode","02");
                        object.put("errMsg","很抱歉，用户不存在！");
                    }else{
                        String redisToken=stringRedisTemplate.opsForValue().get(tuser.getUserId()+":token");
                        if(StringUtil.isBlank(redisToken)||!redisToken.equals(token)){
                            object.put("errCode","07");
                            object.put("errMsg","token令牌失效");
                            return object;
                        }
                        tuser.setMobile(mobile);
                        Tuser tuserMobile= userInfoService.selectByMobile(mobile);//根据手机查出数据
                        if(tuserMobile!=null){
                            if(StringUtil.isBlank(tuser.getCompany()) && !StringUtil.isBlank(tuserMobile.getCompany())){
                                tuser.setCompany(tuserMobile.getCompany());
                            }
                            if(StringUtil.isBlank(tuser.getBusinessLicense()) && !StringUtil.isBlank(tuserMobile.getBusinessLicense())){
                                tuser.setBusinessLicense(tuserMobile.getBusinessLicense());
                            }
                            if(StringUtil.isBlank(tuser.getIdNumber()) && !StringUtil.isBlank(tuserMobile.getIdNumber())){
                                tuser.setIdNumber(tuserMobile.getIdNumber());
                            }
                            if(StringUtil.isBlank(tuser.getOtherQualifications()) && !StringUtil.isBlank(tuserMobile.getOtherQualifications())){
                                tuser.setOtherQualifications(tuserMobile.getOtherQualifications());
                            }
                            if(tuser.getEmployeesNumber().intValue()==0 && tuserMobile.getEmployeesNumber().intValue()!=0){
                                tuser.setEmployeesNumber(tuserMobile.getEmployeesNumber());
                            }
                            if(tuser.getCompanyArea().intValue()==0 && tuserMobile.getCompanyArea().intValue()!=0){
                                tuser.setCompanyArea(tuserMobile.getCompanyArea());
                            }
                            if(tuser.getDemandOrders().intValue()==0 && tuserMobile.getDemandOrders().intValue()!=0){
                                tuser.setDemandOrders(tuserMobile.getDemandOrders());
                            }

                            Date mobileDate=DateUtil.parseDate(tuserMobile.getCreateTime(),"yyyy-MM-dd HH:mm:ss");
                            Date tuDate=DateUtil.parseDate(tuser.getCreateTime(),"yyyy-MM-dd HH:mm:ss");
                            if(mobileDate.before(tuDate)){
                                tuser.setCreateTime(tuserMobile.getCreateTime());
                            }
                        }
                        userInfoService.deleteByPrimaryKey(tuserMobile.getId());
                        userInfoService.updateByPrimaryKeySelective(tuser);
                        object.put("errCode","00");
                    }
                }
            }else{
                object.put("errCode","09");
                object.put("errMsg","请求过期");
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
     * @param sign  手机号+密码+key 生成加密文
     * @return
     */
    @ApiOperation(value="退出登录", notes="退出当前登录")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "mobile", value = "手机号", required = true, dataType = "String",paramType = "query"),
            @ApiImplicitParam(name = "timestamp", value = "时间戳", required = true, dataType = "long",paramType = "query"),
            @ApiImplicitParam(name = "token", value = "token令牌", required = true, dataType = "String",paramType = "query"),
            @ApiImplicitParam(name = "sign", value = "调用接口加签密文", required = true, dataType = "String",paramType = "query")
    })
    @RequestMapping(value = "/getLogout/v1",method = RequestMethod.POST)
    @ResponseBody
    public JSONObject getLogout(@RequestParam String mobile,@RequestParam long timestamp,@RequestParam String token,@RequestParam String sign) {
        JSONObject object=new JSONObject();
        if(StringUtils.isEmpty(mobile) ||StringUtils.isEmpty(sign)){
            object.put("errCode","06");
            object.put("errMsg","参数为空");
            return object;
        }
        try {
            if(VerificationTimestamp(object,timestamp)){
                Map<String, String> map =new HashMap<>();
                map.put("mobile",mobile);
                map.put("timestamp",String.valueOf(timestamp));
                map.put("token",token);
                map.put("key",key);
                String signStr= TokenUtil.createSign(map);
                if(!signStr.equals(sign)){
                    object.put("errCode","01");
                    object.put("errMsg","非法请求");
                }else{
                    Tuser tuser= userInfoService.selectByMobile(mobile);
                    if(tuser==null){
                        object.put("errCode","02");
                        object.put("errMsg","很抱歉，用户不存在！");
                    }else{
                        String redisToken=stringRedisTemplate.opsForValue().get(tuser.getUserId()+":token");
                        if(StringUtil.isBlank(redisToken)||!redisToken.equals(token)){
                            object.put("errCode","07");
                            object.put("errMsg","token令牌失效");
                            return object;
                        }
                        stringRedisTemplate.delete(tuser.getUserId()+":token");
                        tuser.setLoginStatus("0");
                        userInfoService.updateByPrimaryKeySelective(tuser);
                        object.put("errCode","00");
                    }
                }
            }else{
                object.put("errCode","09");
                object.put("errMsg","请求过期");
            }
        } catch (Exception e) {
            object.put("errCode","03");
            object.put("errMsg","请求发生错误");
            logger.info(e.getMessage());

        }
        return object;
    }

    /**
     * 完善资料
     * @param user
     * @return
     */
    @ApiOperation(value="完善资料", notes="完善公司资料")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "user", value = "用户信息", required = true, dataType = "json",paramType = "body"),
            @ApiImplicitParam(name = "timestamp", value = "时间戳", required = true, dataType = "long",paramType = "query"),
            @ApiImplicitParam(name = "token", value = "token令牌", required = true, dataType = "String",paramType = "query"),
            @ApiImplicitParam(name = "sign", value = "调用接口加签密文", required = true, dataType = "String",paramType = "query"),
    })

    @RequestMapping(value = "/inputData/v1",method = RequestMethod.POST)
    @ResponseBody
    public JSONObject inputData(@RequestBody Tuser  user,@RequestParam long timestamp,@RequestParam String token,@RequestParam String sign) {
        JSONObject object=new JSONObject();
        try {
            if(StringUtils.isEmpty(token) || StringUtils.isEmpty(sign) ||StringUtils.isEmpty(user.getCompany()) ){
                object.put("errCode","06");
                object.put("errMsg","参数为空");
                return object;
            }

            if(VerificationTimestamp(object,timestamp)){
                Map<String, String> map =new HashMap<>();
                map.put("mobile",user.getMobile());
                map.put("company",user.getCompany());
                map.put("businessLicense",user.getBusinessLicense());
                map.put("idNumber",user.getIdNumber());
                map.put("otherQualifications",user.getOtherQualifications());
                map.put("employeesNumber",String.valueOf(user.getEmployeesNumber()));
                map.put("companyArea",String.valueOf(user.getCompanyArea()));
                map.put("demandOrders",String.valueOf(user.getDemandOrders()));
                map.put("timestamp",String.valueOf(timestamp));
                map.put("token",token);
                map.put("key",key);
                String signStr=TokenUtil.createSign(map);
                if(!signStr.equals(sign)){
                    object.put("errCode","01");
                    object.put("errMsg","非法请求");
                    return object;
                }else{
                    Tuser tuser= userInfoService.selectByMobile(user.getMobile());
                    String redisToken=stringRedisTemplate.opsForValue().get(tuser.getUserId()+":token");
                    if(StringUtil.isBlank(redisToken)||!redisToken.equals(token)){
                        object.put("errCode","07");
                        object.put("errMsg","token令牌失效或者令牌不对");
                        return object;
                    }
                    userInfoService.updateByMobile(user);
                    object.put("errCode","00");
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
            @ApiImplicitParam(name = "timestamp", value = "时间戳", required = true, dataType = "long",paramType = "query"),
            @ApiImplicitParam(name = "sign", value = "调用接口加签密文", required = true, dataType = "String",paramType = "query")
    })
    @RequestMapping(value = "/forgetPassword/v1",method = RequestMethod.POST)
    @ResponseBody
    public JSONObject getForgetPassword(@RequestParam String mobile, @RequestParam String password,@RequestParam String vcode,
                                        @RequestParam long timestamp,  @RequestParam String sign) {
        JSONObject object=new JSONObject();
        if(StringUtils.isEmpty(mobile) || StringUtils.isEmpty(password) ||StringUtils.isEmpty(sign) ||StringUtils.isEmpty(vcode)){
            object.put("errCode","06");
            object.put("errMsg","参数为空");
            return object;
        }
        try {
            if(VerificationTimestamp(object,timestamp)){
                Map<String, String> map =new HashMap<>();
                map.put("mobile",mobile);
                map.put("password",password);
                map.put("vcode",vcode);
                map.put("timestamp",String.valueOf(timestamp));
                map.put("key",key);
                String signStr= TokenUtil.createSign(map);
                if(!signStr.equals(sign)){
                    object.put("errCode","01");
                    object.put("errMsg","非法请求");
                }else{
                    //String code=stringRedisTemplate.opsForValue().get(mobile+":"+"vcode");
                    String code=vcode;
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
            }else{
                object.put("errCode","09");
                object.put("errMsg","请求过期");
            }
        } catch (Exception e) {
            object.put("errCode","03");
            object.put("errMsg","请求发生错误");
            e.printStackTrace();
        }
        return object;
    }

    @ApiOperation(value="上传文件", notes="上传文件")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "file", value = "上传的文件参数", required = true, dataType = "MultipartFile",paramType = "query")
    })
    @RequestMapping(value = "/uploadFile/v1", method = RequestMethod.POST)
    @ResponseBody
    public JSONObject uploadFile(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        JSONObject object=new JSONObject();
        if (file.isEmpty()) {
            object.put("errCode","01");
            object.put("errMsg","文件参数为空");
            return object;
        }
        // 获取文件名
        String fileName = file.getOriginalFilename();
        // 获取文件的后缀名
        String suffixName = fileName.substring(fileName.lastIndexOf("."));
        // 解决中文问题，liunx下中文路径，图片显示问题
        fileName = UUID.randomUUID() + suffixName;
        String relativeUrl = DateUtil.format(new Date(),"yyyy-MM-dd") + CommonConstant.FILE_SEPARATOR + fileName;
        File dest = new File(uploadPath+relativeUrl);
        // 检测是否存在目录
        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdirs();
        }
        try {
            file.transferTo(dest);
            object.put("errCode","00");
            object.put("filePath",relativeUrl);
            return object;
        } catch (Exception e) {
            object.put("errCode","08");
            object.put("errMsg","文件上传失败");
            return object;
        }
    }



}
