package com.shrishailacademy.controller;

import com.shrishailacademy.dto.AttendanceDTO;
import com.shrishailacademy.model.SessionAttendance;
import com.shrishailacademy.model.ClassSession;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.SessionAttendanceRepository;
import com.shrishailacademy.repository.ClassSessionRepository;
import com.shrishailacademy.repository.UserRepository;
import com.shrishailacademy.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API for session-based attendance management (class scheduling system)
 */
@RestController
@RequestMapping("/api/v1/session-attendance")
@RequiredArgsConstructor
public class SessionAttendanceController {

        private final SessionAttendanceRepository attendanceRepository;
        private final ClassSessionRepository sessionRepository;
        private final UserRepository userRepository;

        /**
         * Get attendance for a session
         */
        @GetMapping("/session/{sessionId}")
        @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
        public ResponseEntity<List<AttendanceDTO>> getSessionAttendance(@PathVariable Long sessionId) {
                Long tenantId = TenantContext.requireTenantId();
                List<SessionAttendance> attendances = attendanceRepository.findBySession_IdAndTenantId(sessionId,
                                tenantId);
                return ResponseEntity.ok(attendances.stream().map(this::convertToDTO).collect(Collectors.toList()));
        }

        /**
         * Get student's attendance history
         */
        @GetMapping("/student/{studentId}")
        @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
        public ResponseEntity<List<AttendanceDTO>> getStudentAttendance(@PathVariable Long studentId) {
                Long tenantId = TenantContext.requireTenantId();
                List<SessionAttendance> attendances = attendanceRepository.findByStudent_IdAndTenantId(studentId,
                                tenantId);
                return ResponseEntity.ok(attendances.stream().map(this::convertToDTO).collect(Collectors.toList()));
        }

        /**
         * Mark attendance (bulk operation)
         */
        @PostMapping("/mark")
        @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
        public ResponseEntity<List<AttendanceDTO>> markAttendance(
                        @Valid @RequestBody List<AttendanceDTO> attendanceDTOs,
                        Authentication authentication) {

                Long tenantId = TenantContext.requireTenantId();
                String markedByEmail = authentication.getName();
                User markedBy = userRepository.findByEmailAndTenantId(markedByEmail, tenantId)
                                .orElseThrow(() -> new IllegalStateException("User not found: " + markedByEmail));

                List<SessionAttendance> attendances = attendanceDTOs.stream()
                                .map(dto -> {
                                        ClassSession session = sessionRepository.findByIdAndTenantId(dto.getSessionId(),
                                                        tenantId)
                                                        .orElseThrow(() -> new IllegalArgumentException(
                                                                        "Session not found: " + dto.getSessionId()));

                                        User student = userRepository.findByIdAndTenantId(dto.getStudentId(),
                                                        tenantId)
                                                        .orElseThrow(() -> new IllegalArgumentException(
                                                                        "Student not found: " + dto.getStudentId()));

                                        // Check if attendance already exists
                                        SessionAttendance attendance = attendanceRepository
                                                        .findBySessionIdAndStudentIdAndTenantId(dto.getSessionId(),
                                                                        dto.getStudentId(), tenantId)
                                                        .orElse(SessionAttendance.builder()
                                                                        .tenantId(tenantId)
                                                                        .session(session)
                                                                        .student(student)
                                                                        .build());

                                        // Update status based on DTO
                                        switch (dto.getStatus()) {
                                                case PRESENT -> attendance.markPresent(
                                                                dto.getCheckInTime() != null ? dto.getCheckInTime()
                                                                                : LocalTime.now(),
                                                                markedBy.getId());
                                                case LATE -> attendance.markLate(
                                                                dto.getCheckInTime() != null ? dto.getCheckInTime()
                                                                                : LocalTime.now(),
                                                                markedBy.getId());
                                                case ABSENT -> attendance.markAbsent(markedBy.getId());
                                                case EXCUSED ->
                                                        attendance.markExcused(dto.getNotes(), markedBy.getId());
                                        }

                                        if (dto.getNotes() != null) {
                                                attendance.setNotes(dto.getNotes());
                                        }

                                        return attendance;
                                })
                                .collect(Collectors.toList());

                List<SessionAttendance> saved = attendanceRepository.saveAll(attendances);

                // Mark session as attendance completed
                if (!attendanceDTOs.isEmpty()) {
                        Long sessionId = attendanceDTOs.get(0).getSessionId();
                        sessionRepository.findByIdAndTenantId(sessionId, tenantId).ifPresent(session -> {
                                session.setAttendanceMarked(true);
                                sessionRepository.save(session);
                        });
                }

                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(saved.stream().map(this::convertToDTO).collect(Collectors.toList()));
        }

        /**
         * Update single attendance record
         */
        @PutMapping("/{id}")
        @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
        public ResponseEntity<AttendanceDTO> updateAttendance(
                        @PathVariable Long id,
                        @Valid @RequestBody AttendanceDTO dto,
                        Authentication authentication) {

                Long tenantId = TenantContext.requireTenantId();
                String markedByEmail = authentication.getName();
                User markedBy = userRepository.findByEmailAndTenantId(markedByEmail, tenantId)
                                .orElseThrow(() -> new IllegalStateException("User not found: " + markedByEmail));

                SessionAttendance attendance = attendanceRepository.findByIdAndTenantId(id, tenantId)
                                .orElseThrow(() -> new IllegalArgumentException("Attendance record not found: " + id));

                // Update based on status
                switch (dto.getStatus()) {
                        case PRESENT -> attendance.markPresent(
                                        dto.getCheckInTime() != null ? dto.getCheckInTime() : LocalTime.now(),
                                        markedBy.getId());
                        case LATE -> attendance.markLate(
                                        dto.getCheckInTime() != null ? dto.getCheckInTime() : LocalTime.now(),
                                        markedBy.getId());
                        case ABSENT -> attendance.markAbsent(markedBy.getId());
                        case EXCUSED -> attendance.markExcused(dto.getNotes(), markedBy.getId());
                }

                if (dto.getNotes() != null) {
                        attendance.setNotes(dto.getNotes());
                }

                SessionAttendance updated = attendanceRepository.save(attendance);
                return ResponseEntity.ok(convertToDTO(updated));
        }

        // Helper method

        private AttendanceDTO convertToDTO(SessionAttendance attendance) {
                return AttendanceDTO.builder()
                                .id(attendance.getId())
                                .sessionId(attendance.getSession().getId())
                                .studentId(attendance.getStudent().getId())
                                .studentName(attendance.getStudent().getName())
                                .studentEmail(attendance.getStudent().getEmail())
                                .status(attendance.getStatus())
                                .checkInTime(attendance.getCheckInTime())
                                .notes(attendance.getNotes())
                                .markedAt(attendance.getMarkedAt())
                                .courseName(attendance.getSession().getSchedule().getCourse().getTitle())
                                .build();
        }
}
