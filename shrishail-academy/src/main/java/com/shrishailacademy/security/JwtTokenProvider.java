package com.shrishailacademy.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.shrishailacademy.tenant.TenantContext;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT Token Utility
 * Handles JWT token generation, validation, and extraction.
 * HS512 requires a key of at least 64 bytes (512 bits).
 */
@Component
public class JwtTokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final int MIN_SECRET_LENGTH = 64;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * Generate JWT token from user authentication
     */
    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority())
                .orElse("ROLE_STUDENT");
        Long tenantId = TenantContext.getTenantId();
        return generateTokenFromUsername(userDetails.getUsername(), role, tenantId);
    }

    /**
     * Generate JWT token from username (defaults to ROLE_STUDENT).
     */
    public String generateTokenFromUsername(String username) {
        return generateTokenFromUsername(username, "ROLE_STUDENT");
    }

    /**
     * Generate JWT token with role claim.
     */
    public String generateTokenFromUsername(String username, String role) {
        return generateTokenFromUsername(username, role, null);
    }

    /**
     * Generate JWT token with role + tenantId claim.
     */
    public String generateTokenFromUsername(String username, String role, Long tenantId) {
        validateSecret();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        JwtBuilder builder = Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuer("brightnest-academy")
                .setAudience("brightnest-api")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS512);

        if (tenantId != null) {
            builder.claim("tenantId", tenantId);
        }

        return builder.compact();
    }

    /**
     * Get username from JWT token
     */
    public String getUsernameFromToken(String token) {
        validateSecret();
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .requireIssuer("brightnest-academy")
                .requireAudience("brightnest-api")
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }

    /**
     * Extract tenantId from JWT token (claim: tenantId).
     */
    public Long getTenantIdFromToken(String token) {
        validateSecret();
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .requireIssuer("brightnest-academy")
                .requireAudience("brightnest-api")
                .build()
                .parseClaimsJws(token)
                .getBody();

        Object tenantId = claims.get("tenantId");
        if (tenantId instanceof Number n) {
            return n.longValue();
        }
        return null;
    }

    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        try {
            validateSecret();
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));

            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .requireIssuer("brightnest-academy")
                    .requireAudience("brightnest-api")
                    .build()
                    .parseClaimsJws(token);

            return true;
        } catch (io.jsonwebtoken.security.SignatureException ex) {
            logger.error("Invalid JWT signature");
        } catch (SecurityException ex) {
            logger.error("Invalid JWT signature");
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token");
        } catch (ExpiredJwtException ex) {
            logger.error("Expired JWT token");
        } catch (UnsupportedJwtException ex) {
            logger.error("Unsupported JWT token");
        } catch (JwtException ex) {
            logger.error("JWT validation error: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty");
        }
        return false;
    }

    /**
     * Validates that the JWT secret meets the minimum length for HS512 (64 bytes /
     * 512 bits).
     */
    private void validateSecret() {
        if (jwtSecret == null || jwtSecret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "JWT secret must be at least " + MIN_SECRET_LENGTH + " characters for HS512. Current length: "
                            + (jwtSecret == null ? 0 : jwtSecret.length()));
        }
    }
}
