package com.shrishailacademy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Enrollment Entity - Represents student-course enrollments
 * 
 * Tracks which students are enrolled in which courses
 */
@Entity
@Table(name = "enrollments", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "user_id", "course_id" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({ "enrollments", "password", "hibernateLazyInitializer" })
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @JsonIgnoreProperties({ "enrollments", "hibernateLazyInitializer" })
    private Course course;

    @CreationTimestamp
    @Column(name = "enrolled_at", updatable = false)
    private LocalDateTime enrolledAt;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    /**
     * Enrollment Status
     */
    public enum Status {
        ACTIVE,
        COMPLETED,
        CANCELLED
    }
}
