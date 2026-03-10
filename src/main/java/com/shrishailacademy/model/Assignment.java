package com.shrishailacademy.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Assignment Entity - Represents course assignments/homework
 */
@Entity
@Table(name = "assignments", indexes = {
    @Index(name = "idx_assignment_tenant", columnList = "tenant_id"),
    @Index(name = "idx_assignment_course", columnList = "course_id"),
    @Index(name = "idx_assignment_teacher", columnList = "teacher_id"),
    @Index(name = "idx_assignment_due_date", columnList = "dueDate"),
    @Index(name = "idx_assignment_published", columnList = "isPublished"),
    @Index(name = "idx_assignment_course_due", columnList = "course_id, dueDate")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assignment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_id", nullable = false)
    @NotNull(message = "Tenant ID is required")
    private Long tenantId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @NotNull(message = "Course is required")
    private Course course;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    @NotNull(message = "Teacher is required")
    private User teacher;
    
    @Column(name = "title", nullable = false, length = 200)
    @NotBlank(message = "Assignment title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;
    
    @Column(name = "description", columnDefinition = "TEXT")
    @Size(max = 10000, message = "Description must not exceed 10000 characters")
    private String description;
    
    @Column(name = "due_date", nullable = false)
    @NotNull(message = "Due date is required")
    private LocalDateTime dueDate;
    
    @Column(name = "max_score")
    @Min(value = 1, message = "Max score must be at least 1")
    @Max(value = 1000, message = "Max score must not exceed 1000")
    @Builder.Default
    private Integer maxScore = 100;
    
    @Column(name = "attachment_url", length = 500)
    @Size(max = 500, message = "Attachment URL must not exceed 500 characters")
    private String attachmentUrl;
    
    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private Boolean isPublished = false;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by")
    private Long createdBy;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isPublished == null) {
            isPublished = false;
        }
        if (maxScore == null) {
            maxScore = 100;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Check if assignment is overdue
     */
    public boolean isOverdue() {
        return dueDate.isBefore(LocalDateTime.now());
    }
    
    /**
     * Check if assignment is due soon (within 24 hours)
     */
    public boolean isDueSoon() {
        LocalDateTime twentyFourHoursFromNow = LocalDateTime.now().plusHours(24);
        return !isOverdue() && dueDate.isBefore(twentyFourHoursFromNow);
    }
    
    /**
     * Publish assignment (make visible to students)
     */
    public void publish() {
        this.isPublished = true;
    }
    
    /**
     * Unpublish assignment (hide from students)
     */
    public void unpublish() {
        this.isPublished = false;
    }
}
