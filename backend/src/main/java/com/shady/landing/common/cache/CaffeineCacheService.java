package com.shady.landing.common.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class CaffeineCacheService implements CacheService {

    private final ConcurrentMap<String, Cache<Object, Object>> caches = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String cacheName, Object key, Supplier<T> loader) {
        return (T) cache(cacheName).get(key, k -> loader.get());
    }

    @Override
    public void evict(String cacheName, Object key) {
        cache(cacheName).invalidate(key);
    }

    @Override
    public void evictAll(String cacheName) {
        cache(cacheName).invalidateAll();
    }

    private Cache<Object, Object> cache(String name) {
        return caches.computeIfAbsent(name, n -> Caffeine.newBuilder()
                .expireAfterWrite(CacheNames.TTL.getOrDefault(n, CacheNames.DEFAULT_TTL))
                .maximumSize(CacheNames.MAX_ENTRIES_PER_CACHE)
                .build());
    }
}
