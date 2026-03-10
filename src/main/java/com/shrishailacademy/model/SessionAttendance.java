package com.shrishailacademy.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalTime;
import java.time.LocalDateTime;

/**
 * SessionAttendance Entity - Tracks student attendance for each class session
 */
@Entity
@Table(name = "session_attendance",
    uniqueConstraints = @UniqueConstraint(name = "uk_session_student", columnNames = {"session_id", "student_id"}),
    indexes = {
        @Index(name = "idx_attendance_tenant", columnList = "tenant_id"),
        @Index(name = "idx_attendance_session", columnList = "session_id"),
        @Index(name = "idx_attendance_student", columnList = "student_id"),
        @Index(name = "idx_attendance_status", columnList = "status"),
        @Index(name = "idx_attendance_student_status", columnList = "student_id, status")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionAttendance {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "tenant_id", nullable = false)
    @NotNull(message = "Tenant ID is required")
    private Long tenantId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    @NotNull(message = "Session is required")
    private ClassSession session;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @NotNull(message = "Student is required")
    private User student;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AttendanceStatus status = AttendanceStatus.ABSENT;
    
    @Column(name = "check_in_time")
    private LocalTime checkInTime;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
    
    @Column(name = "marked_at")
    private LocalDateTime markedAt;
    
    @Column(name = "marked_by")
    private Long markedBy;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = AttendanceStatus.ABSENT;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * Attendance status enum
     */
    public enum AttendanceStatus {
        PRESENT,  // Student attended on time
        ABSENT,   // Student did not attend
        LATE,     // Student attended but was late
        EXCUSED   // Student was excused (valid reason)
    }
    
    /**
     * Mark student as present
     */
    public void markPresent(LocalTime checkIn, Long markedByUserId) {
        this.status = AttendanceStatus.PRESENT;
        this.checkInTime = checkIn;
        this.markedAt = LocalDateTime.now();
        this.markedBy = markedByUserId;
    }
    
    /**
     * Mark student as late
     */
    public void markLate(LocalTime checkIn, Long markedByUserId) {
        this.status = AttendanceStatus.LATE;
        this.checkInTime = checkIn;
        this.markedAt = LocalDateTime.now();
        this.markedBy = markedByUserId;
    }
    
    /**
     * Mark student as absent
     */
    public void markAbsent(Long markedByUserId) {
        this.status = AttendanceStatus.ABSENT;
        this.checkInTime = null;
        this.markedAt = LocalDateTime.now();
        this.markedBy = markedByUserId;
    }
    
    /**
     * Mark student as excused
     */
    public void markExcused(String reason, Long markedByUserId) {
        this.status = AttendanceStatus.EXCUSED;
        this.notes = reason;
        this.markedAt = LocalDateTime.now();
        this.markedBy = markedByUserId;
    }
}
