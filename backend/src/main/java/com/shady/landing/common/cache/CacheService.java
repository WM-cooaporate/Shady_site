package com.shady.landing.common.cache;

import java.util.function.Supplier;

/**
 * Application cache abstraction. The default implementation is in-memory (Caffeine); it can be swapped for
 * Redis without touching callers.
 */
public interface CacheService {

    /** Returns the cached value or computes, caches and returns it. {@code null} results are not cached. */
    <T> T get(String cacheName, Object key, Supplier<T> loader);

    void evict(String cacheName, Object key);

    void evictAll(String cacheName);
}
