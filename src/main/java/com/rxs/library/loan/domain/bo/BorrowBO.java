package com.rxs.library.loan.domain.bo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import com.rxs.library.exception.LibraryErrorCode;

/** 用户身份来自认证令牌，不接受客户端指定。 */
public record BorrowBO(
        @NotNull(message = "{" + LibraryErrorCode.Keys.ID_REQUIRED + "}")
                @Positive(message = "{" + LibraryErrorCode.Keys.ID_POSITIVE + "}")
                Long bookId) {}
