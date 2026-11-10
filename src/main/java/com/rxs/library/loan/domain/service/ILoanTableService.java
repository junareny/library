package com.rxs.library.loan.domain.service;

import java.time.Instant;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.rxs.library.loan.domain.entity.LoanTPO;

/** 借阅表数据操作服务。 */
public interface ILoanTableService extends IService<LoanTPO> {
    IPage<LoanTPO> queryCurrentPage(Long userId, int page, int size);

    long countActive(Long userId);

    boolean existsActive(Long userId, Long bookId);

    int markReturned(Long id, Long userId, Instant now);
}
