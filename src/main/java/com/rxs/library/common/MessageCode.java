package com.rxs.library.common;

/** 统一消息协议，message 为国际化资源键，不保存固定语言文案。 */
public interface MessageCode {
    String getCode();

    String getMessage();
}
