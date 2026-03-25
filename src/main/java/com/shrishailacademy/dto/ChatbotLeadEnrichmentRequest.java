package com.shrishailacademy.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatbotLeadEnrichmentRequest(
        @NotBlank(message = "Session ID is required")
        @Size(max = 80, message = "Session ID must be 80 characters or fewer")
        String sessionId,

        @Size(max = 100, message = "Name must be 100 characters or fewer")
        String name,

        @Email(message = "Please provide a valid email")
        @Size(max = 100, message = "Email must be 100 characters or fewer")
        String email,

        @Size(max = 20, message = "Phone must be 20 characters or fewer")
        String phone,

        @Size(max = 30, message = "Class must be 30 characters or fewer")
        String studentClass) {
}