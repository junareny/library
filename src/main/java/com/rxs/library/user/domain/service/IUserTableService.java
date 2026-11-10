package com.rxs.library.user.domain.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rxs.library.user.domain.entity.UserTPO;

/** 用户表数据操作服务。 */
public interface IUserTableService extends IService<UserTPO> {
    UserTPO lockById(Long id);

    UserTPO findByUsername(String username);
}
