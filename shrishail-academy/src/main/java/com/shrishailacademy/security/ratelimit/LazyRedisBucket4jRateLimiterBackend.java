package com.shrishailacademy.security.ratelimit;

import io.lettuce.core.RedisURI;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Lazily initializes the Redis-based Bucket4j backend on first use.
 *
 * This avoids failing application startup in dev/test environments
 * where Redis is not running.
 */
public class LazyRedisBucket4jRateLimiterBackend implements RateLimiterBackend {

    private final Supplier<RedisURI> redisUriSupplier;
    private final AtomicReference<LettuceRedisBucket4jRateLimiterBackend> delegate = new AtomicReference<>();

    public LazyRedisBucket4jRateLimiterBackend(Supplier<RedisURI> redisUriSupplier) {
        this.redisUriSupplier = Objects.requireNonNull(redisUriSupplier, "redisUriSupplier");
    }

    @Override
    public RateLimitResult tryConsume(String key, RateLimitPolicy policy) {
        return getOrCreate().tryConsume(key, policy);
    }

    public void closeIfInitialized() {
        LettuceRedisBucket4jRateLimiterBackend current = delegate.get();
        if (current != null) {
            current.close();
        }
    }

    private LettuceRedisBucket4jRateLimiterBackend getOrCreate() {
        LettuceRedisBucket4jRateLimiterBackend existing = delegate.get();
        if (existing != null) {
            return existing;
        }

        synchronized (delegate) {
            LettuceRedisBucket4jRateLimiterBackend doubleCheck = delegate.get();
            if (doubleCheck != null) {
                return doubleCheck;
            }
            LettuceRedisBucket4jRateLimiterBackend created = new LettuceRedisBucket4jRateLimiterBackend(
                    redisUriSupplier.get());
            delegate.set(created);
            return created;
        }
    }
}
