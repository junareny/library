package com.rxs.library.loan.application.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rxs.library.book.domain.service.IBookTableService;
import com.rxs.library.common.ApiException;
import com.rxs.library.exception.LibraryErrorCode;
import com.rxs.library.loan.domain.entity.LoanTPO;
import com.rxs.library.loan.domain.service.ILoanTableService;
import com.rxs.library.loan.domain.vo.LoanVO;
import com.rxs.library.user.domain.service.IUserTableService;

import lombok.RequiredArgsConstructor;

/** 遵循用户先锁协议的借阅服务，保证额度和库存的一致性。 */
@Service
@RequiredArgsConstructor
public class LoanAppService {
    private static final int MAX_ACTIVE_LOANS = 5;
    private static final Duration LOAN_DURATION = Duration.ofDays(30);

    private final IUserTableService users;
    private final IBookTableService books;
    private final ILoanTableService loans;
    private final Clock clock;

    /**
     * 在用户行锁保护下校验额度、扣减库存并创建借阅，任一步失败均回滚。
     *
     * @param userId 已认证用户 ID
     * @param bookId 待借阅书目 ID
     * @return 创建成功的借阅记录
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public LoanVO borrow(Long userId, Long bookId) {
        lockUser(userId);
        if (loans.existsActive(userId, bookId)) {
            throw new ApiException(LibraryErrorCode.ALREADY_BORROWED);
        }
        if (loans.countActive(userId) >= MAX_ACTIVE_LOANS) {
            throw new ApiException(LibraryErrorCode.LOAN_LIMIT_EXCEEDED);
        }
        if (books.decrement(bookId) != 1) {
            if (books.getById(bookId) == null) {
                throw ApiException.notFound();
            }
            throw new ApiException(LibraryErrorCode.OUT_OF_STOCK);
        }
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        LoanTPO loan = new LoanTPO(userId, bookId, now, now.plus(LOAN_DURATION));
        loans.save(loan);
        return LoanVO.from(loan, now);
    }

    /**
     * 归还本人借阅。用户先锁，再刷新借阅记录，防止并发归还重复增加库存。
     *
     * @param userId 已认证用户 ID
     * @param loanId 待归还借阅 ID
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void returnLoan(Long userId, Long loanId) {
        LoanTPO loan = findLoan(loanId);
        if (!loan.getUserId().equals(userId)) {
            throw ApiException.notFound();
        }
        lockUser(userId);
        // 初次读取早于用户加锁，STATEMENT 缓存范围保证此处重新读取最新状态。
        loan = findLoan(loanId);
        if (!loan.getUserId().equals(userId)) {
            throw ApiException.notFound();
        }
        Long bookId = loan.getBookId();
        if (loans.markReturned(loanId, userId, clock.instant().truncatedTo(ChronoUnit.MICROS))
                != 1) {
            throw new ApiException(LibraryErrorCode.ALREADY_RETURNED);
        }
        if (books.increment(bookId) != 1) {
            throw new IllegalStateException("Inventory invariant violated while returning loan");
        }
    }

    /**
     * 分页读取当前未归还借阅，并使用同一时刻推导逾期状态。
     *
     * @param userId 已认证用户 ID
     * @param page 从一开始的页码
     * @param size 每页数量
     * @return 当前借阅分页
     */
    @Transactional(readOnly = true)
    public IPage<LoanVO> current(Long userId, int page, int size) {
        Instant now = clock.instant();
        return loans.queryCurrentPage(userId, page, size).convert(loan -> LoanVO.from(loan, now));
    }

    private LoanTPO findLoan(Long id) {
        LoanTPO loan = loans.getById(id);
        if (loan == null) {
            throw ApiException.notFound();
        }
        return loan;
    }

    private void lockUser(Long id) {
        if (users.lockById(id) == null) {
            throw new ApiException(LibraryErrorCode.USER_NO_LONGER_EXISTS);
        }
    }
}
