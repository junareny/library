package com.rxs.library.i18n;

import java.io.IOException;
import java.util.Locale;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** 在安全过滤器之前设置语言，使认证和权限错误也使用同一套消息。 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLanguageFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        LocaleContext previous = LocaleContextHolder.getLocaleContext();
        Locale locale = LocalizedMessages.resolve(request);
        try {
            LocaleContextHolder.setLocale(locale);
            response.setHeader("Content-Language", locale.toLanguageTag());
            response.addHeader("Vary", "Accept-Language");
            chain.doFilter(request, response);
        } finally {
            LocaleContextHolder.setLocaleContext(previous);
        }
    }
}
