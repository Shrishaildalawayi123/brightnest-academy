package com.shrishailacademy.security.ratelimit;

public interface RateLimiterBackend {
    RateLimitResult tryConsume(String key, RateLimitPolicy policy);
}
