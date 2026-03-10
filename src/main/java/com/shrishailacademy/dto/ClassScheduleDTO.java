package com.shrishailacademy.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * DTO for creating and updating class schedules
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassScheduleDTO {
    
    private Long id;
    
    @NotNull(message = "Course ID is required")
    private Long courseId;
    
    @NotNull(message = "Teacher ID is required")
    private Long teacherId;
    
    @NotNull(message = "Day of week is required")
    private DayOfWeek dayOfWeek;
    
    @NotNull(message = "Start time is required")
    private LocalTime startTime;
    
    @NotNull(message = "End time is required")
    private LocalTime endTime;
    
    @NotBlank(message = "Room number is required")
    @Size(max = 50, message = "Room number must not exceed 50 characters")
    private String roomNumber;
    
    @NotNull(message = "Maximum students is required")
    @Min(value = 1, message = "Must allow at least 1 student")
    @Max(value = 200, message = "Maximum students cannot exceed 200")
    private Integer maxStudents;
    
    private Boolean isActive = true;
    
    // Response-only fields
    private String courseName;
    private String teacherName;
    private String teacherEmail;
}
