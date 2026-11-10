package com.rxs.library.loan.infrastructure.mapper;

import java.time.Instant;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rxs.library.loan.domain.entity.LoanTPO;

/** 借阅表持久化接口，自定义 SQL 见同名 XML。 */
@Mapper
public interface ILoanTableMapper extends BaseMapper<LoanTPO> {
    int markReturned(@Param("id") Long id, @Param("userId") Long userId, @Param("now") Instant now);
}
