package com.rxs.library.i18n;

import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rxs.library.common.PageListResult;
import com.rxs.library.common.Result;
import com.rxs.library.common.SuccessCode;

import lombok.RequiredArgsConstructor;

/** 按当前请求语言构造成功结果，供控制器或其他后端组件复用。 */
@Component
@RequiredArgsConstructor
public class LocalizedResultFactory {
    private final LocalizedMessages messages;

    public <T> Result<T> success(T body) {
        return new Result<>(
                SuccessCode.SUCCESS.getCode(), messages.text(SuccessCode.SUCCESS), body);
    }

    public <T> PageListResult<T> page(IPage<T> page) {
        return new PageListResult<>(
                SuccessCode.QUERY_SUCCESS.getCode(),
                messages.text(SuccessCode.QUERY_SUCCESS),
                new PageListResult.PageBody<>(page.getTotal(), page.getRecords()));
    }
}
