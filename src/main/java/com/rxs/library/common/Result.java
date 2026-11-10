package com.rxs.library.common;

/** 参考 SIF 的统一结果协议，body 为业务数据。 */
public record Result<T>(String code, String msg, T body) {
    public static Result<Void> failure(MessageCode code, String msg) {
        return new Result<>(code.getCode(), msg, null);
    }
}
