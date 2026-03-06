package com.shrishailacademy.security.ratelimit;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryBucket4jRateLimiterBackend implements RateLimiterBackend {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    public RateLimitResult tryConsume(String key, RateLimitPolicy policy) {
        String scopedKey = key + ":" + policy.capacity() + ":" + policy.window().toSeconds();
        Bucket bucket = buckets.computeIfAbsent(scopedKey, k -> {
            Bandwidth limit = Bandwidth.classic(policy.capacity(),
                    Refill.intervally(policy.capacity(), policy.window()));
            return Bucket.builder().addLimit(limit).build();
        });

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (probe.isConsumed()) {
            return RateLimitResult.permit();
        }
        return RateLimitResult.deny(Duration.ofNanos(probe.getNanosToWaitForRefill()));
    }
}
