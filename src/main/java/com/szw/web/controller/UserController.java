package com.szw.web.controller;


import com.szw.web.model.Tuser;
import com.szw.web.service.IUserInfoService;
import com.szw.web.util.DateUtil;
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

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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
    public JSONObject getMobileLogin(@RequestBody Tuser  user,@RequestParam String timestamp,@RequestParam String token,@RequestParam String sign) {
        JSONObject object=new JSONObject();
        try {

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
            if(StringUtils.isEmpty(user.getToken())){
                object.put("errCode","0601");
                object.put("errMsg","非法请求");
                return object;
            }else{
                if(StringUtils.isEmpty(user.getCompany())){
                    object.put("errCode","0601");
                    object.put("errMsg","公司全称为空");
                    return object;
                }
                try {
                    userInfoService.updateByMobile(user);
                    object.put("errCode","00");
                } catch (NumberFormatException e) {
                    object.put("errCode","03");
                    object.put("errMsg","请求发生错误");
                    e.printStackTrace();
                }
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return object;
    }



}
