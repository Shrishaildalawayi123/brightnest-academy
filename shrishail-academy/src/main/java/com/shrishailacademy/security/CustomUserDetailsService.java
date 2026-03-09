package com.shrishailacademy.security;

import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.UserRepository;
import com.shrishailacademy.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);
    private final UserRepository userRepository;

    @Value("${auth.require-email-verification:true}")
    private boolean requireEmailVerification;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Long tenantId = TenantContext.getTenantId();
        log.debug("loadUserByUsername called: email={}, tenantId={}", email, tenantId);

        if (tenantId == null) {
            log.error("Tenant context missing for authentication: email={}", email);
            throw new UsernameNotFoundException("Tenant context is missing for authentication");
        }

        User user = userRepository.findByEmailAndTenantId(email, tenantId)
                .orElseThrow(() -> {
                    log.warn("User not found in loadUserByUsername: email={}, tenantId={}", email, tenantId);
                    return new UsernameNotFoundException(
                            "User not found with email: " + email + " for tenantId=" + tenantId);
                });

        log.debug("User loaded: id={}, email={}, role={}, verified={}, locked={}, passwordHash={}",
                user.getId(), user.getEmail(), user.getRole(), user.isEmailVerified(),
                isCurrentlyLocked(user),
                user.getPassword() != null ? user.getPassword().substring(0, 10) + "..." : "null");

        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));

        boolean enabled = !requireEmailVerification || user.isEmailVerified();

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                enabled,
                true,
                true,
                !isCurrentlyLocked(user),
                authorities);
    }

    private boolean isCurrentlyLocked(User user) {
        return user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now());
    }
}
