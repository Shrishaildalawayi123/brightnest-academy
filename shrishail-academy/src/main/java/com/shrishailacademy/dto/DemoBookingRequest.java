package com.shrishailacademy.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for public demo booking form submission
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemoBookingRequest {

    @NotBlank(message = "Student name is required")
    @Size(max = 100)
    private String studentName;

    @Size(max = 100)
    private String parentName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Size(max = 20)
    private String phone;

    @NotBlank(message = "Subject is required")
    @Size(max = 50)
    private String subject;

    @Size(max = 30)
    private String grade;

    @Size(max = 30)
    private String board;

    @NotBlank(message = "Class mode is required")
    private String classMode;

    @Size(max = 500)
    private String requirements;

    @Size(max = 1000)
    private String message;
}
