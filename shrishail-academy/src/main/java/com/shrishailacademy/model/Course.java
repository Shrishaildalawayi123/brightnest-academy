package com.shrishailacademy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courses", uniqueConstraints = {
        @UniqueConstraint(name = "uk_course_tenant_title", columnNames = { "tenant_id", "title" }),
        @UniqueConstraint(name = "uk_course_tenant_subject_key", columnNames = { "tenant_id", "subject_key" })
}, indexes = {
        @Index(name = "idx_course_tenant", columnList = "tenant_id"),
        @Index(name = "idx_course_tenant_title", columnList = "tenant_id,title"),
        @Index(name = "idx_course_tenant_subject_key", columnList = "tenant_id,subject_key"),
        @Index(name = "idx_course_teacher", columnList = "teacher_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Course extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer" })
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    @JsonIgnoreProperties({ "enrollments", "password", "hibernateLazyInitializer" })
    private User teacher;

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

    @Column(name = "subject_key", length = 50)
    private String subjectKey;

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
