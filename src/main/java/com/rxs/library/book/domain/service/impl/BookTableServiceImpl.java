package com.rxs.library.book.domain.service.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rxs.library.book.domain.entity.BookTPO;
import com.rxs.library.book.domain.service.IBookTableService;
import com.rxs.library.book.infrastructure.mapper.IBookTableMapper;

/** 书目表 MyBatis-Plus 数据操作实现。 */
@Service
public class BookTableServiceImpl extends ServiceImpl<IBookTableMapper, BookTPO>
        implements IBookTableService {
    @Override
    public IPage<BookTPO> queryPage(int page, int size, String title) {
        // ! 是显式 LIKE 转义字符，搜索字符串始终通过参数绑定。
        String pattern = "%" + title.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        return page(
                new Page<BookTPO>(page, size),
                Wrappers.<BookTPO>lambdaQuery()
                        .apply("title LIKE {0} ESCAPE '!'", pattern)
                        .orderByAsc(BookTPO::getId));
    }

    @Override
    public int decrement(Long id) {
        return baseMapper.decrement(id);
    }

    @Override
    public int increment(Long id) {
        return baseMapper.increment(id);
    }
}
