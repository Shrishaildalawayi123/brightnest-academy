package com.shrishailacademy.controller;

import com.shrishailacademy.dto.ClassScheduleDTO;
import com.shrishailacademy.model.ClassSchedule;
import com.shrishailacademy.model.Course;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.ClassScheduleRepository;
import com.shrishailacademy.repository.CourseRepository;
import com.shrishailacademy.repository.UserRepository;
import com.shrishailacademy.service.ClassScheduleService;
import com.shrishailacademy.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API for managing class schedules
 * Accessible by ADMIN and TEACHER roles
 */
@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
public class ClassScheduleController {

    private final ClassScheduleService scheduleService;
    private final ClassScheduleRepository scheduleRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    /**
     * Get all active schedules
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<List<ClassScheduleDTO>> getAllSchedules() {
        List<ClassSchedule> schedules = scheduleService.getAllActiveSchedules();
        List<ClassScheduleDTO> dtos = schedules.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get schedule by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ClassScheduleDTO> getScheduleById(@PathVariable Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ClassSchedule schedule = scheduleRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));
        return ResponseEntity.ok(convertToDTO(schedule));
    }

    /**
     * Create new schedule (ADMIN/TEACHER only)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ClassScheduleDTO> createSchedule(@Valid @RequestBody ClassScheduleDTO dto) {
        ClassSchedule schedule = convertToEntity(dto);
        ClassSchedule created = scheduleService.createSchedule(schedule);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(created));
    }

    /**
     * Update existing schedule (ADMIN/TEACHER only)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ClassScheduleDTO> updateSchedule(
            @PathVariable Long id,
            @Valid @RequestBody ClassScheduleDTO dto) {
        ClassSchedule updates = convertToEntity(dto);
        ClassSchedule updated = scheduleService.updateSchedule(id, updates);
        return ResponseEntity.ok(convertToDTO(updated));
    }

    /**
     * Delete schedule (soft delete - ADMIN only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        scheduleService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Generate sessions for a schedule manually
     */
    @PostMapping("/{id}/generate-sessions")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<String> generateSessions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "4") Integer weeksAhead) {
        Long tenantId = TenantContext.requireTenantId();
        ClassSchedule schedule = scheduleRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));
        scheduleService.generateSessionsForSchedule(schedule, weeksAhead);
        return ResponseEntity.ok(String.format("Generated sessions for the next %d weeks", weeksAhead));
    }

    // Helper methods

    private ClassScheduleDTO convertToDTO(ClassSchedule schedule) {
        return ClassScheduleDTO.builder()
                .id(schedule.getId())
                .courseId(schedule.getCourse().getId())
                .courseName(schedule.getCourse().getTitle())
                .teacherId(schedule.getTeacher().getId())
                .teacherName(schedule.getTeacher().getName())
                .teacherEmail(schedule.getTeacher().getEmail())
                .dayOfWeek(schedule.getDayOfWeek())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .roomNumber(schedule.getRoomNumber())
                .maxStudents(schedule.getMaxStudents())
                .isActive(schedule.getIsActive())
                .build();
    }

    private ClassSchedule convertToEntity(ClassScheduleDTO dto) {
        Long tenantId = TenantContext.requireTenantId();

        Course course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + dto.getCourseId()));

        User teacher = userRepository.findById(dto.getTeacherId())
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found: " + dto.getTeacherId()));

        return ClassSchedule.builder()
                .tenantId(tenantId)
                .course(course)
                .teacher(teacher)
                .dayOfWeek(dto.getDayOfWeek())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .roomNumber(dto.getRoomNumber())
                .maxStudents(dto.getMaxStudents())
                .isActive(Boolean.TRUE.equals(dto.getIsActive()) || dto.getIsActive() == null)
                .build();
    }
}
