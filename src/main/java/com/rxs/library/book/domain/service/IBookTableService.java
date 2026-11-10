package com.rxs.library.book.domain.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.rxs.library.book.domain.entity.BookTPO;

/** 书目表数据操作服务。 */
public interface IBookTableService extends IService<BookTPO> {
    IPage<BookTPO> queryPage(int page, int size, String title);

    int decrement(Long id);

    int increment(Long id);
}
