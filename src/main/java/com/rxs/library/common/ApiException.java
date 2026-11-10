package com.rxs.library.common;

import org.springframework.http.HttpStatus;

import com.rxs.library.exception.LibraryErrorCode;

/** 业务异常只接受错误枚举，防止错误码、消息键与 HTTP 状态不一致。 */
public class ApiException extends RuntimeException {
    private final LibraryErrorCode errorCode;

    public ApiException(LibraryErrorCode errorCode) {
        super(java.util.Objects.requireNonNull(errorCode, "errorCode").getMessage());
        this.errorCode = errorCode;
    }

    public LibraryErrorCode errorCode() {
        return errorCode;
    }

    public HttpStatus status() {
        return errorCode.getStatus();
    }

    public String code() {
        return errorCode.getCode();
    }

    public static ApiException notFound() {
        return new ApiException(LibraryErrorCode.RESOURCE_NOT_FOUND);
    }
}
