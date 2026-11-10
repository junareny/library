package com.rxs.library.common;

import java.util.List;

/** 参考 SIF 的分页结果，total 为总数，data 为当前页。 */
public record PageListResult<T>(String code, String msg, PageBody<T> body) {
    /** 分页数据。 */
    public record PageBody<T>(long total, List<T> data) {}
}
