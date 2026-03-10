package com.shrishailacademy.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * AssignmentSubmission Entity - Represents student assignment submissions
 */
@Entity
@Table(name = "assignment_submissions",
    uniqueConstraints = @UniqueConstraint(name = "uk_student_assignment", columnNames = {"student_id", "assignment_id"}),
    indexes = {
        @Index(name = "idx_submission_tenant", columnList = "tenant_id"),
        @Index(name = "idx_submission_assignment", columnList = "assignment_id"),
        @Index(name = "idx_submission_student", columnList = "student_id"),
        @Index(name = "idx_submission_graded", columnList = "gradedAt"),
        @Index(name = "idx_submission_student_assignment", columnList = "student_id, assignment_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentSubmission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_id", nullable = false)
    @NotNull(message = "Tenant ID is required")
    private Long tenantId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    @NotNull(message = "Assignment is required")
    private Assignment assignment;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @NotNull(message = "Student is required")
    private User student;
    
    @Column(name = "submission_text", columnDefinition = "TEXT")
    @Size(max = 10000, message = "Submission text must not exceed 10000 characters")
    private String submissionText;
    
    @Column(name = "attachment_url", length = 500)
    @Size(max = 500, message = "Attachment URL must not exceed 500 characters")
    private String attachmentUrl;
    
    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;
    
    @Column(name = "score")
    @Min(value = 0, message = "Score must be at least 0")
    @Max(value = 100, message = "Score must not exceed 100")
    private Integer score;
    
    @Column(name = "feedback", columnDefinition = "TEXT")
    @Size(max = 5000, message = "Feedback must not exceed 5000 characters")
    private String feedback;
    
    @Column(name = "graded_at")
    private LocalDateTime gradedAt;
    
    @Column(name = "graded_by")
    private Long gradedBy;
    
    @Column(name = "is_late", nullable = false)
    @Builder.Default
    private Boolean isLate = false;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
        if (isLate == null) {
            isLate = false;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Grade the submission
     */
    public void grade(Integer scoreValue, String feedbackText, Long gradedByUserId) {
        if (scoreValue < 0 || scoreValue > 100) {
            throw new IllegalArgumentException("Score must be between 0 and 100");
        }
        this.score = scoreValue;
        this.feedback = feedbackText;
        this.gradedAt = LocalDateTime.now();
        this.gradedBy = gradedByUserId;
    }
    
    /**
     * Check if submission is graded
     */
    public boolean isGraded() {
        return score != null && gradedAt != null;
    }
    
    /**
     * Check if submission was made late
     */
    public boolean wasLate() {
        return isLate != null && isLate;
    }
    
    /**
     * Get percentage score
     */
    public Double getPercentageScore() {
        if (score == null) {
            return null;
        }
        return (score.doubleValue() / 100.0) * 100.0;
    }
    
    /**
     * Get letter grade (A, B, C, D, F)
     */
    public String getLetterGrade() {
        if (score == null) {
            return "Not Graded";
        }
        if (score >= 90) return "A";
        if (score >= 80) return "B";
        if (score >= 70) return "C";
        if (score >= 60) return "D";
        return "F";
    }
}
