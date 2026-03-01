package com.shrishailacademy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Course extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Course title is required")
    @Size(max = 100, message = "Title must not exceed 100 characters")
    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String duration;

    @Column(length = 50)
    private String icon;

    @Column(length = 20)
    private String color;

    @NotNull(message = "Course fee is required")
    @DecimalMin(value = "0.00", message = "Fee must be non-negative")
    @Column(name = "fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal fee;

    // Break circular reference - don't serialize enrollments list in course
    // responses
    @JsonIgnore
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Enrollment> enrollments = new ArrayList<>();

    @JsonIgnore
    public int getEnrollmentCount() {
        return 0; // suppressed to prevent lazy load during JSON serialization
    }
}
