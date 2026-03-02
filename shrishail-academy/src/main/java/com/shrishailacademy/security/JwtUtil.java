package com.shrishailacademy.security;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * JWT utility wrapper with the method signatures commonly used in tutorials.
 *
 * This delegates to the existing {@link JwtTokenProvider} implementation.
 */
public class JwtUtil {

    private final JwtTokenProvider tokenProvider;

    public JwtUtil(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    public String generateToken(UserDetails userDetails) {
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority())
                .orElse("ROLE_STUDENT");
        return tokenProvider.generateTokenFromUsername(userDetails.getUsername(), role);
    }

    public String extractUsername(String token) {
        return tokenProvider.getUsernameFromToken(token);
    }

    public boolean validateToken(String token) {
        return tokenProvider.validateToken(token);
    }
}
