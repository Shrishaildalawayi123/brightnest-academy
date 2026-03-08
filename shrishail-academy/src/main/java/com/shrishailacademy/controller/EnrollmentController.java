package com.shrishailacademy.controller;

import com.shrishailacademy.dto.ApiResponse;
import com.shrishailacademy.dto.response.EnrollmentResponse;
import com.shrishailacademy.model.Enrollment;
import com.shrishailacademy.model.User;
import com.shrishailacademy.service.EnrollmentService;
import com.shrishailacademy.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/api/enrollments", "/api/v1/enrollments"})
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final UserService userService;

    public EnrollmentController(EnrollmentService enrollmentService, UserService userService) {
        this.enrollmentService = enrollmentService;
        this.userService = userService;
    }

    @PostMapping("/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse> enrollInCourse(@PathVariable Long courseId, Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        Enrollment enrollment = enrollmentService.enrollStudent(user.getId(), courseId);
        return ResponseEntity
                .ok(ApiResponse.success("Enrolled successfully", EnrollmentResponse.fromEntity(enrollment)));
    }

    @GetMapping("/my-courses")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<EnrollmentResponse>> getMyEnrollments(Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        List<EnrollmentResponse> enrollments = enrollmentService.getStudentEnrollments(user.getId()).stream()
                .map(EnrollmentResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(enrollments);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<EnrollmentResponse>> getAllEnrollments(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        Page<EnrollmentResponse> enrollments = enrollmentService.getAllEnrollments(pageable)
                .map(EnrollmentResponse::fromEntity);
        return ResponseEntity.ok(enrollments);
    }

    @DeleteMapping("/{enrollmentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STUDENT')")
    public ResponseEntity<ApiResponse> cancelEnrollment(@PathVariable Long enrollmentId,
            Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        enrollmentService.cancelEnrollment(enrollmentId, user.getId(), user.getRole().name());
        return ResponseEntity.ok(ApiResponse.success("Enrollment cancelled"));
    }
}



