package com.shrishailacademy.controller;

import com.shrishailacademy.dto.ClassScheduleDTO;
import com.shrishailacademy.model.ClassSchedule;
import com.shrishailacademy.service.ClassScheduleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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
@Validated
public class ClassScheduleController {

    private final ClassScheduleService scheduleService;

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
        ClassSchedule schedule = scheduleService.getScheduleById(id);
        return ResponseEntity.ok(convertToDTO(schedule));
    }

    /**
     * Create new schedule (ADMIN/TEACHER only)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ClassScheduleDTO> createSchedule(@Valid @RequestBody ClassScheduleDTO dto) {
        ClassSchedule created = scheduleService.createSchedule(
                dto.getCourseId(),
                dto.getTeacherId(),
                dto.getDayOfWeek(),
                dto.getStartTime(),
                dto.getEndTime(),
                dto.getRoomNumber(),
                dto.getMaxStudents(),
                dto.getIsActive());
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
        ClassSchedule updated = scheduleService.updateSchedule(
            id,
            dto.getCourseId(),
            dto.getTeacherId(),
            dto.getDayOfWeek(),
            dto.getStartTime(),
            dto.getEndTime(),
            dto.getRoomNumber(),
            dto.getMaxStudents(),
            dto.getIsActive());
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
            @RequestParam(defaultValue = "4")
            @Min(value = 1, message = "weeksAhead must be at least 1")
            @Max(value = 12, message = "weeksAhead must not exceed 12") Integer weeksAhead) {
        scheduleService.generateSessionsForScheduleId(id, weeksAhead);
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

}
