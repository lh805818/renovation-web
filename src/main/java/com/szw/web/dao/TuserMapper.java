package com.szw.web.dao;

import com.szw.web.model.Tuser;

public interface TuserMapper {
    int deleteByPrimaryKey(Long id);

    int insert(Tuser record);

    int insertSelective(Tuser record);

    Tuser selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Tuser record);

    int updateByPrimaryKey(Tuser record);

    Tuser selectByMobile(String mobile);

    Tuser selectByOpenId(String openId);

    int updateByMobile(Tuser record);
}