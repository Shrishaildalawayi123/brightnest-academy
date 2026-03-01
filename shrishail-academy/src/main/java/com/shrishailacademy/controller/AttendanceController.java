package com.shrishailacademy.controller;

import com.shrishailacademy.dto.ApiResponse;
import com.shrishailacademy.dto.AttendanceRequest;
import com.shrishailacademy.dto.response.AttendanceResponse;
import com.shrishailacademy.model.Attendance;
import com.shrishailacademy.model.User;
import com.shrishailacademy.service.AttendanceService;
import com.shrishailacademy.service.UserService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final UserService userService;

    public AttendanceController(AttendanceService attendanceService, UserService userService) {
        this.attendanceService = attendanceService;
        this.userService = userService;
    }

    /**
     * Admin marks attendance for multiple students in a course
     */
    @PostMapping("/mark")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> markAttendance(@Valid @RequestBody AttendanceRequest request,
            Authentication authentication) {
        User admin = userService.getUserByEmail(authentication.getName());
        List<Attendance> records = attendanceService.markAttendance(request, admin.getId());
        List<AttendanceResponse> responses = records.stream()
                .map(AttendanceResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity
                .ok(ApiResponse.success("Attendance marked for " + records.size() + " students", responses));
    }

    /**
     * Get attendance for a course on a specific date (Admin)
     */
    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AttendanceResponse>> getCourseAttendance(
            @PathVariable Long courseId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<Attendance> records = (date != null)
                ? attendanceService.getCourseAttendanceByDate(courseId, date)
                : attendanceService.getCourseAttendance(courseId);
        return ResponseEntity.ok(records.stream().map(AttendanceResponse::fromEntity).collect(Collectors.toList()));
    }

    /**
     * Student views their own attendance
     */
    @GetMapping("/my-attendance")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<AttendanceResponse>> getMyAttendance(Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        List<AttendanceResponse> records = attendanceService.getStudentAttendance(user.getId()).stream()
                .map(AttendanceResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(records);
    }

    /**
     * Student views attendance for a specific course
     */
    @GetMapping("/my-attendance/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<AttendanceResponse>> getMyAttendanceForCourse(
            @PathVariable Long courseId, Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        List<AttendanceResponse> records = attendanceService.getStudentCourseAttendance(user.getId(), courseId).stream()
                .map(AttendanceResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(records);
    }

    /**
     * Get attendance summary (present/absent/late/%) for a student-course combo
     */
    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STUDENT')")
    public ResponseEntity<ApiResponse> getAttendanceSummary(
            @RequestParam(required = false) Long userId,
            @RequestParam Long courseId,
            Authentication authentication) {
        User currentUser = userService.getUserByEmail(authentication.getName());

        // Students can only view their own summary
        Long effectiveUserId = currentUser.isAdmin() ? userId : currentUser.getId();
        if (effectiveUserId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("User ID is required"));
        }

        Map<String, Object> summary = attendanceService.getAttendanceSummary(effectiveUserId, courseId);
        return ResponseEntity.ok(ApiResponse.success("Attendance summary", summary));
    }

    /**
     * Get all attendance records (Admin)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AttendanceResponse>> getAllAttendance() {
        List<AttendanceResponse> records = attendanceService.getAllAttendance().stream()
                .map(AttendanceResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(records);
    }
}
