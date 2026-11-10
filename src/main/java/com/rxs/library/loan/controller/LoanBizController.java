package com.rxs.library.loan.controller;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rxs.library.common.AbstractController;
import com.rxs.library.common.IdDTO;
import com.rxs.library.common.PageListResult;
import com.rxs.library.common.ParamPageDTO;
import com.rxs.library.common.Result;
import com.rxs.library.i18n.LocalizedResultFactory;
import com.rxs.library.loan.application.service.LoanAppService;
import com.rxs.library.loan.domain.bo.BorrowBO;
import com.rxs.library.loan.domain.vo.LoanVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/** 本人借还书业务接口。 */
@RestController
@RequestMapping("/ILoanBiz")
@Tag(name = "借还管理")
@SecurityRequirement(name = "bearerAuth")
public class LoanBizController extends AbstractController<LoanAppService> {
    public LoanBizController(LoanAppService service, LocalizedResultFactory results) {
        super(service, results);
    }

    @PostMapping("/borrowBook")
    @Operation(description = "借阅图书")
    public Result<LoanVO> borrowBook(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody BorrowBO bo) {
        return success(getAppService().borrow(Long.valueOf(jwt.getSubject()), bo.bookId()));
    }

    @PostMapping("/queryCurrentLoan/page")
    @Operation(description = "分页查询本人当前借阅")
    public PageListResult<LoanVO> queryCurrentLoanPage(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ParamPageDTO<Void> param) {
        return page(
                getAppService()
                        .current(
                                Long.valueOf(jwt.getSubject()),
                                param.getPageCurrent(),
                                param.getPageSize()));
    }

    @PostMapping("/returnBook")
    @Operation(description = "归还本人图书")
    public Result<Void> returnBook(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody IdDTO param) {
        getAppService().returnLoan(Long.valueOf(jwt.getSubject()), param.id());
        return success(null);
    }
}
