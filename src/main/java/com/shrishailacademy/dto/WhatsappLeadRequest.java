package com.shrishailacademy.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for WhatsApp click attribution payloads from the public site.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WhatsappLeadRequest {

    @Size(max = 200, message = "Source page must be 200 characters or fewer")
    private String sourcePage;
}
