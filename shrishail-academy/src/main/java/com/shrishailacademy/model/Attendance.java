package com.shrishailacademy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Attendance Entity - Tracks student attendance for enrolled courses
 */
@Entity
@Table(name = "attendance", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "course_id", "attendance_date" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({ "enrollments", "password", "hibernateLazyInitializer" })
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    @JsonIgnoreProperties({ "enrollments", "hibernateLazyInitializer" })
    private Course course;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status = Status.PRESENT;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "marked_by")
    @JsonIgnoreProperties({ "enrollments", "password", "hibernateLazyInitializer" })
    private User markedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Attendance Status
     */
    public enum Status {
        PRESENT,
        ABSENT,
        LATE
    }
}
