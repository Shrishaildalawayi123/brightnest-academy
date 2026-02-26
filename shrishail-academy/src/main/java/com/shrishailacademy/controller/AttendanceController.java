package com.shrishailacademy.controller;

import com.shrishailacademy.dto.ApiResponse;
import com.shrishailacademy.dto.AttendanceRequest;
import com.shrishailacademy.model.Attendance;
import com.shrishailacademy.model.User;
import com.shrishailacademy.service.AttendanceService;
import com.shrishailacademy.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private UserService userService;

    /**
     * Admin marks attendance for multiple students in a course
     * POST /api/attendance/mark
     */
    @PostMapping("/mark")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> markAttendance(@Valid @RequestBody AttendanceRequest request,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            User admin = userService.getUserByEmail(email);
            List<Attendance> records = attendanceService.markAttendance(request, admin.getId());
            return ResponseEntity
                    .ok(ApiResponse.success("Attendance marked for " + records.size() + " students", records));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get attendance for a course on a specific date (Admin)
     * GET /api/attendance/course/{courseId}?date=2026-02-24
     */
    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getCourseAttendance(
            @PathVariable Long courseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            List<Attendance> records;
            if (date != null) {
                records = attendanceService.getCourseAttendanceByDate(courseId, date);
            } else {
                records = attendanceService.getCourseAttendance(courseId);
            }
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Student views their own attendance
     * GET /api/attendance/my-attendance
     */
    @GetMapping("/my-attendance")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getMyAttendance(Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userService.getUserByEmail(email);
            List<Attendance> records = attendanceService.getStudentAttendance(user.getId());
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Student views attendance for a specific course
     * GET /api/attendance/my-attendance/{courseId}
     */
    @GetMapping("/my-attendance/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getMyAttendanceForCourse(
            @PathVariable Long courseId, Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userService.getUserByEmail(email);
            List<Attendance> records = attendanceService.getStudentCourseAttendance(user.getId(), courseId);
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get attendance summary (present/absent/late/%) for a student-course combo
     * GET /api/attendance/summary?userId=1&courseId=2
     */
    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STUDENT')")
    public ResponseEntity<?> getAttendanceSummary(
            @RequestParam(required = false) Long userId,
            @RequestParam Long courseId,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            User currentUser = userService.getUserByEmail(email);

            // Students can only view their own summary
            if (!currentUser.isAdmin()) {
                userId = currentUser.getId();
            }
            if (userId == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("User ID is required"));
            }

            Map<String, Object> summary = attendanceService.getAttendanceSummary(userId, courseId);
            return ResponseEntity.ok(ApiResponse.success("Attendance summary", summary));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get all attendance records (Admin)
     * GET /api/attendance
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Attendance>> getAllAttendance() {
        return ResponseEntity.ok(attendanceService.getAllAttendance());
    }
}
