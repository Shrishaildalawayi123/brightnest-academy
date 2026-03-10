package com.shrishailacademy.chaos;

import com.shrishailacademy.security.ratelimit.FallbackRateLimiterBackend;
import com.shrishailacademy.security.ratelimit.RateLimitPolicy;
import com.shrishailacademy.security.ratelimit.RateLimitResult;
import com.shrishailacademy.security.ratelimit.RateLimiterBackend;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CHAOS TEST 8 — Redis Failure Resilience
 *
 * Tests that when the primary rate-limiter backend (Redis) fails, the system
 * degrades gracefully to in-memory rate limiting via
 * FallbackRateLimiterBackend.
 * In test profile, inmemory is already the backend, so we unit-test the
 * fallback
 * logic directly with a simulated failing primary.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RedisFailureResilienceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RateLimiterBackend rateLimiterBackend;

    @Test
    void fallbackShouldHandlePrimaryFailureGracefully() {
        // Create a primary that always throws (simulating Redis down)
        RateLimiterBackend failingPrimary = (key, policy) -> {
            throw new RuntimeException("Redis connection refused");
        };

        // Create a working in-memory fallback
        RateLimiterBackend inMemoryFallback = (key, policy) -> new RateLimitResult(true, Duration.ZERO);

        FallbackRateLimiterBackend fallbackBackend = new FallbackRateLimiterBackend(failingPrimary, inMemoryFallback,
                Duration.ofSeconds(10));

        // First call — primary fails, fallback kicks in
        RateLimitResult result = fallbackBackend.tryConsume("test:key",
                new RateLimitPolicy(100, Duration.ofMinutes(1)));
        assertThat(result.allowed()).isTrue();
    }

    @Test
    void fallbackShouldUseFallbackDuringCooldownPeriod() {
        // Track which backend was called
        java.util.concurrent.atomic.AtomicInteger primaryCalls = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger fallbackCalls = new java.util.concurrent.atomic.AtomicInteger(0);

        RateLimiterBackend failingPrimary = (key, policy) -> {
            primaryCalls.incrementAndGet();
            throw new RuntimeException("Redis timeout");
        };

        RateLimiterBackend trackingFallback = (key, policy) -> {
            fallbackCalls.incrementAndGet();
            return new RateLimitResult(true, Duration.ZERO);
        };

        FallbackRateLimiterBackend fallbackBackend = new FallbackRateLimiterBackend(failingPrimary, trackingFallback,
                Duration.ofSeconds(10));

        RateLimitPolicy policy = new RateLimitPolicy(100, Duration.ofMinutes(1));

        // First call triggers primary failure
        fallbackBackend.tryConsume("k1", policy);
        assertThat(primaryCalls.get()).isEqualTo(1);
        assertThat(fallbackCalls.get()).isEqualTo(1);

        // Second call should go straight to fallback (cooldown active)
        fallbackBackend.tryConsume("k2", policy);
        assertThat(primaryCalls.get()).isEqualTo(1); // primary NOT called again
        assertThat(fallbackCalls.get()).isEqualTo(2);
    }

    @Test
    void rateLimiterBeanShouldBeAvailable() {
        // In test profile, should resolve to InMemoryBucket4jRateLimiterBackend
        assertThat(rateLimiterBackend).isNotNull();

        RateLimitResult result = rateLimiterBackend.tryConsume("chaos:test",
                new RateLimitPolicy(100, Duration.ofMinutes(1)));
        assertThat(result.allowed()).isTrue();
    }

    @Test
    void applicationShouldServeRequestsWhenUsingInMemoryBackend() throws Exception {
        // Even without Redis, the application should function normally
        mockMvc.perform(get("/api/courses").header("X-Tenant-ID", "default"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void fallbackShouldPreserveRateLimitEnforcement() {
        RateLimiterBackend failingPrimary = (key, policy) -> {
            throw new RuntimeException("Redis down");
        };

        java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(0);
        RateLimiterBackend countingFallback = (key, policy) -> {
            int count = counter.incrementAndGet();
            boolean allowed = count <= policy.capacity();
            return new RateLimitResult(allowed,
                    allowed ? Duration.ZERO : Duration.ofSeconds(60));
        };

        FallbackRateLimiterBackend fallbackBackend = new FallbackRateLimiterBackend(failingPrimary, countingFallback,
                Duration.ofSeconds(10));

        RateLimitPolicy policy = new RateLimitPolicy(3, Duration.ofMinutes(1));

        assertThat(fallbackBackend.tryConsume("k", policy).allowed()).isTrue();
        assertThat(fallbackBackend.tryConsume("k", policy).allowed()).isTrue();
        assertThat(fallbackBackend.tryConsume("k", policy).allowed()).isTrue();
        // 4th call exceeds limit
        assertThat(fallbackBackend.tryConsume("k", policy).allowed()).isFalse();
    }
}
