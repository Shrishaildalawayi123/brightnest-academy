package com.shrishailacademy.dto;

import com.shrishailacademy.model.SessionAttendance;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * DTO for attendance marking and retrieval
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceDTO {
    
    private Long id;
    
    @NotNull(message = "Session ID is required")
    private Long sessionId;
    
    @NotNull(message = "Student ID is required")
    private Long studentId;
    
    @NotNull(message = "Attendance status is required")
    private SessionAttendance.AttendanceStatus status;
    
    private LocalTime checkInTime;
    
    @Size(max = 200, message = "Notes must not exceed 200 characters")
    private String notes;
    
    // Response-only fields
    private String studentName;
    private String studentEmail;
    private String courseName;
    private LocalDateTime markedAt;
    private String markedByName;
}
