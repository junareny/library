package com.rxs.library.security.ratelimit;

import java.time.Duration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.hibernate.validator.constraints.time.DurationMax;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import lombok.Data;

/** 单实例令牌桶配置，容量表示允许的初始突发量及每周期补充量。 */
@Data
@Validated
@ConfigurationProperties("app.rate-limit")
public class RateLimitProperties {
    private boolean enabled = true;

    @Min(1)
    @Max(1000000)
    private int maxBuckets = 10000;

    @Valid @NotNull private Rule login = new Rule(20, Duration.ofMinutes(1));
    @Valid @NotNull private Rule register = new Rule(5, Duration.ofMinutes(1));
    @Valid @NotNull private Rule loan = new Rule(10, Duration.ofMinutes(1));

    /** 使用连续补充策略，空闲一个完整周期后可安全回收令牌桶。 */
    public record Rule(
            @Min(1) @Max(1000000) long capacity,
            @NotNull @DurationMin(millis = 1) @DurationMax(days = 1) Duration period) {}
}
