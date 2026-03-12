package com.shrishailacademy.service;

import com.shrishailacademy.dto.UserCreateRequest;
import com.shrishailacademy.dto.UserUpdateRequest;
import com.shrishailacademy.exception.DuplicateResourceException;
import com.shrishailacademy.exception.ResourceNotFoundException;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.UserRepository;
import com.shrishailacademy.tenant.TenantContext;
import com.shrishailacademy.util.InputSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantService tenantService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, TenantService tenantService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantService = tenantService;
    }

    public User getUserById(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        return userRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    public User getUserByEmail(String email) {
        Long tenantId = TenantContext.requireTenantId();
        return userRepository.findByEmailAndTenantId(email, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    public List<User> getAllStudents() {
        Long tenantId = TenantContext.requireTenantId();
        return userRepository.findByRoleAndTenantId(User.Role.STUDENT, tenantId);
    }

    public List<User> getFacultyUsers() {
        Long tenantId = TenantContext.requireTenantId();
        return userRepository.findByRoleInAndTenantId(Set.of(User.Role.TEACHER, User.Role.ADMIN), tenantId).stream()
                .sorted(Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<User> getAllUsers() {
        Long tenantId = TenantContext.requireTenantId();
        return userRepository.findAllByTenantId(tenantId);
    }

    @Transactional
    public User createUser(UserCreateRequest request) {
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
        user.setRole(request.getRole());

        User saved = userRepository.save(user);
        log.info("User created by admin: id={} email={} role={}", saved.getId(), saved.getEmail(), saved.getRole());
        return saved;
    }

    @Transactional
    public User updateUser(Long id, UserUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        User user = userRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (request.getName() != null) {
            user.setName(InputSanitizer.sanitizeAndTruncate(request.getName(), 100));
        }

        if (request.getEmail() != null) {
            String newEmail = InputSanitizer.sanitizeEmailAndTruncate(request.getEmail(), 100);
            if (!newEmail.equals(user.getEmail())
                    && userRepository.existsByEmailAndTenantIdAndIdNot(newEmail, tenantId, id)) {
                throw new DuplicateResourceException("User", "email", newEmail);
            }
            user.setEmail(newEmail);
        }

        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getPhone() != null) {
            user.setPhone(InputSanitizer.sanitizeAndTruncateNullable(request.getPhone(), 20));
        }

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        User updated = userRepository.save(user);
        log.info("User updated by admin: id={} email={} role={}", updated.getId(), updated.getEmail(),
                updated.getRole());
        return updated;
    }

    @Transactional
    public void deleteUser(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        User user = userRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        userRepository.delete(user);
        log.info("User deleted by admin: id={} email={}", id, user.getEmail());
    }
}
