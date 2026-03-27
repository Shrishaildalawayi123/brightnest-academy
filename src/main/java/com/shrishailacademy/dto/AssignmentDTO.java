package com.shrishailacademy.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for assignment creation and updates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentDTO {
    
    private Long id;
    
    @NotNull(message = "Course ID is required")
    private Long courseId;
    
    @NotNull(message = "Teacher ID is required")
    private Long teacherId;
    
    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;
    
    @NotBlank(message = "Description is required")
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;
    
    @NotNull(message = "Due date is required")
    @Future(message = "Due date must be in the future")
    private LocalDateTime dueDate;
    
    @NotNull(message = "Maximum score is required")
    @Min(value = 1, message = "Maximum score must be at least 1")
    @Max(value = 1000, message = "Maximum score cannot exceed 1000")
    private Integer maxScore;
    
    private String attachmentUrl;
    
    @Default
    private Boolean isPublished = false;
    
    // Response-only fields
    private String courseName;
    private String teacherName;
    private Integer totalSubmissions;
    private Integer gradedSubmissions;
    private Double averageScore;
    private Boolean isOverdue;
}
