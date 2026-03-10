package com.shrishailacademy.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * ClassSession Entity - Represents individual class session instances
 * Generated from ClassSchedule templates for specific dates
 */
@Entity
@Table(name = "class_sessions", 
    uniqueConstraints = @UniqueConstraint(name = "uk_schedule_date", columnNames = {"schedule_id", "session_date"}),
    indexes = {
        @Index(name = "idx_session_tenant", columnList = "tenant_id"),
        @Index(name = "idx_session_schedule", columnList = "schedule_id"),
        @Index(name = "idx_session_date", columnList = "sessionDate"),
        @Index(name = "idx_session_status", columnList = "status"),
        @Index(name = "idx_session_date_status", columnList = "sessionDate, status")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_id", nullable = false)
    @NotNull(message = "Tenant ID is required")
    private Long tenantId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    @NotNull(message = "Schedule is required")
    private ClassSchedule schedule;
    
    @Column(name = "session_date", nullable = false)
    @NotNull(message = "Session date is required")
    private LocalDate sessionDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SessionStatus status = SessionStatus.SCHEDULED;
    
    @Column(name = "actual_start_time")
    private LocalTime actualStartTime;
    
    @Column(name = "actual_end_time")
    private LocalTime actualEndTime;
    
    @Column(name = "attendance_marked", nullable = false)
    @Builder.Default
    private Boolean attendanceMarked = false;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    @Size(max = 5000, message = "Notes must not exceed 5000 characters")
    private String notes;
    
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    @Size(max = 1000, message = "Cancellation reason must not exceed 1000 characters")
    private String cancellationReason;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = SessionStatus.SCHEDULED;
        }
        if (attendanceMarked == null) {
            attendanceMarked = false;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Session status enum
     */
    public enum SessionStatus {
        SCHEDULED,    // Session is scheduled to happen
        IN_PROGRESS,  // Session is currently ongoing
        COMPLETED,    // Session has finished
        CANCELLED     // Session was cancelled
    }
    
    /**
     * Mark session as completed
     */
    public void markCompleted(LocalTime actualStart, LocalTime actualEnd) {
        this.status = SessionStatus.COMPLETED;
        this.actualStartTime = actualStart;
        this.actualEndTime = actualEnd;
    }
    
    /**
     * Cancel session with reason
     */
    public void cancel(String reason) {
        this.status = SessionStatus.CANCELLED;
        this.cancellationReason = reason;
    }
    
    /**
     * Start session
     */
    public void start() {
        this.status = SessionStatus.IN_PROGRESS;
        this.actualStartTime = LocalTime.now();
    }
    
    /**
     * Check if session is in the past
     */
    public boolean isPast() {
        return sessionDate.isBefore(LocalDate.now());
    }
    
    /**
     * Check if session is today
     */
    public boolean isToday() {
        return sessionDate.equals(LocalDate.now());
    }
    
    /**
     * Check if session is upcoming (future)
     */
    public boolean isUpcoming() {
        return sessionDate.isAfter(LocalDate.now());
    }
}
