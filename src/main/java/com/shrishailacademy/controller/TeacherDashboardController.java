package com.shrishailacademy.controller;

import com.shrishailacademy.model.ClassSchedule;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.ClassScheduleRepository;
import com.shrishailacademy.repository.EnrollmentRepository;
import com.shrishailacademy.service.UserService;
import com.shrishailacademy.tenant.TenantContext;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/api/teacher-dashboard", "/api/v1/teacher-dashboard"})
public class TeacherDashboardController {

    private final ClassScheduleRepository classScheduleRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserService userService;

    public TeacherDashboardController(
            ClassScheduleRepository classScheduleRepository,
            EnrollmentRepository enrollmentRepository,
            UserService userService) {
        this.classScheduleRepository = classScheduleRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.userService = userService;
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Map<String, Long>> getSummary(Authentication authentication) {
        Long tenantId = TenantContext.requireTenantId();
        User currentUser = userService.getUserByEmail(authentication.getName());

        List<ClassSchedule> schedules = classScheduleRepository.findByTenantIdAndIsActiveTrueOrderByDayOfWeekAsc(tenantId);
        if (currentUser.getRole() == User.Role.TEACHER) {
            schedules = schedules.stream()
                    .filter(schedule -> schedule.getTeacher().getId().equals(currentUser.getId()))
                    .toList();
        }

        Set<Long> courseIds = schedules.stream()
                .map(schedule -> schedule.getCourse().getId())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        long studentsEnrolled = courseIds.stream()
                .mapToLong(courseId -> enrollmentRepository.countByCourseIdAndTenantId(courseId, tenantId))
                .sum();

        return ResponseEntity.ok(Map.of(
                "activeCourses", (long) courseIds.size(),
                "studentsEnrolled", studentsEnrolled));
    }
}
