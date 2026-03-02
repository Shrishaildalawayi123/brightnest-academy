package com.shrishailacademy.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Authentication entrypoint for unauthorized requests.
 *
 * Returns JSON for API requests, and redirects for HTML browser requests.
 */
@Component
public class AuthEntryPointJwt implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {

        String accept = request.getHeader("Accept");
        boolean wantsHtml = request.getRequestURI().endsWith(".html")
                || (accept != null && accept.contains("text/html"));

        if (wantsHtml) {
            response.sendRedirect("/login.html");
            return;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        String body = "{"
                + "\"timestamp\":\"" + Instant.now().toString() + "\","
                + "\"status\":" + HttpServletResponse.SC_UNAUTHORIZED + ","
                + "\"error\":\"Unauthorized\","
                + "\"message\":\"Unauthorized - please login\""
                + "}";
        response.getWriter().write(body);
    }
}
