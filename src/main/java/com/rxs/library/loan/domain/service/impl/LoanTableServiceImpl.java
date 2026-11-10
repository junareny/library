package com.rxs.library.loan.domain.service.impl;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rxs.library.loan.domain.entity.LoanTPO;
import com.rxs.library.loan.domain.service.ILoanTableService;
import com.rxs.library.loan.infrastructure.mapper.ILoanTableMapper;

/** 借阅表 MyBatis-Plus 数据操作实现。 */
@Service
public class LoanTableServiceImpl extends ServiceImpl<ILoanTableMapper, LoanTPO>
        implements ILoanTableService {
    @Override
    public IPage<LoanTPO> queryCurrentPage(Long userId, int page, int size) {
        return page(
                new Page<LoanTPO>(page, size),
                Wrappers.<LoanTPO>lambdaQuery()
                        .eq(LoanTPO::getUserId, userId)
                        .isNull(LoanTPO::getReturnedAt)
                        .orderByDesc(LoanTPO::getBorrowedAt, LoanTPO::getId));
    }

    @Override
    public long countActive(Long userId) {
        return lambdaQuery().eq(LoanTPO::getUserId, userId).isNull(LoanTPO::getReturnedAt).count();
    }

    @Override
    public boolean existsActive(Long userId, Long bookId) {
        return lambdaQuery()
                .eq(LoanTPO::getUserId, userId)
                .eq(LoanTPO::getBookId, bookId)
                .isNull(LoanTPO::getReturnedAt)
                .exists();
    }

    @Override
    public int markReturned(Long id, Long userId, Instant now) {
        return baseMapper.markReturned(id, userId, now);
    }
}
