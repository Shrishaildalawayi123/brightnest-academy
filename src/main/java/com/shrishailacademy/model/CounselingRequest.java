package com.shrishailacademy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * CounselingRequest - Stores free counseling callback requests from the home
 * page form.
 */
@Entity
@Table(name = "counseling_requests", indexes = {
        @Index(name = "idx_counseling_tenant", columnList = "tenant_id"),
        @Index(name = "idx_counseling_status", columnList = "status"),
        @Index(name = "idx_counseling_date", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CounselingRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer"})
    private Tenant tenant;

    @NotBlank(message = "Student name is required")
    @Size(max = 100)
    @Column(name = "student_name", nullable = false, length = 100)
    private String studentName;

    @NotBlank(message = "Class/Grade is required")
    @Size(max = 30)
    @Column(name = "student_class", nullable = false, length = 30)
    private String studentClass;

    @NotBlank(message = "Board is required")
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String board;

    @NotBlank(message = "Phone number is required")
    @Size(max = 20)
    @Pattern(regexp = "^[+]?[0-9\\s-]{7,20}$", message = "Please enter a valid phone number")
    @Column(name = "parent_phone", nullable = false, length = 20)
    private String parentPhone;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Status status = Status.NEW;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum Status {
        NEW, CONTACTED, COMPLETED, CANCELLED
    }
}
