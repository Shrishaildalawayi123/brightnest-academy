package com.shrishailacademy.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestimonialRequest {

    @NotBlank(message = "Student name is required")
    @Size(max = 100, message = "Student name must not exceed 100 characters")
    private String studentName;

    @Size(max = 100, message = "Course name must not exceed 100 characters")
    private String courseName;

    @NotBlank(message = "Review text is required")
    @Size(max = 1000, message = "Review must not exceed 1000 characters")
    private String review;

    @Min(value = 1, message = "Rating must be between 1 and 5")
    @Max(value = 5, message = "Rating must be between 1 and 5")
    private int rating = 5;
}
