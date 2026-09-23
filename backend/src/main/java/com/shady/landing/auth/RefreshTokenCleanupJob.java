package com.shady.landing.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Deletes refresh tokens that expired more than a day ago. Idempotent, safe on several instances. */
@Component
public class RefreshTokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupJob.class);

    private final RefreshTokenService refreshTokens;
    private final Clock clock;

    public RefreshTokenCleanupJob(RefreshTokenService refreshTokens, Clock clock) {
        this.refreshTokens = refreshTokens;
        this.clock = clock;
    }

    @Scheduled(cron = "${app.jobs.refresh-token-cleanup-cron:0 17 3 * * *}", zone = "UTC")
    public void run() {
        int deleted = refreshTokens.deleteExpired(Instant.now(clock).minus(Duration.ofDays(1)));
        if (deleted > 0) {
            log.info("Deleted {} expired refresh tokens", deleted);
        }
    }
}
