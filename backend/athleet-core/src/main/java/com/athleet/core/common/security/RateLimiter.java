package com.athleet.core.common.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed-window counters held in memory. Good for a single instance; move to a shared store
 * (Postgres or Redis) before running more than one backend instance.
 */
@Component
public class RateLimiter {

    private final Cache<String, AtomicInteger> perMinute = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1)).maximumSize(100_000).build();
    private final Cache<String, AtomicInteger> perQuarterHour = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(15)).maximumSize(100_000).build();

    /** Counts a hit; returns false once more than {@code limit} hits happened in the last minute. */
    public boolean tryPerMinute(String key, int limit) {
        return perMinute.get(key, k -> new AtomicInteger()).incrementAndGet() <= limit;
    }

    /** Failure counter for lockouts (15-minute window). */
    public int recordFailure(String key) {
        return perQuarterHour.get(key, k -> new AtomicInteger()).incrementAndGet();
    }

    public int failures(String key) {
        AtomicInteger c = perQuarterHour.getIfPresent(key);
        return c == null ? 0 : c.get();
    }

    public void reset(String key) {
        perQuarterHour.invalidate(key);
    }
}
