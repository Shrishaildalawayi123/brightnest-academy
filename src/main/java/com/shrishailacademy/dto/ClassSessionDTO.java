package com.shrishailacademy.dto;

import com.shrishailacademy.model.ClassSession;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for class session operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassSessionDTO {
    
    private Long id;
    
    @NotNull(message = "Schedule ID is required")
    private Long scheduleId;
    
    @NotNull(message = "Session date is required")
    private LocalDate sessionDate;
    
    private ClassSession.SessionStatus status;
    
    private LocalTime actualStartTime;
    
    private LocalTime actualEndTime;
    
    private Boolean attendanceMarked;
    
    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
    
    @Size(max = 200, message = "Cancellation reason must not exceed 200 characters")
    private String cancellationReason;
    
    // Response-only fields
    private String courseName;
    private String teacherName;
    private String roomNumber;
    private LocalTime scheduledStartTime;
    private LocalTime scheduledEndTime;
    private Integer totalStudents;
    private Integer presentCount;
}
