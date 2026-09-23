package com.shady.landing.auth;

import com.shady.landing.common.config.AppProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Persists failed-login counters and lockouts (they survive restarts and apply across instances). */
@Service
public class LoginAttemptService {

    private final AdminUserRepository users;
    private final AppProperties.Lockout lockout;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public LoginAttemptService(AdminUserRepository users, AppProperties properties,
                               ApplicationEventPublisher events, Clock clock) {
        this.users = users;
        this.lockout = properties.security().lockout();
        this.events = events;
        this.clock = clock;
    }

    /** Returns {@code true} if this failure locked the account. */
    @Transactional
    public boolean recordFailure(long adminId, String ipAddress) {
        AdminUser user = users.findByIdForUpdate(adminId).orElse(null);
        if (user == null) {
            return false;
        }
        Instant now = Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
        boolean lockedNow = user.registerFailedLogin(lockout.maxAttempts(), lockout.duration(), now);
        if (lockedNow) {
            events.publishEvent(new AdminAccountLockedEvent(user.getId(), user.getEmail(), user.getLockedUntil(), ipAddress));
        }
        return lockedNow;
    }

    @Transactional
    public void recordSuccess(long adminId) {
        users.findByIdForUpdate(adminId)
                .filter(AdminUser::hasFailedLoginState)
                .ifPresent(AdminUser::clearFailedLogins);
    }
}
