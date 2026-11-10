package com.rxs.library.loan.domain.vo;

import java.time.Instant;

import com.rxs.library.loan.domain.entity.LoanTPO;

/** 借阅响应，状态根据当前时刻和归还时间动态推导。 */
public record LoanVO(
        Long id,
        Long bookId,
        Instant borrowedAt,
        Instant dueAt,
        Instant returnedAt,
        String status) {
    public static LoanVO from(LoanTPO loan, Instant now) {
        String status =
                loan.getReturnedAt() != null
                        ? "RETURNED"
                        : now.isAfter(loan.getDueAt()) ? "OVERDUE" : "BORROWED";
        return new LoanVO(
                loan.getId(),
                loan.getBookId(),
                loan.getBorrowedAt(),
                loan.getDueAt(),
                loan.getReturnedAt(),
                status);
    }
}
