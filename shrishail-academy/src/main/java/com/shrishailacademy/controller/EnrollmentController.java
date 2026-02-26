package com.shrishailacademy.controller;

import com.shrishailacademy.dto.ApiResponse;
import com.shrishailacademy.model.Enrollment;
import com.shrishailacademy.model.User;
import com.shrishailacademy.service.EnrollmentService;
import com.shrishailacademy.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private UserService userService;

    @PostMapping("/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> enrollInCourse(@PathVariable Long courseId, Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userService.getUserByEmail(email);
            Enrollment enrollment = enrollmentService.enrollStudent(user.getId(), courseId);
            return ResponseEntity.ok(ApiResponse.success("Enrolled successfully", enrollment));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/my-courses")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getMyEnrollments(Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userService.getUserByEmail(email);
            List<Enrollment> enrollments = enrollmentService.getStudentEnrollments(user.getId());
            return ResponseEntity.ok(enrollments);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Enrollment>> getAllEnrollments() {
        return ResponseEntity.ok(enrollmentService.getAllEnrollments());
    }

    @DeleteMapping("/{enrollmentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STUDENT')")
    public ResponseEntity<?> cancelEnrollment(@PathVariable Long enrollmentId, Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userService.getUserByEmail(email);
            enrollmentService.cancelEnrollment(enrollmentId, user.getId(), user.getRole().name());
            return ResponseEntity.ok(ApiResponse.success("Enrollment cancelled"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
