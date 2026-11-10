package com.rxs.library.book.infrastructure.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rxs.library.book.domain.entity.BookTPO;

/** 书目表持久化接口，自定义 SQL 见同名 XML。 */
@Mapper
public interface IBookTableMapper extends BaseMapper<BookTPO> {
    int decrement(@Param("id") Long id);

    int increment(@Param("id") Long id);
}
