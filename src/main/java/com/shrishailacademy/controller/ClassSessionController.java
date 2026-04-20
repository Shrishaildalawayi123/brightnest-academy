package com.shrishailacademy.controller;

import com.shrishailacademy.dto.ClassSessionDTO;
import com.shrishailacademy.model.ClassSession;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.ClassSessionRepository;
import com.shrishailacademy.repository.UserRepository;
import com.shrishailacademy.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API for managing class sessions
 */
@RestController
@RequestMapping({"/api/sessions", "/api/v1/sessions"})
@RequiredArgsConstructor
public class ClassSessionController {
    
    private final ClassSessionRepository sessionRepository;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<ClassSessionDTO>> getSessions(
            @RequestParam(defaultValue = "false") boolean today,
            Authentication authentication) {
        if (!today) {
            return getUpcomingSessions();
        }

        Long tenantId = TenantContext.requireTenantId();
        User currentUser = userRepository.findByEmailAndTenantId(authentication.getName(), tenantId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + authentication.getName()));

        List<ClassSession> sessions = currentUser.getRole() == User.Role.ADMIN
                ? sessionRepository.findTodaySessions(tenantId, LocalDate.now())
                : sessionRepository.findTodaySessionsForTeacher(tenantId, currentUser.getId(), LocalDate.now());
        return ResponseEntity.ok(sessions.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }
    
    /**
     * Get upcoming sessions
     */
    @GetMapping("/upcoming")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<List<ClassSessionDTO>> getUpcomingSessions() {
        Long tenantId = TenantContext.requireTenantId();
        List<ClassSession> sessions = sessionRepository.findUpcomingSessions(tenantId, LocalDate.now());
        return ResponseEntity.ok(sessions.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }
    
    /**
     * Get sessions needing attendance
     */
    @GetMapping("/needs-attendance")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<ClassSessionDTO>> getSessionsNeedingAttendance() {
        Long tenantId = TenantContext.requireTenantId();
        List<ClassSession> sessions = sessionRepository.findSessionsNeedingAttendance(tenantId, LocalDate.now());
        return ResponseEntity.ok(sessions.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }
    
    /**
     * Get session by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ClassSessionDTO> getSessionById(@PathVariable Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ClassSession session = sessionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + id));
        return ResponseEntity.ok(convertToDTO(session));
    }
    
    /**
     * Start a session (marks as IN_PROGRESS)
     */
    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ClassSessionDTO> startSession(@PathVariable Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ClassSession session = sessionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + id));
        
        session.start();
        ClassSession updated = sessionRepository.save(session);
        return ResponseEntity.ok(convertToDTO(updated));
    }
    
    /**
     * Mark session as completed
     */
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ClassSessionDTO> completeSession(@PathVariable Long id) {
        Long tenantId = TenantContext.requireTenantId();
        ClassSession session = sessionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + id));
        
        session.markCompleted(session.getActualStartTime() != null ? session.getActualStartTime() : LocalTime.now(), LocalTime.now());
        ClassSession updated = sessionRepository.save(session);
        return ResponseEntity.ok(convertToDTO(updated));
    }
    
    /**
     * Cancel session
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ClassSessionDTO> cancelSession(
            @PathVariable Long id,
            @RequestBody(required = false) String reason) {
        Long tenantId = TenantContext.requireTenantId();
        ClassSession session = sessionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + id));
        
        session.cancel(reason);
        ClassSession updated = sessionRepository.save(session);
        return ResponseEntity.ok(convertToDTO(updated));
    }
    
    /**
     * Update session notes
     */
    @PatchMapping("/{id}/notes")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ClassSessionDTO> updateSessionNotes(
            @PathVariable Long id,
            @RequestBody @Valid ClassSessionDTO dto) {
        Long tenantId = TenantContext.requireTenantId();
        ClassSession session = sessionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + id));
        
        session.setNotes(dto.getNotes());
        ClassSession updated = sessionRepository.save(session);
        return ResponseEntity.ok(convertToDTO(updated));
    }
    
    // Helper method
    
    private ClassSessionDTO convertToDTO(ClassSession session) {
        return ClassSessionDTO.builder()
                .id(session.getId())
                .scheduleId(session.getSchedule().getId())
                .sessionDate(session.getSessionDate())
                .status(session.getStatus())
                .actualStartTime(session.getActualStartTime())
                .actualEndTime(session.getActualEndTime())
                .attendanceMarked(session.getAttendanceMarked())
                .notes(session.getNotes())
                .cancellationReason(session.getCancellationReason())
                .courseName(session.getSchedule().getCourse().getTitle())
                .teacherName(session.getSchedule().getTeacher().getName())
                .roomNumber(session.getSchedule().getRoomNumber())
                .scheduledStartTime(session.getSchedule().getStartTime())
                .scheduledEndTime(session.getSchedule().getEndTime())
                .build();
    }
}
