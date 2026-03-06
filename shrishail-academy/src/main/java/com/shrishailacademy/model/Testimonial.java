package com.shrishailacademy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Testimonial - Student reviews visible on the website
 */
@Entity
@Table(name = "testimonials", indexes = {
        @Index(name = "idx_testimonial_tenant", columnList = "tenant_id"),
        @Index(name = "idx_testimonial_approved", columnList = "approved")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Testimonial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer"})
    private Tenant tenant;

    @NotBlank(message = "Student name is required")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String studentName;

    @Size(max = 100)
    @Column(length = 100)
    private String courseName;

    @NotBlank(message = "Review text is required")
    @Size(max = 1000)
    @Column(nullable = false, length = 1000)
    private String review;

    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private int rating = 5;

    @Column(nullable = false)
    private boolean approved = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
