package com.shrishailacademy.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for public educator recruitment form submission
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherApplicationRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 100)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email")
    private String email;

    @NotBlank(message = "Phone number is required")
    @Size(max = 20)
    private String phone;

    @NotBlank(message = "Subject expertise is required")
    @Size(max = 200)
    private String subjectExpertise;

    @Size(max = 200)
    private String qualification;

    @Size(max = 100)
    private String city;

    @Size(max = 20)
    private String teachingMode;

    @Size(max = 500)
    private String experience;

    @Size(max = 1000)
    private String motivation;
}
