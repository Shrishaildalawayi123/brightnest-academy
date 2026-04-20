package com.shrishailacademy.controller;

import com.shrishailacademy.dto.ApiResponse;
import com.shrishailacademy.dto.RoleChangeRequest;
import com.shrishailacademy.dto.UserCreateRequest;
import com.shrishailacademy.dto.UserUpdateRequest;
import com.shrishailacademy.dto.response.UserResponse;
import com.shrishailacademy.model.User;
import com.shrishailacademy.service.AuditLogService;
import com.shrishailacademy.service.UserService;
import com.shrishailacademy.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/api/users", "/api/v1/users"})
public class UserController {

    private final UserService userService;
    private final AuditLogService auditLogService;

    public UserController(UserService userService, AuditLogService auditLogService) {
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }

    @GetMapping("/students")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllStudents() {
        List<UserResponse> students = userService.getAllStudents().stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(students);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> getAllUsers(Pageable pageable) {
        Page<UserResponse> users = userService.getAllUsers(pageable)
                .map(UserResponse::fromEntity);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/faculty")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getFacultyUsers() {
        List<UserResponse> users = userService.getFacultyUsers().stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        User created = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created successfully", UserResponse.fromEntity(created)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> updateUser(@PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        User updated = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", UserResponse.fromEntity(updated)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully"));
    }

    @PatchMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> changeUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody RoleChangeRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        try {
            String requestedRole = request.role() == null ? "" : request.role().trim().toUpperCase();
            User.Role newRole;
            try {
                newRole = User.Role.valueOf(requestedRole);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid role. Must be ADMIN, TEACHER, or STUDENT"));
            }

            User updated = userService.changeUserRole(userId, newRole);
            Long adminId = userService.getUserByEmail(authentication.getName()).getId();
            Long tenantId = TenantContext.requireTenantId();
            auditLogService.logEvent(
                    tenantId,
                    adminId,
                    "USER_ROLE_CHANGED",
                    String.format("User %s (ID: %d) role changed to %s", updated.getEmail(), userId, newRole),
                    httpRequest);

            return ResponseEntity.ok(ApiResponse.success("User role updated successfully", UserResponse.fromEntity(updated)));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}



