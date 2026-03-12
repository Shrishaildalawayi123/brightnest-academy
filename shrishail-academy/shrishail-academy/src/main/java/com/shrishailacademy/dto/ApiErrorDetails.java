package com.shrishailacademy.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * Standard error details carried inside ApiResponse.data.
 * This keeps the outer {success,message,data} envelope stable while
 * providing consistent, debuggable error metadata.
 */
@Data
@Builder
public class ApiErrorDetails {
    private Instant timestamp;
    private int status;
    private String code;
    private String path;
    private String requestId;
    private Map<String, String> fieldErrors;
}
