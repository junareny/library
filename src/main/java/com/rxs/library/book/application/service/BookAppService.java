package com.rxs.library.book.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rxs.library.book.domain.entity.BookTPO;
import com.rxs.library.book.domain.service.IBookTableService;
import com.rxs.library.book.domain.vo.BookVO;
import com.rxs.library.common.ApiException;

import lombok.RequiredArgsConstructor;

/** 书目查询应用服务。 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookAppService {
    private final IBookTableService books;

    public IPage<BookVO> queryBookPage(int page, int size, String title) {
        return books.queryPage(page, size, title).convert(BookVO::from);
    }

    public BookVO queryBook(Long id) {
        BookTPO book = books.getById(id);
        if (book == null) {
            throw ApiException.notFound();
        }
        return BookVO.from(book);
    }
}
