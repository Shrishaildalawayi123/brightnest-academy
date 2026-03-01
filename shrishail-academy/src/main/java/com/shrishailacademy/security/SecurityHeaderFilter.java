package com.shrishailacademy.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Adds additional security headers to every response.
 * Supplements the headers configured in SecurityConfig.
 */
@Component
@Order(2)
public class SecurityHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Prevent caching of API responses (sensitive data)
        if (request.getRequestURI().startsWith("/api/")) {
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);
        }

        // Remove server identification headers
        response.setHeader("Server", "");
        response.setHeader("X-Powered-By", "");

        // Cross-Origin isolation headers
        response.setHeader("Cross-Origin-Opener-Policy", "same-origin");
        response.setHeader("Cross-Origin-Resource-Policy", "same-origin");

        filterChain.doFilter(request, response);
    }
}
