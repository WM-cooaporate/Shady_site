package com.shady.landing.common;

import static org.assertj.core.api.Assertions.assertThat;

import com.shady.landing.common.config.AppProperties.RateLimitRule;
import com.shady.landing.common.ratelimit.Bucket4jRateLimiter;
import com.shady.landing.common.ratelimit.RateLimiter;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class Bucket4jRateLimiterTest {

    private final Bucket4jRateLimiter limiter = new Bucket4jRateLimiter();
    private final RateLimitRule rule = new RateLimitRule("login", "POST", "/api/v1/auth/login", 3, Duration.ofMinutes(1));

    @Test
    void allowsUpToCapacityThenRejectsWithRetryAfter() {
        for (int i = 0; i < 3; i++) {
            assertThat(limiter.tryConsume(rule, "1.1.1.1").allowed()).isTrue();
        }
        RateLimiter.Result rejected = limiter.tryConsume(rule, "1.1.1.1");
        assertThat(rejected.allowed()).isFalse();
        assertThat(rejected.retryAfterSeconds()).isBetween(1L, 60L);
    }

    @Test
    void bucketsAreIndependentPerKeyAndRule() {
        for (int i = 0; i < 3; i++) {
            limiter.tryConsume(rule, "2.2.2.2");
        }
        assertThat(limiter.tryConsume(rule, "2.2.2.2").allowed()).isFalse();
        assertThat(limiter.tryConsume(rule, "3.3.3.3").allowed()).isTrue();
        RateLimitRule other = new RateLimitRule("click", "POST", "/api/v1/public/clicks", 1, Duration.ofMinutes(1));
        assertThat(limiter.tryConsume(other, "2.2.2.2").allowed()).isTrue();
    }
}
