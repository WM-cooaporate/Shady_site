package com.shady.landing.common.ratelimit;

import com.shady.landing.common.config.AppProperties.RateLimitRule;

/** Token-bucket rate limiting; in-memory today, swappable for a distributed store (e.g. Redis) later. */
public interface RateLimiter {

    Result tryConsume(RateLimitRule rule, String key);

    record Result(boolean allowed, long retryAfterSeconds) {
    }
}
