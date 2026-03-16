package com.shrishailacademy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for public chatbot prompts.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotRequest {

    @NotBlank(message = "Message is required")
    @Size(max = 500, message = "Message must be 500 characters or fewer")
    private String message;
}
