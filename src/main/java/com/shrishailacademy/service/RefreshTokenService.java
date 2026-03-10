package com.shrishailacademy.service;

import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.UserRepository;
import com.shrishailacademy.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Refresh token service with rotation and at-rest token hashing.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${jwt.refresh.expiration:604800000}")
    private long refreshTokenExpirationMs;

    @Value("${jwt.refresh.pepper:}")
    private String refreshTokenPepper;

    private final UserRepository userRepository;

    public RefreshTokenService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public String createRefreshToken(Long userId) {
        Long tenantId = TenantContext.requireTenantId();
        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        String rawToken = generateSecureToken();
        user.setRefreshToken(hashToken(rawToken));
        user.setRefreshTokenExpiry(LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000));
        userRepository.save(user);

        log.debug("Refresh token created for userId={}", userId);
        return rawToken;
    }

    @Transactional
    public Optional<RefreshTokenRotation> rotateRefreshToken(String oldToken) {
        Long tenantId = TenantContext.requireTenantId();
        Optional<User> userOpt = userRepository.findByRefreshTokenAndTenantId(hashToken(oldToken), tenantId);

        if (userOpt.isEmpty()) {
            log.warn("Refresh token not found, possible token reuse");
            return Optional.empty();
        }

        User user = userOpt.get();
        if (user.getRefreshTokenExpiry() == null || user.getRefreshTokenExpiry().isBefore(LocalDateTime.now())) {
            log.warn("Expired refresh token used for userId={}", user.getId());
            user.setRefreshToken(null);
            user.setRefreshTokenExpiry(null);
            userRepository.save(user);
            return Optional.empty();
        }

        String newRawToken = generateSecureToken();
        user.setRefreshToken(hashToken(newRawToken));
        user.setRefreshTokenExpiry(LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000));
        userRepository.save(user);

        log.debug("Refresh token rotated for userId={}", user.getId());
        return Optional.of(new RefreshTokenRotation(user, newRawToken));
    }

    @Transactional
    public void revokeRefreshToken(String email) {
        Long tenantId = TenantContext.requireTenantId();
        userRepository.findByEmailAndTenantId(email, tenantId).ifPresent(user -> {
            user.setRefreshToken(null);
            user.setRefreshTokenExpiry(null);
            userRepository.save(user);
            log.debug("Refresh token revoked for email={}", email);
        });
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return null;
        }
        String value = rawToken + (refreshTokenPepper == null ? "" : refreshTokenPepper);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", ex);
        }
    }

    public record RefreshTokenRotation(User user, String rawRefreshToken) {
    }
}
