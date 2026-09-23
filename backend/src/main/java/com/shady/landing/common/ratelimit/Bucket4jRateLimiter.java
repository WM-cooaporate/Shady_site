package com.shady.landing.common.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.shady.landing.common.config.AppProperties.RateLimitRule;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class Bucket4jRateLimiter implements RateLimiter {

    /** Idle buckets are dropped so memory stays bounded under many distinct client IPs. */
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(1))
            .maximumSize(100_000)
            .build();

    @Override
    public Result tryConsume(RateLimitRule rule, String key) {
        Bucket bucket = buckets.get(rule.name() + ':' + key, k -> newBucket(rule));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return new Result(true, 0);
        }
        long seconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1);
        return new Result(false, seconds);
    }

    private static Bucket newBucket(RateLimitRule rule) {
        return Bucket.builder()
                .addLimit(limit -> limit.capacity(rule.capacity()).refillGreedy(rule.capacity(), rule.period()))
                .build();
    }
}
