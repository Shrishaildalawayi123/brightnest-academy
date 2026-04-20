package com.shrishailacademy.dto;

import jakarta.validation.constraints.NotBlank;

public record RoleChangeRequest(
        @NotBlank(message = "Role is required") String role) {
}
