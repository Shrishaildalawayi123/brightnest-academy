package com.shrishailacademy.controller;

import com.shrishailacademy.dto.ApiResponse;
import com.shrishailacademy.model.User;
import com.shrishailacademy.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userService.getUserByEmail(email);
            return ResponseEntity.ok(toSafeDto(user));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/students")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllStudents() {
        List<Map<String, Object>> students = userService.getAllStudents().stream()
                .map(this::toSafeDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(students);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers() {
        List<Map<String, Object>> users = userService.getAllUsers().stream()
                .map(this::toSafeDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    /**
     * Safe DTO projection — never exposes password or mutates JPA entity
     */
    private Map<String, Object> toSafeDto(User user) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", user.getId());
        dto.put("name", user.getName());
        dto.put("email", user.getEmail());
        dto.put("phone", user.getPhone());
        dto.put("role", user.getRole() != null ? user.getRole().name() : null);
        dto.put("createdAt", user.getCreatedAt());
        dto.put("updatedAt", user.getUpdatedAt());
        return dto;
    }
}
