package com.shrishailacademy.security.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class FallbackRateLimiterBackend implements RateLimiterBackend {

    private static final Logger log = LoggerFactory.getLogger(FallbackRateLimiterBackend.class);

    private final RateLimiterBackend primary;
    private final RateLimiterBackend fallback;
    private final Duration primaryCooldown;

    private final AtomicReference<Instant> primaryDisabledUntil = new AtomicReference<>(Instant.EPOCH);

    public FallbackRateLimiterBackend(RateLimiterBackend primary, RateLimiterBackend fallback,
            Duration primaryCooldown) {
        this.primary = Objects.requireNonNull(primary, "primary");
        this.fallback = Objects.requireNonNull(fallback, "fallback");
        this.primaryCooldown = primaryCooldown == null ? Duration.ofSeconds(5) : primaryCooldown;
    }

    @Override
    public RateLimitResult tryConsume(String key, RateLimitPolicy policy) {
        Instant now = Instant.now();
        if (now.isBefore(primaryDisabledUntil.get())) {
            return fallback.tryConsume(key, policy);
        }
        try {
            return primary.tryConsume(key, policy);
        } catch (Exception ex) {
            primaryDisabledUntil.set(now.plus(primaryCooldown));
            log.warn("Primary rate limiter failed, falling back for {}s: {}", primaryCooldown.toSeconds(),
                    ex.toString());
            return fallback.tryConsume(key, policy);
        }
    }
}
