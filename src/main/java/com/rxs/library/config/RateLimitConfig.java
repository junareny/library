package com.rxs.library.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.rxs.library.security.ratelimit.RateLimitInterceptor;
import com.rxs.library.security.ratelimit.RateLimitProperties;

import lombok.RequiredArgsConstructor;

/** 将接口限流纳入现有 MVC 请求链，保留安全过滤器及国际化处理顺序。 */
@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
@RequiredArgsConstructor
public class RateLimitConfig implements WebMvcConfigurer {
    private final RateLimitInterceptor interceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor);
    }
}
