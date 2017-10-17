package com.szw.web.controller;

import com.szw.web.model.Tuser;
import com.szw.web.service.IUserInfoService;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by Administrator on 2017/7/12.
 */
@Controller
@RequestMapping("/test")
public class TestController {
    protected static final Logger logger = LoggerFactory.getLogger(TestController.class);

    @Autowired
    private IUserInfoService userInfoService;

    @RequestMapping("/xx")
    public  ModelAndView  addUserInfo(){
        long id=1l;
        Tuser tuser=userInfoService.selectByPrimaryKey(id);
        ModelAndView mv =new ModelAndView();
        mv.addObject("users",tuser);
        mv.addObject("title","的撒是打发");
        mv.setViewName("index");
        return mv;
    }

    public static void main(String[] args) {
        Calendar nowTime = Calendar.getInstance();
        nowTime.add(Calendar.MINUTE, 2);
        System.out.println(2*60*1000);
        System.out.println(nowTime.getTime().getTime()-new Date().getTime());

    }
}
