package com.szw.web.service;

import com.szw.web.model.Tuser;

/**
 * Created by Lenovo on 2017/8/2.
 */
public interface IUserInfoService {

    public Tuser selectByPrimaryKey(Long id);

    public Tuser selectByMobile(String mobile);

    public Tuser selectByOpenId(String openId);

    public int updateByPrimaryKeySelective(Tuser record);

    public int insertSelective(Tuser record);

    public  int updateByMobile(Tuser record);
}
