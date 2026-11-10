package com.rxs.library.user.domain.service.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rxs.library.user.domain.entity.UserTPO;
import com.rxs.library.user.domain.service.IUserTableService;
import com.rxs.library.user.infrastructure.mapper.IUserTableMapper;

/** 用户表 MyBatis-Plus 数据操作实现。 */
@Service
public class UserTableServiceImpl extends ServiceImpl<IUserTableMapper, UserTPO>
        implements IUserTableService {
    @Override
    public UserTPO lockById(Long id) {
        return baseMapper.lockById(id);
    }

    @Override
    public UserTPO findByUsername(String username) {
        return lambdaQuery().eq(UserTPO::getUsername, username).one();
    }
}
