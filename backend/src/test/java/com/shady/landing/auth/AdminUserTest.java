package com.shady.landing.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AdminUserTest {

    private static final Instant NOW = Instant.parse("2026-09-22T10:00:00Z");

    @Test
    void locksAfterMaxFailedAttemptsForConfiguredDuration() {
        AdminUser user = new AdminUser("a@b.co", "hash", NOW);
        for (int i = 0; i < 4; i++) {
            assertThat(user.registerFailedLogin(5, Duration.ofMinutes(15), NOW)).isFalse();
        }
        assertThat(user.registerFailedLogin(5, Duration.ofMinutes(15), NOW)).isTrue();
        assertThat(user.isLocked(NOW.plusSeconds(60))).isTrue();
        assertThat(user.isLocked(NOW.plus(Duration.ofMinutes(15)))).isFalse();
        assertThat(user.getFailedLoginAttempts()).isZero();
    }

    @Test
    void passwordChangeBumpsCredentialsVersionAndClearsLock() {
        AdminUser user = new AdminUser("a@b.co", "hash", NOW);
        long before = user.credentialsVersion();
        user.registerFailedLogin(1, Duration.ofMinutes(15), NOW);

        user.changePassword("new-hash", NOW.plusMillis(5));

        assertThat(user.credentialsVersion()).isNotEqualTo(before);
        assertThat(user.hasFailedLoginState()).isFalse();
    }
}
