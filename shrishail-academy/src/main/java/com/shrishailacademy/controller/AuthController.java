package com.shrishailacademy.controller;

import com.shrishailacademy.dto.ApiResponse;
import com.shrishailacademy.dto.AuthResponse;
import com.shrishailacademy.dto.LoginRequest;
import com.shrishailacademy.dto.RegisterRequest;
import com.shrishailacademy.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String SAME_SITE_POLICY = "Lax";

    @Autowired
    private AuthService authService;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${security.cookie.secure:false}")
    private boolean secureCookie;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        try {
            AuthResponse response = authService.register(request);
            setAuthCookies(httpRequest, httpResponse, response.getToken());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Generic message prevents email enumeration
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Registration failed. Please check your details and try again."));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        try {
            AuthResponse response = authService.login(request);
            setAuthCookies(httpRequest, httpResponse, response.getToken());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Generic message prevents user enumeration
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid email or password."));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        clearAuthCookies(request, response);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    private void setAuthCookies(HttpServletRequest request, HttpServletResponse response, String jwtToken) {
        boolean isSecure = isSecureRequest(request);
        long maxAgeSeconds = Math.max(1, jwtExpirationMs / 1000);
        String csrfToken = UUID.randomUUID().toString();

        ResponseCookie authCookie = ResponseCookie.from("AUTH_TOKEN", jwtToken)
                .httpOnly(true)
                .secure(isSecure)
                .sameSite(SAME_SITE_POLICY)
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();

        ResponseCookie csrfCookie = ResponseCookie.from("XSRF-TOKEN", csrfToken)
                .httpOnly(false)
                .secure(isSecure)
                .sameSite(SAME_SITE_POLICY)
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, authCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, csrfCookie.toString());
    }

    private void clearAuthCookies(HttpServletRequest request, HttpServletResponse response) {
        boolean isSecure = isSecureRequest(request);

        ResponseCookie authCookie = ResponseCookie.from("AUTH_TOKEN", "")
                .httpOnly(true)
                .secure(isSecure)
                .sameSite(SAME_SITE_POLICY)
                .path("/")
                .maxAge(0)
                .build();

        ResponseCookie csrfCookie = ResponseCookie.from("XSRF-TOKEN", "")
                .httpOnly(false)
                .secure(isSecure)
                .sameSite(SAME_SITE_POLICY)
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, authCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, csrfCookie.toString());
    }

    private boolean isSecureRequest(HttpServletRequest request) {
        return secureCookie
                || request.isSecure()
                || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    }
}
