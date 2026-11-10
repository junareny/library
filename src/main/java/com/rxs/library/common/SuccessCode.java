package com.rxs.library.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 成功消息同样绑定返回码与国际化资源键。 */
@Getter
@AllArgsConstructor
public enum SuccessCode implements MessageCode {
    /** 普通操作成功。 */
    SUCCESS("0", "library.result.success"),
    /** 分页查询成功。 */
    QUERY_SUCCESS("0", "library.result.query.success");

    private final String code;
    private final String message;
}
