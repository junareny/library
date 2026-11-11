package com.rxs.library.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import jakarta.validation.Validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.bucket4j.TimeMeter;

/** 使用可控单调时钟验证补充、并发、身份隔离和桶容量边界。 */
class RateLimitServiceTest {
    @Test
    @DisplayName("令牌耗尽后拒绝，按连续补充时间恢复并向上取整重试秒数")
    void refillsWithoutSleeping() {
        var time = new TestTimeMeter();
        var limiter = new RateLimitService(new RateLimitProperties(), time);
        var rule = new RateLimitProperties.Rule(2, Duration.ofSeconds(1));
        assertThat(limiter.tryAcquire("login", "client", rule).allowed()).isTrue();
        assertThat(limiter.tryAcquire("login", "client", rule).allowed()).isTrue();
        assertThat(limiter.tryAcquire("login", "client", rule))
                .isEqualTo(new RateLimitService.Decision(false, 1));
        time.now = Duration.ofMillis(499).toNanos();
        assertThat(limiter.tryAcquire("login", "client", rule).allowed()).isFalse();
        time.now = Duration.ofMillis(500).toNanos();
        assertThat(limiter.tryAcquire("login", "client", rule).allowed()).isTrue();
    }

    @Test
    @DisplayName("不同身份与不同业务范围的额度互相隔离")
    void isolatesIdentitiesAndScopes() {
        var limiter = new RateLimitService(new RateLimitProperties(), new TestTimeMeter());
        var rule = new RateLimitProperties.Rule(1, Duration.ofMinutes(1));
        assertThat(limiter.tryAcquire("login", "first", rule).allowed()).isTrue();
        assertThat(limiter.tryAcquire("login", "first", rule).allowed()).isFalse();
        assertThat(limiter.tryAcquire("login", "second", rule).allowed()).isTrue();
        assertThat(limiter.tryAcquire("register", "first", rule).allowed()).isTrue();
    }

    @Test
    @DisplayName("达到内存桶上限时不淘汰活跃额度，空闲期满后可回收")
    void boundsMemoryWithoutResettingActiveBuckets() {
        var properties = new RateLimitProperties();
        properties.setMaxBuckets(1);
        var time = new TestTimeMeter();
        var limiter = new RateLimitService(properties, time);
        var rule = new RateLimitProperties.Rule(1, Duration.ofSeconds(2));
        assertThat(limiter.tryAcquire("login", "first", rule).allowed()).isTrue();
        assertThat(limiter.tryAcquire("login", "second", rule).allowed()).isFalse();
        assertThat(limiter.tryAcquire("login", "first", rule).allowed()).isFalse();
        time.now = Duration.ofSeconds(2).toNanos();
        assertThat(limiter.tryAcquire("login", "second", rule).allowed()).isTrue();
        assertThat(limiter.tryAcquire("login", "third", rule).allowed()).isFalse();
    }

    @Test
    @DisplayName("关闭限流时不消耗额度")
    void canDisableRateLimiting() {
        var properties = new RateLimitProperties();
        properties.setEnabled(false);
        var limiter = new RateLimitService(properties, new TestTimeMeter());
        var rule = new RateLimitProperties.Rule(1, Duration.ofMinutes(1));
        for (int i = 0; i < 10; i++) {
            assertThat(limiter.tryAcquire("login", "client", rule).allowed()).isTrue();
        }
        properties.setEnabled(true);
        assertThat(limiter.tryAcquire("login", "client", rule).allowed()).isTrue();
        assertThat(limiter.tryAcquire("login", "client", rule).allowed()).isFalse();
    }

    @Test
    @DisplayName("同一身份并发请求不会超过令牌容量")
    void concurrentRequestsRespectCapacity() throws Exception {
        var limiter = new RateLimitService(new RateLimitProperties(), new TestTimeMeter());
        var rule = new RateLimitProperties.Rule(10, Duration.ofMinutes(1));
        List<Callable<Boolean>> actions = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            actions.add(() -> limiter.tryAcquire("loan", "user", rule).allowed());
        }
        int allowed = 0;
        try (var pool = Executors.newFixedThreadPool(8)) {
            for (var result : pool.invokeAll(actions)) {
                if (result.get()) {
                    allowed++;
                }
            }
        }
        assertThat(allowed).isEqualTo(10);
    }

    @Test
    @DisplayName("配置校验拒绝非法容量、周期和内存上限")
    void validatesConfiguration() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(new RateLimitProperties())).isEmpty();
            var properties = new RateLimitProperties();
            properties.setMaxBuckets(0);
            properties.setLogin(new RateLimitProperties.Rule(0, Duration.ZERO));
            properties.setRegister(new RateLimitProperties.Rule(1, Duration.ofDays(2)));
            assertThat(validator.validate(properties)).hasSize(4);
        }
    }

    private static final class TestTimeMeter implements TimeMeter {
        private long now;

        @Override
        public long currentTimeNanos() {
            return now;
        }

        @Override
        public boolean isWallClockBased() {
            return false;
        }
    }
}
