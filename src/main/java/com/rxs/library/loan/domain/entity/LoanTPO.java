package com.rxs.library.loan.domain.entity;

import java.time.Instant;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;
import lombok.experimental.Accessors;

/** 保留历史记录的借阅模型，未归还记录占用库存和额度。 */
@Data
@Accessors(chain = true)
@TableName("loans")
public class LoanTPO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long bookId;

    private Instant borrowedAt;

    private Instant dueAt;

    private Instant returnedAt;

    public LoanTPO() {}

    public LoanTPO(Long userId, Long bookId, Instant borrowedAt, Instant dueAt) {
        this.userId = userId;
        this.bookId = bookId;
        this.borrowedAt = borrowedAt;
        this.dueAt = dueAt;
    }
}
