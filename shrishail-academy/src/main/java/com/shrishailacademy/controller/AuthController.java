package com.shrishailacademy.controller;

import com.shrishailacademy.dto.ApiErrorResponse;
import com.shrishailacademy.dto.AuthResponse;
import com.shrishailacademy.dto.LoginRequest;
import com.shrishailacademy.dto.RegisterRequest;
import com.shrishailacademy.model.User;
import com.shrishailacademy.security.JwtTokenProvider;
import com.shrishailacademy.service.AuditLogService;
import com.shrishailacademy.service.AuthService;
import com.shrishailacademy.service.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final AuditLogService auditLogService;
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${jwt.refresh.expiration:604800000}")
    private long refreshTokenExpirationMs;

    @Value("${security.cookie.secure:false}")
    private boolean secureCookie;

    @Value("${security.cookie.same-site:Lax}")
    private String sameSitePolicy;

    public AuthController(AuthService authService,
            AuditLogService auditLogService,
            RefreshTokenService refreshTokenService,
            JwtTokenProvider jwtTokenProvider) {
        this.authService = authService;
        this.auditLogService = auditLogService;
        this.refreshTokenService = refreshTokenService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        try {
            AuthResponse response = authService.register(request);
            String refreshToken = refreshTokenService.createRefreshToken(response.getId());
            setAuthCookies(httpRequest, httpResponse, response.getToken(), refreshToken);
            auditLogService.logEvent(response.getId(), "REGISTER",
                    "New user registered: " + request.getEmail(), httpRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.warn("Registration failed for email: {}", request.getEmail());
            auditLogService.logEvent(null, "REGISTER_FAILED",
                    "Registration attempt: " + request.getEmail(), httpRequest);
            throw e;
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        try {
            AuthResponse response = authService.login(request);
            String refreshToken = refreshTokenService.createRefreshToken(response.getId());
            setAuthCookies(httpRequest, httpResponse, response.getToken(), refreshToken);
            auditLogService.logEvent(response.getId(), "LOGIN_SUCCESS",
                    "User logged in: " + request.getEmail(), httpRequest);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.warn("Login failed for email: {}", request.getEmail());
            auditLogService.logEvent(null, "LOGIN_FAILED",
                    "Failed login attempt: " + request.getEmail(), httpRequest);
            throw e;
        }
    }

    /**
     * Refresh token endpoint - rotates refresh token and issues new access token.
     * Access token: 1 hour (jwt.expiration), Refresh token: 7 days
     * (jwt.refresh.expiration).
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String oldRefreshToken = extractCookieValue(httpRequest, "REFRESH_TOKEN");

        if (oldRefreshToken == null || oldRefreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiErrorResponse(
                            Instant.now(),
                            HttpStatus.UNAUTHORIZED.value(),
                            "Unauthorized",
                            "Refresh token missing"));
        }

        Optional<User> userOpt = refreshTokenService.rotateRefreshToken(oldRefreshToken);
        if (userOpt.isEmpty()) {
            clearAuthCookies(httpRequest, httpResponse);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiErrorResponse(
                            Instant.now(),
                            HttpStatus.UNAUTHORIZED.value(),
                            "Unauthorized",
                            "Invalid or expired refresh token. Please login again."));
        }

        User user = userOpt.get();
        String role = "ROLE_" + user.getRole().name();
        String newAccessToken = jwtTokenProvider.generateTokenFromUsername(user.getEmail(), role);
        setAuthCookies(httpRequest, httpResponse, newAccessToken, user.getRefreshToken());

        auditLogService.logEvent(user.getId(), "TOKEN_REFRESH",
                "Access token refreshed", httpRequest);

        return ResponseEntity.ok(Map.of(
                "message", "Token refreshed successfully",
                "token", newAccessToken,
                "type", "Bearer"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        // Extract email from current auth cookie to revoke refresh token
        String authToken = extractCookieValue(request, "AUTH_TOKEN");
        if (authToken != null) {
            try {
                String email = jwtTokenProvider.getUsernameFromToken(authToken);
                refreshTokenService.revokeRefreshToken(email);
            } catch (Exception e) {
                log.debug("Could not extract email from expired token during logout");
            }
        }
        clearAuthCookies(request, response);
        auditLogService.logEvent(null, "LOGOUT", "User logged out", request);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    private void setAuthCookies(HttpServletRequest request, HttpServletResponse response,
            String jwtToken, String refreshToken) {
        boolean isSecure = isSecureRequest(request);
        long accessMaxAge = Math.max(1, jwtExpirationMs / 1000);
        long refreshMaxAge = Math.max(1, refreshTokenExpirationMs / 1000);
        String csrfToken = UUID.randomUUID().toString();

        String sameSite = normalizeSameSitePolicy(sameSitePolicy);

        ResponseCookie authCookie = ResponseCookie.from("AUTH_TOKEN", jwtToken)
                .httpOnly(true)
                .secure(isSecure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(accessMaxAge)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("REFRESH_TOKEN", refreshToken)
                .httpOnly(true)
                .secure(isSecure)
                .sameSite(sameSite)
                .path("/api/auth/refresh")
                .maxAge(refreshMaxAge)
                .build();

        ResponseCookie csrfCookie = ResponseCookie.from("XSRF-TOKEN", csrfToken)
                .httpOnly(false)
                .secure(isSecure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(accessMaxAge)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, authCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, csrfCookie.toString());
    }

    private void clearAuthCookies(HttpServletRequest request, HttpServletResponse response) {
        boolean isSecure = isSecureRequest(request);

        String sameSite = normalizeSameSitePolicy(sameSitePolicy);

        ResponseCookie authCookie = ResponseCookie.from("AUTH_TOKEN", "")
                .httpOnly(true)
                .secure(isSecure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(0)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("REFRESH_TOKEN", "")
                .httpOnly(true)
                .secure(isSecure)
                .sameSite(sameSite)
                .path("/api/auth/refresh")
                .maxAge(0)
                .build();

        ResponseCookie csrfCookie = ResponseCookie.from("XSRF-TOKEN", "")
                .httpOnly(false)
                .secure(isSecure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, authCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, csrfCookie.toString());
    }

    private String extractCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private boolean isSecureRequest(HttpServletRequest request) {
        return secureCookie
                || request.isSecure()
                || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    }

    private String normalizeSameSitePolicy(String value) {
        if (value == null || value.isBlank()) {
            return "Lax";
        }
        String normalized = value.trim();
        if ("Lax".equalsIgnoreCase(normalized))
            return "Lax";
        if ("Strict".equalsIgnoreCase(normalized))
            return "Strict";
        if ("None".equalsIgnoreCase(normalized))
            return "None";
        return "Lax";
    }
}
