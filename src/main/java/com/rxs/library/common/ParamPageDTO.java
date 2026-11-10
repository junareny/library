package com.rxs.library.common;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import com.rxs.library.exception.LibraryErrorCode;

import lombok.Data;

/** 分页请求，页码从一开始，dto 保存查询条件。 */
@Data
public class ParamPageDTO<T> {
    @Min(value = 1, message = "{" + LibraryErrorCode.Keys.PAGE_CURRENT + "}")
    private int pageCurrent = 1;

    @Min(value = 1, message = "{" + LibraryErrorCode.Keys.PAGE_SIZE + "}")
    @Max(value = 100, message = "{" + LibraryErrorCode.Keys.PAGE_SIZE + "}")
    private int pageSize = 20;

    @Valid private T dto;
}
