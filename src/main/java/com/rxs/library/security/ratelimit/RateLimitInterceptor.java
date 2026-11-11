package com.rxs.library.security.ratelimit;

import java.io.IOException;
import java.security.Principal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rxs.library.common.Result;
import com.rxs.library.exception.LibraryErrorCode;
import com.rxs.library.i18n.LocalizedMessages;

import lombok.RequiredArgsConstructor;

/** 在认证之后、业务执行之前限流，按实际路由匹配，避免上下文路径影响规则。 */
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {
    private final RateLimitProperties properties;
    private final RateLimitService limiter;
    private final LocalizedMessages messages;
    private final ObjectMapper mapper;

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (!properties.isEnabled() || !"POST".equals(request.getMethod())) {
            return true;
        }
        Object route = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        RateLimitService.Decision decision;
        if ("/IAuthBiz/login".equals(route)) {
            decision = limiter.tryAcquire("login", request.getRemoteAddr(), properties.getLogin());
        } else if ("/IAuthBiz/register".equals(route)) {
            decision = limiter.tryAcquire("register", request.getRemoteAddr(), properties.getRegister());
        } else if ("/ILoanBiz/borrowBook".equals(route) || "/ILoanBiz/returnBook".equals(route)) {
            Principal principal = request.getUserPrincipal();
            if (principal == null) {
                return true;
            }
            // 借阅和归还共享用户额度，身份仅来自安全过滤链验证后的主体。
            decision = limiter.tryAcquire("loan", principal.getName(), properties.getLoan());
        } else {
            return true;
        }
        if (decision.allowed()) {
            return true;
        }
        LibraryErrorCode error = LibraryErrorCode.TOO_MANY_REQUESTS;
        response.setStatus(error.getStatus().value());
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(decision.retryAfterSeconds()));
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getOutputStream(), Result.failure(error, messages.text(error)));
        return false;
    }
}
