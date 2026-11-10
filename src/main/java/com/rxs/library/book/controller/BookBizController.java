package com.rxs.library.book.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rxs.library.book.application.service.BookAppService;
import com.rxs.library.book.domain.bo.BookQueryBO;
import com.rxs.library.book.domain.vo.BookVO;
import com.rxs.library.common.AbstractController;
import com.rxs.library.common.IdDTO;
import com.rxs.library.common.PageListResult;
import com.rxs.library.common.ParamPageDTO;
import com.rxs.library.common.Result;
import com.rxs.library.i18n.LocalizedResultFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** 公开书目查询业务接口。 */
@RestController
@RequestMapping("/IBookBiz")
@Tag(name = "书目查询")
public class BookBizController extends AbstractController<BookAppService> {
    public BookBizController(BookAppService service, LocalizedResultFactory results) {
        super(service, results);
    }

    @PostMapping("/queryBook/page")
    @Operation(description = "分页查询书目")
    public PageListResult<BookVO> queryBookPage(
            @Valid @RequestBody ParamPageDTO<BookQueryBO> param) {
        String title =
                param.getDto() == null || param.getDto().title() == null
                        ? ""
                        : param.getDto().title();
        return page(
                getAppService().queryBookPage(param.getPageCurrent(), param.getPageSize(), title));
    }

    @PostMapping("/queryBook")
    @Operation(description = "查询书目详情")
    public Result<BookVO> queryBook(@Valid @RequestBody IdDTO param) {
        return success(getAppService().queryBook(param.id()));
    }
}
