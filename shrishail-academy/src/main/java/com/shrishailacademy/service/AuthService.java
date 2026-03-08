package com.shrishailacademy.service;

import com.shrishailacademy.dto.AuthResponse;
import com.shrishailacademy.dto.LoginRequest;
import com.shrishailacademy.dto.RegisterRequest;
import com.shrishailacademy.exception.BusinessException;
import com.shrishailacademy.exception.DuplicateResourceException;
import com.shrishailacademy.exception.ResourceNotFoundException;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.UserRepository;
import com.shrishailacademy.security.JwtTokenProvider;
import com.shrishailacademy.tenant.TenantContext;
import com.shrishailacademy.util.InputSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final TenantService tenantService;

    @Value("${auth.lockout.max-attempts:5}")
    private int maxFailedAttempts;

    @Value("${auth.lockout.duration-minutes:30}")
    private long lockoutDurationMinutes;

    @Value("${auth.verification.expiry-hours:24}")
    private long verificationExpiryHours;

    public AuthService(UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenProvider tokenProvider,
            TenantService tenantService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.tenantService = tenantService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        String email = InputSanitizer.sanitizeEmailAndTruncate(request.getEmail(), 100);
        if (userRepository.existsByEmailAndTenantId(email, tenantId)) {
            throw new DuplicateResourceException("User", "email", email);
        }

        User user = new User();
        user.setTenant(tenantService.requireCurrentTenant());
        user.setName(InputSanitizer.sanitizeAndTruncate(request.getName(), 100));
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(InputSanitizer.sanitizeAndTruncateNullable(request.getPhone(), 20));
        user.setRole(User.Role.STUDENT);
        user.setEmailVerified(false);
        user.setEmailVerificationToken(generateVerificationToken());
        user.setEmailVerificationTokenExpiry(LocalDateTime.now().plusHours(verificationExpiryHours));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);

        user = userRepository.save(user);
        log.info("User registered: email={} role={} verificationRequired=true", user.getEmail(), user.getRole());

        return new AuthResponse(null, user.getId(), user.getName(), user.getEmail(), "ROLE_" + user.getRole().name());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        String email = InputSanitizer.sanitizeEmailAndTruncate(request.getEmail(), 100);

        User user = userRepository.findByEmailAndTenantId(email, tenantId)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));

        if (isCurrentlyLocked(user)) {
            throw new BusinessException("ACCOUNT_LOCKED",
                    "Account is temporarily locked. Please try again later.");
        }

        if (!user.isEmailVerified()) {
            throw new BusinessException("EMAIL_NOT_VERIFIED",
                    "Please verify your email before logging in.");
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword()));
        } catch (BadCredentialsException ex) {
            recordFailedLogin(user);
            throw ex;
        }

        clearFailedLoginState(user);

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = tokenProvider.generateToken(authentication);

        log.info("User logged in: email={}", user.getEmail());
        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail(), "ROLE_" + user.getRole().name());
    }

    @Transactional
    public void verifyEmail(String token) {
        Long tenantId = TenantContext.requireTenantId();
        String safeToken = InputSanitizer.sanitizeAndTruncate(token, 128);
        User user = userRepository.findByEmailVerificationTokenAndTenantId(safeToken, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Email verification token", "token", "invalid"));

        if (user.getEmailVerificationTokenExpiry() == null
                || user.getEmailVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BusinessException("VERIFICATION_TOKEN_EXPIRED", "Verification token has expired.");
        }

        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationTokenExpiry(null);
        userRepository.save(user);
        log.info("Email verified successfully for user={}", user.getEmail());
    }

    private boolean isCurrentlyLocked(User user) {
        return user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now());
    }

    private void recordFailedLogin(User user) {
        Integer attempts = user.getFailedLoginAttempts();
        int failed = attempts != null ? attempts : 0;
        failed += 1;
        user.setFailedLoginAttempts(failed);
        if (failed >= maxFailedAttempts) {
            user.setLockedUntil(LocalDateTime.now().plusMinutes(lockoutDurationMinutes));
            user.setFailedLoginAttempts(0);
        }
        userRepository.save(user);
    }

    private void clearFailedLoginState(User user) {
        if ((user.getFailedLoginAttempts() != null && user.getFailedLoginAttempts() > 0)
                || user.getLockedUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }
    }

    private String generateVerificationToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
