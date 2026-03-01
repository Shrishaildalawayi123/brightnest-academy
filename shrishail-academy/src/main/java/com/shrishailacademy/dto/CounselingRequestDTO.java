package com.shrishailacademy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for counseling callback form submissions (public endpoint)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CounselingRequestDTO {

    @NotBlank(message = "Student name is required")
    @Size(min = 2, max = 100, message = "Student name must be between 2 and 100 characters")
    private String studentName;

    @NotBlank(message = "Class/Grade is required")
    @Size(min = 1, max = 30, message = "Class/Grade must be between 1 and 30 characters")
    private String studentClass;

    @NotBlank(message = "Board is required")
    @Size(min = 2, max = 50, message = "Board must be between 2 and 50 characters")
    private String board;

    @NotBlank(message = "Phone number is required")
    @Size(min = 7, max = 20, message = "Phone number must be between 7 and 20 characters")
    @Pattern(regexp = "^[+]?[0-9\\s-]{7,20}$", message = "Please enter a valid phone number")
    private String parentPhone;
}
