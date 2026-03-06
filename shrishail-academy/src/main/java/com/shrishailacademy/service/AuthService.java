package com.shrishailacademy.service;

import com.shrishailacademy.dto.AuthResponse;
import com.shrishailacademy.dto.LoginRequest;
import com.shrishailacademy.dto.RegisterRequest;
import com.shrishailacademy.exception.DuplicateResourceException;
import com.shrishailacademy.exception.ResourceNotFoundException;
import com.shrishailacademy.model.User;
import com.shrishailacademy.service.TenantService;
import com.shrishailacademy.repository.UserRepository;
import com.shrishailacademy.security.JwtTokenProvider;
import com.shrishailacademy.tenant.TenantContext;
import com.shrishailacademy.util.InputSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final TenantService tenantService;

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

        user = userRepository.save(user);
        log.info("User registered: email={} role={}", user.getEmail(), user.getRole());

        String token = tokenProvider.generateTokenFromUsername(user.getEmail(), "ROLE_STUDENT", tenantId);

        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail(), "ROLE_" + user.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        String email = InputSanitizer.sanitizeEmailAndTruncate(request.getEmail(), 100);
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = tokenProvider.generateToken(authentication);

        User user = userRepository.findByEmailAndTenantId(email, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        log.info("User logged in: email={}", user.getEmail());
        return new AuthResponse(token, user.getId(), user.getName(), user.getEmail(), "ROLE_" + user.getRole().name());
    }
}
