package com.rxs.library.user.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rxs.library.user.domain.entity.UserTPO;

/** 用户表持久化接口，自定义 SQL 见同名 XML。 */
@Mapper
public interface IUserTableMapper extends BaseMapper<UserTPO> {
    UserTPO lockById(@Param("id") Long id);
}
