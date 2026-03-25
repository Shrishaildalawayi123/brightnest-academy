package com.shrishailacademy.dto;

import java.time.LocalDateTime;

public record CrmLeadDto(
        String source,
        Long sourceId,
        String sourceLabel,
        String leadName,
        String guardianName,
        String email,
        String phone,
        String subject,
        String grade,
        String board,
        String status,
        String assignee,
        String followUpStatus,
        LocalDateTime followUpAt,
        String followUpNotes,
        String summary,
        LocalDateTime createdAt) {
}