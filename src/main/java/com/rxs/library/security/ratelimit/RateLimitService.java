package com.rxs.library.security.ratelimit;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.TimeMeter;

/** 使用单调时钟维护有界令牌桶，避免时钟回拨及活跃桶被淘汰后额度重置。 */
@Service
public class RateLimitService {
    private static final long NANOS_PER_SECOND = Duration.ofSeconds(1).toNanos();
    private final RateLimitProperties properties;
    private final TimeMeter timeMeter;
    private final Map<Key, Entry> buckets = new HashMap<>();
    private long lastCleanup;

    @Autowired
    public RateLimitService(RateLimitProperties properties) {
        this(properties, TimeMeter.SYSTEM_NANOTIME);
    }

    RateLimitService(RateLimitProperties properties, TimeMeter timeMeter) {
        this.properties = properties;
        this.timeMeter = timeMeter;
        this.lastCleanup = timeMeter.currentTimeNanos();
    }

    /** 获取一个请求令牌；桶数量达到上限时拒绝新身份，不挤出已有身份。 */
    public synchronized Decision tryAcquire(
            String scope, String identity, RateLimitProperties.Rule rule) {
        if (!properties.isEnabled()) {
            return new Decision(true, 0);
        }
        long now = timeMeter.currentTimeNanos();
        if (now - lastCleanup >= NANOS_PER_SECOND) {
            buckets.values().removeIf(entry -> now - entry.lastAccess >= entry.idleNanos);
            lastCleanup = now;
        }
        Key key = new Key(scope, identity);
        Entry entry = buckets.get(key);
        if (entry == null) {
            if (buckets.size() >= properties.getMaxBuckets()) {
                return new Decision(false, 1);
            }
            Bucket bucket =
                    Bucket.builder()
                            .withCustomTimePrecision(timeMeter)
                            .addLimit(
                                    limit ->
                                            limit.capacity(rule.capacity())
                                                    .refillGreedy(rule.capacity(), rule.period()))
                            .build();
            entry = new Entry(bucket, rule.period().toNanos(), now);
            buckets.put(key, entry);
        }
        entry.lastAccess = now;
        ConsumptionProbe probe = entry.bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return new Decision(true, 0);
        }
        long waitNanos = probe.getNanosToWaitForRefill();
        long waitSeconds =
                waitNanos / NANOS_PER_SECOND + (waitNanos % NANOS_PER_SECOND == 0 ? 0 : 1);
        return new Decision(false, Math.max(1, waitSeconds));
    }

    /** 重试时间以整秒向上取整，可直接写入 Retry-After 响应头。 */
    public record Decision(boolean allowed, long retryAfterSeconds) {}

    private record Key(String scope, String identity) {}

    private static final class Entry {
        private final Bucket bucket;
        private final long idleNanos;
        private long lastAccess;

        private Entry(Bucket bucket, long idleNanos, long lastAccess) {
            this.bucket = bucket;
            this.idleNanos = idleNanos;
            this.lastAccess = lastAccess;
        }
    }
}
