package com.shrishailacademy.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for updating a Course. All fields are optional; only provided fields
 * will be updated.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseUpdateRequest {

    @Size(max = 100, message = "Title must not exceed 100 characters")
    private String title;

    private String description;

    @Size(max = 50, message = "Duration must not exceed 50 characters")
    private String duration;

    @Size(max = 50, message = "Icon must not exceed 50 characters")
    private String icon;

    @Size(max = 20, message = "Color must not exceed 20 characters")
    private String color;

    @DecimalMin(value = "0.00", message = "Fee must be non-negative")
    private BigDecimal fee;
}
