package com.shrishailacademy.service;

import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * Refresh Token Service - Handles refresh token rotation.
 * 
 * Flow:
 * 1. On login: generate refresh token, store in DB, return to client
 * 2. On /api/auth/refresh: validate refresh token, rotate it, issue new access
 * + refresh tokens
 * 3. On logout: clear refresh token from DB
 * 
 * Security: Each refresh token is single-use (rotated on every refresh call).
 * If a stolen token is used after rotation, the legitimate user's token will be
 * invalid,
 * signaling a potential breach.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${jwt.refresh.expiration:604800000}")
    private long refreshTokenExpirationMs; // Default: 7 days

    private final UserRepository userRepository;

    public RefreshTokenService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Generate and store a new refresh token for the user.
     */
    @Transactional
    public String createRefreshToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        String token = generateSecureToken();
        user.setRefreshToken(token);
        user.setRefreshTokenExpiry(LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000));
        userRepository.save(user);

        log.debug("Refresh token created for userId={}", userId);
        return token;
    }

    /**
     * Validate and rotate the refresh token. Returns the user if valid.
     * Implements token rotation: old token is invalidated, new token is issued.
     */
    @Transactional
    public Optional<User> rotateRefreshToken(String oldToken) {
        Optional<User> userOpt = userRepository.findByRefreshToken(oldToken);

        if (userOpt.isEmpty()) {
            log.warn("Refresh token not found — possible token reuse attack");
            return Optional.empty();
        }

        User user = userOpt.get();

        // Check expiry
        if (user.getRefreshTokenExpiry() == null || user.getRefreshTokenExpiry().isBefore(LocalDateTime.now())) {
            log.warn("Expired refresh token used for userId={}", user.getId());
            // Clear the expired token
            user.setRefreshToken(null);
            user.setRefreshTokenExpiry(null);
            userRepository.save(user);
            return Optional.empty();
        }

        // Rotate: invalidate old token, issue new one
        String newToken = generateSecureToken();
        user.setRefreshToken(newToken);
        user.setRefreshTokenExpiry(LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000));
        userRepository.save(user);

        log.debug("Refresh token rotated for userId={}", user.getId());
        return Optional.of(user);
    }

    /**
     * Invalidate refresh token on logout.
     */
    @Transactional
    public void revokeRefreshToken(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setRefreshToken(null);
            user.setRefreshTokenExpiry(null);
            userRepository.save(user);
            log.debug("Refresh token revoked for email={}", email);
        });
    }

    /**
     * Generate a cryptographically secure random token.
     */
    private String generateSecureToken() {
        byte[] bytes = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
