package com.shrishailacademy.controller;

import com.shrishailacademy.dto.AssignmentSubmissionDTO;
import com.shrishailacademy.model.Assignment;
import com.shrishailacademy.model.AssignmentSubmission;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.AssignmentRepository;
import com.shrishailacademy.repository.AssignmentSubmissionRepository;
import com.shrishailacademy.repository.UserRepository;
import com.shrishailacademy.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API for assignment submissions and grading
 */
@RestController
@RequestMapping("/api/v1/submissions")
@RequiredArgsConstructor
public class AssignmentSubmissionController {
    
    private final AssignmentSubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    
    /**
     * Get all submissions for an assignment (TEACHER/ADMIN only)
     */
    @GetMapping("/assignment/{assignmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<AssignmentSubmissionDTO>> getAssignmentSubmissions(@PathVariable Long assignmentId) {
        Long tenantId = TenantContext.requireTenantId();
        List<AssignmentSubmission> submissions = submissionRepository.findByAssignment_IdAndTenantId(assignmentId, tenantId);
        return ResponseEntity.ok(submissions.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }
    
    /**
     * Get student's submissions
     */
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ResponseEntity<List<AssignmentSubmissionDTO>> getStudentSubmissions(@PathVariable Long studentId) {
        Long tenantId = TenantContext.requireTenantId();
        List<AssignmentSubmission> submissions = submissionRepository.findByStudent_IdAndTenantId(studentId, tenantId);
        return ResponseEntity.ok(submissions.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }
    
    /**
     * Get ungraded submissions (TEACHER/ADMIN only)
     */
    @GetMapping("/ungraded")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<List<AssignmentSubmissionDTO>> getUngradedSubmissions(
            @RequestParam(required = false) Long assignmentId) {
        Long tenantId = TenantContext.requireTenantId();
        
        List<AssignmentSubmission> submissions;
        if (assignmentId != null) {
            submissions = submissionRepository.findUngradedSubmissions(assignmentId);
        } else {
            submissions = submissionRepository.findByTenantIdAndGradedAtIsNull(tenantId);
        }
        
        return ResponseEntity.ok(submissions.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }
    
    /**
     * Submit assignment (STUDENT only)
     */
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AssignmentSubmissionDTO> submitAssignment(
            @Valid @RequestBody AssignmentSubmissionDTO dto,
            Authentication authentication) {
        
        Long tenantId = TenantContext.requireTenantId();
        String studentEmail = authentication.getName();
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new IllegalStateException("Student not found: " + studentEmail));
        
        Assignment assignment = assignmentRepository.findById(dto.getAssignmentId())
                .filter(a -> a.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + dto.getAssignmentId()));
        
        // Check if already submitted
        if (submissionRepository.findByAssignmentIdAndStudentId(dto.getAssignmentId(), student.getId()).isPresent()) {
            throw new IllegalStateException("Assignment already submitted. Use update endpoint to modify.");
        }
        
        // Check if assignment is published
        if (!assignment.getIsPublished()) {
            throw new IllegalStateException("Cannot submit to unpublished assignment");
        }
        
        boolean isLate = LocalDateTime.now().isAfter(assignment.getDueDate());
        
        AssignmentSubmission submission = AssignmentSubmission.builder()
                .tenantId(tenantId)
                .assignment(assignment)
                .student(student)
                .submissionText(dto.getSubmissionText())
                .attachmentUrl(dto.getAttachmentUrl())
                .submittedAt(LocalDateTime.now())
                .isLate(isLate)
                .build();
        
        AssignmentSubmission saved = submissionRepository.save(submission);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDTO(saved));
    }
    
    /**
     * Update submission (STUDENT only - before grading)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<AssignmentSubmissionDTO> updateSubmission(
            @PathVariable Long id,
            @Valid @RequestBody AssignmentSubmissionDTO dto,
            Authentication authentication) {
        
        Long tenantId = TenantContext.requireTenantId();
        String studentEmail = authentication.getName();
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new IllegalStateException("Student not found: " + studentEmail));
        
        AssignmentSubmission submission = submissionRepository.findById(id)
                .filter(s -> s.getTenantId().equals(tenantId))
                .filter(s -> s.getStudent().getId().equals(student.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Submission not found or unauthorized: " + id));
        
        // Cannot update graded submission
        if (submission.isGraded()) {
            throw new IllegalStateException("Cannot update graded submission");
        }
        
        submission.setSubmissionText(dto.getSubmissionText());
        submission.setAttachmentUrl(dto.getAttachmentUrl());
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setIsLate(LocalDateTime.now().isAfter(submission.getAssignment().getDueDate()));
        
        AssignmentSubmission updated = submissionRepository.save(submission);
        return ResponseEntity.ok(convertToDTO(updated));
    }
    
    /**
     * Grade submission (TEACHER/ADMIN only)
     */
    @PostMapping("/{id}/grade")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<AssignmentSubmissionDTO> gradeSubmission(
            @PathVariable Long id,
            @Valid @RequestBody AssignmentSubmissionDTO dto,
            Authentication authentication) {
        
        Long tenantId = TenantContext.requireTenantId();
        String teacherEmail = authentication.getName();
        User teacher = userRepository.findByEmail(teacherEmail)
                .orElseThrow(() -> new IllegalStateException("Teacher not found: " + teacherEmail));
        
        AssignmentSubmission submission = submissionRepository.findById(id)
                .filter(s -> s.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Submission not found: " + id));
        
        // Validate score
        if (dto.getScore() > submission.getAssignment().getMaxScore()) {
            throw new IllegalArgumentException("Score cannot exceed maximum: " + submission.getAssignment().getMaxScore());
        }
        
        submission.grade(dto.getScore(), dto.getFeedback(), teacher.getId());
        AssignmentSubmission graded = submissionRepository.save(submission);
        
        return ResponseEntity.ok(convertToDTO(graded));
    }
    
    /**
     * Delete submission (ADMIN only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSubmission(@PathVariable Long id) {
        Long tenantId = TenantContext.requireTenantId();
        AssignmentSubmission submission = submissionRepository.findById(id)
                .filter(s -> s.getTenantId().equals(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Submission not found: " + id));
        
        submissionRepository.delete(submission);
        return ResponseEntity.noContent().build();
    }
    
    // Helper method
    
    private AssignmentSubmissionDTO convertToDTO(AssignmentSubmission submission) {
        return AssignmentSubmissionDTO.builder()
                .id(submission.getId())
                .assignmentId(submission.getAssignment().getId())
                .assignmentTitle(submission.getAssignment().getTitle())
                .studentId(submission.getStudent().getId())
                .studentName(submission.getStudent().getName())
                .studentEmail(submission.getStudent().getEmail())
                .submissionText(submission.getSubmissionText())
                .attachmentUrl(submission.getAttachmentUrl())
                .submittedAt(submission.getSubmittedAt())
                .score(submission.getScore())
                .feedback(submission.getFeedback())
                .gradedAt(submission.getGradedAt())
                .isLate(submission.getIsLate())
                .isGraded(submission.isGraded())
                .letterGrade(submission.isGraded() ? submission.getLetterGrade() : null)
                .percentageScore(submission.isGraded() ? submission.getPercentageScore() : null)
                .maxScore(submission.getAssignment().getMaxScore())
                .build();
    }
}
