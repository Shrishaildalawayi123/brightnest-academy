package com.shrishailacademy.dto;

import java.time.Instant;

/**
 * Standard API error response.
 *
 * Shape:
 * {
 * "timestamp": "",
 * "status": 400,
 * "error": "Validation Error",
 * "message": "Email is invalid"
 * }
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message) {
}
