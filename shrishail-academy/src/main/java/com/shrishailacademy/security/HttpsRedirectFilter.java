package com.shrishailacademy.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Redirects HTTP → HTTPS in production.
 * Render/Cloudflare sets X-Forwarded-Proto header.
 * Only active when HTTPS_REQUIRED=true.
 */
@Component
@Order(1)
public class HttpsRedirectFilter extends OncePerRequestFilter {

    @Value("${https.required:false}")
    private boolean httpsRequired;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (httpsRequired && !isSecure(request)) {
            String url = "https://" + request.getServerName() + request.getRequestURI();
            String query = request.getQueryString();
            if (query != null) {
                url += "?" + query;
            }
            response.sendRedirect(url);
            return;
        }

        // Add HSTS header for secure connections
        if (httpsRequired && isSecure(request)) {
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }

        filterChain.doFilter(request, response);
    }

    private boolean isSecure(HttpServletRequest request) {
        return request.isSecure()
                || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"))
                || "https".equalsIgnoreCase(request.getScheme());
    }
}
