package com.shrishailacademy.security;

import com.shrishailacademy.security.ratelimit.RateLimitPolicy;
import com.shrishailacademy.security.ratelimit.RateLimitResult;
import com.shrishailacademy.security.ratelimit.RateLimiterBackend;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.time.Duration;

/**
 * Rate Limiting Filter — protects login and registration endpoints
 * against brute-force and credential-stuffing attacks.
 *
 * Uses Bucket4j.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    @Value("${rate.limit.login.max:5}")
    private int loginMaxAttempts;

    @Value("${rate.limit.login.window-seconds:60}")
    private int loginWindowSeconds;

    @Value("${rate.limit.api.max:100}")
    private int apiMaxRequests;

    @Value("${rate.limit.api.window-seconds:60}")
    private int apiWindowSeconds;

    @Value("${rate.limit.trust-forward-headers:false}")
    private boolean trustForwardHeaders;

    private final RateLimiterBackend rateLimiter;

    public RateLimitFilter(@Qualifier("rateLimiterBackend") RateLimiterBackend rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        if (isApiRateLimitedPath(path)) {
            String clientIp = getClientIp(request);
            String key = "api:" + clientIp;
            RateLimitPolicy policy = new RateLimitPolicy(apiMaxRequests, Duration.ofSeconds(apiWindowSeconds));
            RateLimitResult decision = rateLimiter.tryConsume(key, policy);

            if (!decision.allowed()) {
                log.warn("API rate limit exceeded for {} on {} {}", clientIp, method, path);
                writeTooManyRequests(response, decision.retryAfterSecondsCeil(),
                        "Too many requests. Please try again shortly.");
                return;
            }
        }

        // Stricter brute-force protection for login endpoint.
        if ("POST".equalsIgnoreCase(method) && path.endsWith("/api/auth/login")) {

            String clientIp = getClientIp(request);
            String key = "login:" + clientIp;
            RateLimitPolicy policy = new RateLimitPolicy(loginMaxAttempts, Duration.ofSeconds(loginWindowSeconds));
            RateLimitResult decision = rateLimiter.tryConsume(key, policy);

            if (!decision.allowed()) {
                log.warn("Login rate limit exceeded for {}", clientIp);
                writeTooManyRequests(response, decision.retryAfterSecondsCeil(),
                        "Too many login attempts. Please wait before trying again.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!trustForwardHeaders || !isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }

        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp;
        }
        return remoteAddr;
    }

    private boolean isTrustedProxy(String remoteAddr) {
        if (remoteAddr == null || remoteAddr.isBlank()) {
            return false;
        }
        return remoteAddr.equals("127.0.0.1")
                || remoteAddr.equals("0:0:0:0:0:0:0:1")
                || remoteAddr.startsWith("10.")
                || remoteAddr.startsWith("192.168.")
                || remoteAddr.startsWith("172.16.")
                || remoteAddr.startsWith("172.17.")
                || remoteAddr.startsWith("172.18.")
                || remoteAddr.startsWith("172.19.")
                || remoteAddr.startsWith("172.2")
                || remoteAddr.startsWith("172.30.")
                || remoteAddr.startsWith("172.31.");
    }

    private boolean isApiRateLimitedPath(String path) {
        if (path == null || !path.startsWith("/api/")) {
            return false;
        }
        return !path.startsWith("/api/actuator/")
                && !path.startsWith("/api/auth/")
                && !"/api/health".equals(path);
    }

    private void writeTooManyRequests(HttpServletResponse response, long retryAfterSeconds, String message)
            throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        String body = "{"
                + "\"timestamp\":\"" + Instant.now().toString() + "\","
                + "\"status\":429,"
                + "\"error\":\"Too Many Requests\","
                + "\"message\":\"" + message + "\""
                + "}";
        response.getWriter().write(body);
    }

}
