package com.shrishailacademy.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.DayOfWeek;

/**
 * ClassSchedule Entity - Represents recurring class schedule templates
 * Each schedule defines when a course class occurs (e.g., "Mathematics every Monday 10:00-11:30")
 */
@Entity
@Table(name = "class_schedules", indexes = {
    @Index(name = "idx_schedule_tenant", columnList = "tenant_id"),
    @Index(name = "idx_schedule_course", columnList = "course_id"),
    @Index(name = "idx_schedule_teacher", columnList = "teacher_id"),
    @Index(name = "idx_schedule_day", columnList = "dayOfWeek"),
    @Index(name = "idx_schedule_active", columnList = "isActive"),
    @Index(name = "idx_schedule_teacher_day_time", columnList = "teacher_id, dayOfWeek, startTime, endTime")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassSchedule {
    
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
    
    @Column(name = "day_of_week", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    @NotNull(message = "Day of week is required")
    private DayOfWeek dayOfWeek;
    
    @Column(name = "start_time", nullable = false)
    @NotNull(message = "Start time is required")
    private LocalTime startTime;
    
    @Column(name = "end_time", nullable = false)
    @NotNull(message = "End time is required")
    private LocalTime endTime;
    
    @Column(name = "room_number", length = 50)
    @Size(max = 50, message = "Room number must not exceed 50 characters")
    private String roomNumber;
    
    @Column(name = "max_students")
    @Min(value = 1, message = "Max students must be at least 1")
    @Max(value = 200, message = "Max students must not exceed 200")
    @Builder.Default
    private Integer maxStudents = 30;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "created_by")
    private Long createdBy;
    
    @Column(name = "updated_by")
    private Long updatedBy;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
        validateTimeOrder();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        validateTimeOrder();
    }
    
    private void validateTimeOrder() {
        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            throw new IllegalStateException("End time must be after start time");
        }
    }
    
    /**
     * Check if this schedule conflicts with another schedule (same teacher, overlapping time)
     */
    public boolean conflictsWith(ClassSchedule other) {
        if (!this.dayOfWeek.equals(other.dayOfWeek)) {
            return false;
        }
        
        if (!this.teacher.getId().equals(other.teacher.getId())) {
            return false;
        }
        
        // Check time overlap
        return this.startTime.isBefore(other.endTime) && other.startTime.isBefore(this.endTime);
    }
    
    /**
     * Get formatted time range (e.g., "10:00 AM - 11:30 AM")
     */
    public String getTimeRange() {
        return String.format("%s - %s", 
            startTime.format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a")),
            endTime.format(java.time.format.DateTimeFormatter.ofPattern("hh:mm a"))
        );
    }
}
