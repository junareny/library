package com.rxs.library.auth.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rxs.library.auth.application.service.AuthAppService;
import com.rxs.library.auth.domain.bo.CredentialsBO;
import com.rxs.library.auth.domain.vo.TokenVO;
import com.rxs.library.auth.domain.vo.UserVO;
import com.rxs.library.common.AbstractController;
import com.rxs.library.common.Result;
import com.rxs.library.i18n.LocalizedResultFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/** 用户认证业务接口。 */
@RestController
@RequestMapping("/IAuthBiz")
@Tag(name = "用户认证")
public class AuthBizController extends AbstractController<AuthAppService> {
    public AuthBizController(AuthAppService service, LocalizedResultFactory results) {
        super(service, results);
    }

    @PostMapping("/register")
    @Operation(description = "注册用户")
    public Result<UserVO> register(@Valid @RequestBody CredentialsBO bo) {
        return success(getAppService().register(bo));
    }

    @PostMapping("/login")
    @Operation(description = "登录并签发令牌")
    public Result<TokenVO> login(@Valid @RequestBody CredentialsBO bo) {
        return success(getAppService().login(bo));
    }
}
