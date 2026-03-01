package com.shrishailacademy.security;

import com.shrishailacademy.service.VisitorAnalyticsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Visitor Tracking Filter - Records page visits for analytics.
 * Only tracks public HTML page requests (not API calls, assets, or actuator).
 * Assigns a session cookie for unique visitor counting.
 */
@Component
@Order(1)
public class VisitorTrackingFilter extends OncePerRequestFilter {

    private static final String SESSION_COOKIE = "BN_SESSION";
    private static final Set<String> EXCLUDED_PREFIXES = Set.of(
            "/api/", "/actuator/", "/css/", "/js/", "/images/", "/fonts/", "/health");

    private final VisitorAnalyticsService analyticsService;

    public VisitorTrackingFilter(VisitorAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        // Only track HTML page visits, not API/assets
        if (shouldTrack(uri, request)) {
            String sessionId = getOrCreateSessionId(request, response);
            String referrer = request.getHeader("Referer");
            String ipAddress = extractClientIp(request);
            String userAgent = request.getHeader("User-Agent");

            analyticsService.recordVisit(sessionId, uri, referrer, ipAddress, userAgent);
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldTrack(String uri, HttpServletRequest request) {
        // Skip non-GET requests
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }

        // Skip excluded prefixes (API, assets, actuator)
        for (String prefix : EXCLUDED_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return false;
            }
        }

        // Track HTML pages and root
        return uri.equals("/") || uri.endsWith(".html");
    }

    private String getOrCreateSessionId(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (SESSION_COOKIE.equals(cookie.getName()) && cookie.getValue() != null) {
                    return cookie.getValue();
                }
            }
        }

        // Create new session ID
        String sessionId = UUID.randomUUID().toString();
        Cookie sessionCookie = new Cookie(SESSION_COOKIE, sessionId);
        sessionCookie.setPath("/");
        sessionCookie.setMaxAge(30 * 24 * 60 * 60); // 30 days
        sessionCookie.setHttpOnly(true);
        response.addCookie(sessionCookie);
        return sessionId;
    }

    private String extractClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
