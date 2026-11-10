package com.rxs.library.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rxs.library.i18n.LocalizedResultFactory;

/** 控制器基类，构造器注入应用服务和本地化结果工厂。 */
public abstract class AbstractController<T> {
    private final T appService;
    private final LocalizedResultFactory results;

    protected AbstractController(T appService, LocalizedResultFactory results) {
        this.appService = appService;
        this.results = results;
    }

    protected <R> Result<R> success(R body) {
        return results.success(body);
    }

    protected <R> PageListResult<R> page(IPage<R> page) {
        return results.page(page);
    }

    protected T getAppService() {
        return appService;
    }
}
