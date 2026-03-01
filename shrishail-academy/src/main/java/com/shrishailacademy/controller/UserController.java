package com.shrishailacademy.controller;

import com.shrishailacademy.dto.response.UserResponse;
import com.shrishailacademy.model.User;
import com.shrishailacademy.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
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
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers().stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }
}
