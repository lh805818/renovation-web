package com.szw.web;

import com.alibaba.druid.support.http.StatViewServlet;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;

/**
 * Created by Administrator on 2017/7/6.
 */
@SpringBootApplication
@MapperScan("com.szw.web.dao")
public class Application extends SpringBootServletInitializer {
    private final static Logger logger = LoggerFactory.getLogger(Application.class);

    @Bean
    public ServletRegistrationBean statViewServlet () {
        ServletRegistrationBean reg = new ServletRegistrationBean();
        reg.setServlet (new StatViewServlet());
        reg.addUrlMappings ("/druid/*");
        return reg;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class,args);
        logger.info("Application [activity-wap] started!");
    }
}
