package com.shrishailacademy.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CrmLeadPipelineUpdateRequest(
        @Size(max = 120, message = "Assignee must be 120 characters or fewer")
        String assignee,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm")
        LocalDateTime followUpAt,

        @Size(max = 20, message = "Follow-up status must be 20 characters or fewer")
        String followUpStatus,

        @Size(max = 1000, message = "Follow-up notes must be 1000 characters or fewer")
        String followUpNotes) {
}