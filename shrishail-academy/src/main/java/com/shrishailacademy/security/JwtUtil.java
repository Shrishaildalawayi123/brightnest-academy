package com.shrishailacademy.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * JWT utility using jjwt 0.11.5.
 *
 * Secret key is injected from environment variable JWT_SECRET (with
 *
 *
 * `jwt.secret` as a fallback).
 * Token expiration is fixed to 1 hour.
 */
@Component
public class JwtUtil {

    private static final int MIN_SECRET_LENGTH = 64;
    private static final Duration TOKEN_TTL = Duration.ofHours(1);
    private static final String ISSUER = "brightnest-academy";
    private static final String AUDIENCE = "brightnest-api";

    private final String jwtSecret;

    public JwtUtil(@Value("${JWT_SECRET:${jwt.secret}}") String jwtSecret) {
        validateSecret(jwtSecret);
        this.jwtSecret = jwtSecret;
    }

    /**
     * Generates a signed JWT for the given username with a 1 hour expiry.
     */
    public String generateToken(String username) {
        Instant now = Instant.now();
        Instant expiry = now.plus(TOKEN_TTL);

        return Jwts.builder()
                .setSubject(username)
                .setIssuer(ISSUER)
                .setAudience(AUDIENCE)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Validates token signature and expiry.
     */
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ignored) {
            return false;
        }
    }

    /**
     * Extracts the username (subject) from the token.
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .requireIssuer(ISSUER)
                .requireAudience(AUDIENCE)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    private static void validateSecret(String secret) {
        if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "JWT secret must be at least " + MIN_SECRET_LENGTH + " characters for HS512. Current length: "
                            + (secret == null ? 0 : secret.length()));
        }
    }
}
