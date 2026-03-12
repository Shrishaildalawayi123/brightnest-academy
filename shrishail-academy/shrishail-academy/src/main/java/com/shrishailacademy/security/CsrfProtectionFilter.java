package com.shrishailacademy.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;

/**
 * Double-submit cookie CSRF protection for cookie-authenticated requests.
 */
@Component
public class CsrfProtectionFilter extends OncePerRequestFilter {

    public static final String AUTH_COOKIE_NAME = "AUTH_TOKEN";
    public static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    public static final String CSRF_HEADER_NAME = "X-CSRF-Token";

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod();
        String path = request.getRequestURI();

        if (SAFE_METHODS.contains(method) || isCsrfExempt(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Enforce CSRF only for cookie-authenticated requests.
        String authCookie = getCookieValue(request, AUTH_COOKIE_NAME);
        if (!StringUtils.hasText(authCookie)) {
            filterChain.doFilter(request, response);
            return;
        }

        String csrfCookie = getCookieValue(request, CSRF_COOKIE_NAME);
        String csrfHeader = request.getHeader(CSRF_HEADER_NAME);

        if (!StringUtils.hasText(csrfCookie) || !csrfCookie.equals(csrfHeader)) {
            response.setStatus(403);
            response.setContentType("application/json");
            String body = "{"
                    + "\"timestamp\":\"" + Instant.now().toString() + "\","
                    + "\"status\":403,"
                    + "\"error\":\"Forbidden\","
                    + "\"message\":\"Invalid CSRF token\""
                    + "}";
            response.getWriter().write(body);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isCsrfExempt(String path) {
        return path.equals("/api/auth/login")
                || path.equals("/api/auth/register")
                || path.equals("/api/auth/refresh")
                || path.equals("/api/auth/logout");
    }

    private String getCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
