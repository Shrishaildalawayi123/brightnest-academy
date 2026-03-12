package com.shrishailacademy.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for creating a Course.
 * Keeps controllers from accepting JPA entities directly.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseCreateRequest {

    @NotBlank(message = "Course title is required")
    @Size(max = 100, message = "Title must not exceed 100 characters")
    private String title;

    private String description;

    @Size(max = 50, message = "Duration must not exceed 50 characters")
    private String duration;

    @Size(max = 50, message = "Icon must not exceed 50 characters")
    private String icon;

    @Size(max = 20, message = "Color must not exceed 20 characters")
    private String color;

    @Size(max = 50, message = "Subject key must not exceed 50 characters")
    private String subjectKey;

    private Long teacherId;

    @NotNull(message = "Course fee is required")
    @DecimalMin(value = "0.00", message = "Fee must be non-negative")
    private BigDecimal fee;
}
