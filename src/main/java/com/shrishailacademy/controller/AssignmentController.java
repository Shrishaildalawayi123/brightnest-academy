package com.shrishailacademy.controller;

import com.shrishailacademy.dto.AssignmentDTO;
import com.shrishailacademy.model.Assignment;
import com.shrishailacademy.model.Course;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.AssignmentRepository;
import com.shrishailacademy.repository.AssignmentSubmissionRepository;
import com.shrishailacademy.repository.CourseRepository;
import com.shrishailacademy.repository.UserRepository;
import com.shrishailacademy.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API for assignment management
 */
@RestController
@RequestMapping("/api/v1/assignments")
@RequiredArgsConstructor
public class AssignmentController {
    
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    
    /**
     * Get all assignments
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<List<AssignmentDTO>> getAllAssignments(
            @RequestParam(required = false) Long courseId) {
        Long tenantId = TenantContext.requireTenantId();
        
        List<Assignment> assignments;
        if (courseId != null) {
            assignments = assignmentRepository.findByCourse_IdAndTenantId(courseId, tenantId);
        } else {
            assignments = assignmentRepository.findByTenantId(tenantId);
        }
        
        return ResponseEntity.ok(assignments.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }
    
    /**
     * Get upcoming assignments
     */
    @GetMapping("/upcoming")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<List<AssignmentDTO>> getUpcomingAssignments() {
        Long tenantId = TenantContext.requireTenantId();
        List<Assignment> assignments = assignmentRepository.findUpcomingAssignments(tenantId, LocalDateTime.now());
        return ResponseEntity.ok(assignments.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }
    
    /**
     * Get overdue assignments
     */
    @GetMapping("/overdue")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<AssignmentDTO>> getOverdueAssignments() {
        Long tenantId = TenantContext.requireTenantId();
        List<Assignment> assignments = assignmentRepository.findOverdueAssignments(tenantId, LocalDateTime.now());
        return ResponseEntity.ok(assignments.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }
    
    /**
     * Get assignment by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<AssignmentDTO> getAssignmentById(@PathVariable Long id) {
        Long tenantId = TenantContext.requireTenantId();
        Assignment assignment = assignmentRepository.findById(id)
                .filter(a -> a.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + id));
        return ResponseEntity.ok(convertToDTO(assignment));
    }
    
    /**
     * Create new assignment (TEACHER/ADMIN only)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<AssignmentDTO> createAssignment(@Valid @RequestBody AssignmentDTO dto) {
        Assignment assignment = convertToEntity(dto);
        Assignment created = assignmentRepository.save(assignment);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(created));
    }
    
    /**
     * Update assignment (TEACHER/ADMIN only)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<AssignmentDTO> updateAssignment(
            @PathVariable Long id,
            @Valid @RequestBody AssignmentDTO dto) {
        Long tenantId = TenantContext.requireTenantId();
        Assignment assignment = assignmentRepository.findById(id)
                .filter(a -> a.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + id));
        
        // Update fields
        assignment.setTitle(dto.getTitle());
        assignment.setDescription(dto.getDescription());
        assignment.setDueDate(dto.getDueDate());
        assignment.setMaxScore(dto.getMaxScore());
        assignment.setAttachmentUrl(dto.getAttachmentUrl());
        
        Assignment updated = assignmentRepository.save(assignment);
        return ResponseEntity.ok(convertToDTO(updated));
    }
    
    /**
     * Publish assignment
     */
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<AssignmentDTO> publishAssignment(@PathVariable Long id) {
        Long tenantId = TenantContext.requireTenantId();
        Assignment assignment = assignmentRepository.findById(id)
                .filter(a -> a.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + id));
        
        assignment.publish();
        Assignment updated = assignmentRepository.save(assignment);
        return ResponseEntity.ok(convertToDTO(updated));
    }
    
    /**
     * Unpublish assignment
     */
    @PostMapping("/{id}/unpublish")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<AssignmentDTO> unpublishAssignment(@PathVariable Long id) {
        Long tenantId = TenantContext.requireTenantId();
        Assignment assignment = assignmentRepository.findById(id)
                .filter(a -> a.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + id));
        
        assignment.unpublish();
        Assignment updated = assignmentRepository.save(assignment);
        return ResponseEntity.ok(convertToDTO(updated));
    }
    
    /**
     * Delete assignment (ADMIN only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAssignment(@PathVariable Long id) {
        Long tenantId = TenantContext.requireTenantId();
        Assignment assignment = assignmentRepository.findById(id)
                .filter(a -> a.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + id));
        
        assignmentRepository.delete(assignment);
        return ResponseEntity.noContent().build();
    }
    
    // Helper methods
    
    private AssignmentDTO convertToDTO(Assignment assignment) {
        Long totalSubmissions = submissionRepository.countByAssignmentId(assignment.getId());
        Long gradedSubmissions = submissionRepository.countByAssignmentIdAndGradedAtIsNotNull(assignment.getId());
        Double averageScore = submissionRepository.getAverageScore(assignment.getId());
        
        return AssignmentDTO.builder()
                .id(assignment.getId())
                .courseId(assignment.getCourse().getId())
                .courseName(assignment.getCourse().getTitle())
                .teacherId(assignment.getTeacher().getId())
                .teacherName(assignment.getTeacher().getName())
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .dueDate(assignment.getDueDate())
                .maxScore(assignment.getMaxScore())
                .attachmentUrl(assignment.getAttachmentUrl())
                .isPublished(assignment.getIsPublished())
                .totalSubmissions(totalSubmissions.intValue())
                .gradedSubmissions(gradedSubmissions.intValue())
                .averageScore(averageScore)
                .isOverdue(assignment.isOverdue())
                .build();
    }
    
    private Assignment convertToEntity(AssignmentDTO dto) {
        Long tenantId = TenantContext.requireTenantId();
        
        Course course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + dto.getCourseId()));
        
        User teacher = userRepository.findById(dto.getTeacherId())
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found: " + dto.getTeacherId()));
        
        return Assignment.builder()
                .tenantId(tenantId)
                .course(course)
                .teacher(teacher)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .dueDate(dto.getDueDate())
                .maxScore(dto.getMaxScore())
                .attachmentUrl(dto.getAttachmentUrl())
                .isPublished(dto.getIsPublished() != null ? dto.getIsPublished() : false)
                .build();
    }
}
