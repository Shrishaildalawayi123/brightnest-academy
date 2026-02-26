package com.shrishailacademy.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate Limiting Filter — protects login and registration endpoints
 * against brute-force and credential-stuffing attacks.
 *
 * Uses in-memory sliding window per IP.
 * For production, consider Redis-based rate limiting.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    @Value("${rate.limit.login.max:5}")
    private int maxAttempts;

    @Value("${rate.limit.login.window-seconds:60}")
    private int windowSeconds;

    private final Map<String, RateLimitEntry> attempts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Only rate-limit POST to auth endpoints
        if ("POST".equalsIgnoreCase(method) &&
                (path.endsWith("/api/auth/login") || path.endsWith("/api/auth/register"))) {

            String clientIp = getClientIp(request);
            String key = clientIp + ":" + path;

            RateLimitEntry entry = attempts.compute(key, (k, existing) -> {
                long now = System.currentTimeMillis();
                if (existing == null || (now - existing.windowStart) > windowSeconds * 1000L) {
                    return new RateLimitEntry(now, new AtomicInteger(1));
                }
                existing.count.incrementAndGet();
                return existing;
            });

            if (entry.count.get() > maxAttempts) {
                log.warn("Rate limit exceeded for {} on {}", clientIp, path);
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"success\":false,\"message\":\"Too many requests. Please wait " +
                                windowSeconds + " seconds before trying again.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }

    private static class RateLimitEntry {
        final long windowStart;
        final AtomicInteger count;

        RateLimitEntry(long windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
