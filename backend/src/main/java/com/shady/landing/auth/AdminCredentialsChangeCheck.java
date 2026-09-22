package com.shady.landing.auth;

import com.shady.landing.common.cache.CacheNames;
import com.shady.landing.common.cache.CacheService;
import com.shady.landing.security.CredentialsChangeCheck;
import java.time.Instant;
import org.springframework.stereotype.Component;

/** Access tokens issued before the last password change (or for a deleted admin) are rejected. */
@Component
public class AdminCredentialsChangeCheck implements CredentialsChangeCheck {

    private final AdminUserRepository users;
    private final CacheService cache;

    public AdminCredentialsChangeCheck(AdminUserRepository users, CacheService cache) {
        this.users = users;
        this.cache = cache;
    }

    @Override
    public boolean isTokenStillValid(long adminId, long credentialsVersion) {
        Instant changedAt = cache.get(CacheNames.ADMIN_CREDENTIALS, adminId,
                () -> users.findPasswordChangedAt(adminId).orElse(null));
        return changedAt != null && AdminUser.credentialsVersion(changedAt) == credentialsVersion;
    }

    public void evict(long adminId) {
        cache.evict(CacheNames.ADMIN_CREDENTIALS, adminId);
    }
}
