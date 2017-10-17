package com.szw.web.service.impl;

import com.szw.web.dao.TuserMapper;
import com.szw.web.model.Tuser;
import com.szw.web.service.IUserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by Wangx on 2017/8/2.
 */
@Service
public class UserInfoServiceImpl implements IUserInfoService {

    @Autowired
    private TuserMapper tuserMapper;

    @Override
    public Tuser selectByPrimaryKey(Long id) {
        return tuserMapper.selectByPrimaryKey(id);
    }


    public Tuser selectByMobile(String mobile){
        return tuserMapper.selectByMobile(mobile);
    }

    public Tuser selectByOpenId(String openId){
        return tuserMapper.selectByOpenId(openId);
    }


    public int updateByPrimaryKeySelective(Tuser record){
        return tuserMapper.updateByPrimaryKeySelective(record);
    }


    public int updateByMobile(Tuser record){
        return tuserMapper.updateByMobile(record);
    }


    public int insertSelective(Tuser record){
        return tuserMapper.insertSelective(record);
    }
}
