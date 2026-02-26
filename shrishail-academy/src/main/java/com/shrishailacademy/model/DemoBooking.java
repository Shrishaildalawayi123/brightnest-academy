package com.shrishailacademy.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * DemoBooking - Demo class booking requests from prospective students
 */
@Entity
@Table(name = "demo_bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemoBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Student name is required")
    @Size(max = 100)
    @Column(name = "student_name", nullable = false, length = 100)
    private String studentName;

    @Size(max = 100)
    @Column(name = "parent_name", length = 100)
    private String parentName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Column(nullable = false, length = 100)
    private String email;

    @NotBlank(message = "Phone number is required")
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String phone;

    @NotBlank(message = "Subject is required")
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String subject;

    @Size(max = 30)
    @Column(length = 30)
    private String grade;

    @Size(max = 30)
    @Column(length = 30)
    private String board;

    @Column(name = "class_mode", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ClassMode classMode = ClassMode.ONLINE;

    @Size(max = 500)
    @Column(length = 500)
    private String requirements;

    @Size(max = 1000)
    @Column(length = 1000)
    private String message;

    @Column(name = "demo_fee", nullable = false)
    private int demoFee = 100;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    @Column(name = "scheduled_date")
    private LocalDateTime scheduledDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum ClassMode {
        ONLINE, OFFLINE
    }

    public enum Status {
        PENDING, SCHEDULED, COMPLETED, CANCELLED
    }
}
