package com.shrishailacademy.security.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;

import java.time.Duration;
import java.util.Objects;

public record RateLimitPolicy(long capacity, Duration window) {

    public RateLimitPolicy {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be > 0");
        }
        Objects.requireNonNull(window, "window");
        if (window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("window must be > 0");
        }
    }

    public BucketConfiguration toBucketConfiguration() {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(capacity, window));
        return BucketConfiguration.builder().addLimit(limit).build();
    }
}
