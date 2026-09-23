package com.shady.landing.common.cache;

import java.time.Duration;
import java.util.Map;

public final class CacheNames {

    /** Per-admin "credentials changed at" used to reject access tokens issued before a password change. */
    public static final String ADMIN_CREDENTIALS = "admin-credentials";

    /** Time-to-live per cache; caches not listed use {@link #DEFAULT_TTL}. */
    static final Map<String, Duration> TTL = Map.of(
            ADMIN_CREDENTIALS, Duration.ofMinutes(1));

    static final Duration DEFAULT_TTL = Duration.ofMinutes(10);
    static final long MAX_ENTRIES_PER_CACHE = 5_000;

    private CacheNames() {
    }
}
