package com.shrishailacademy.security.ratelimit;

import java.time.Duration;

public record RateLimitResult(boolean allowed, Duration retryAfter) {
    public static RateLimitResult permit() {
        return new RateLimitResult(true, Duration.ZERO);
    }

    public static RateLimitResult deny(Duration retryAfter) {
        if (retryAfter == null || retryAfter.isNegative()) {
            retryAfter = Duration.ZERO;
        }
        return new RateLimitResult(false, retryAfter);
    }

    public long retryAfterSecondsCeil() {
        if (allowed) {
            return 0;
        }
        if (retryAfter == null || retryAfter.isZero() || retryAfter.isNegative()) {
            return 1;
        }
        long seconds = retryAfter.getSeconds();
        if (retryAfter.getNano() > 0) {
            seconds += 1;
        }
        return Math.max(1, seconds);
    }
}
