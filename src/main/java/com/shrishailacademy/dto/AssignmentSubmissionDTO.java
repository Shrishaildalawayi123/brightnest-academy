package com.shrishailacademy.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for assignment submissions and grading
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentSubmissionDTO {
    
    private Long id;
    
    @NotNull(message = "Assignment ID is required")
    private Long assignmentId;
    
    @NotNull(message = "Student ID is required")
    private Long studentId;
    
    @NotBlank(message = "Submission text is required")
    @Size(max = 5000, message = "Submission text must not exceed 5000 characters")
    private String submissionText;
    
    private String attachmentUrl;
    
    // Grading fields
    @Min(value = 0, message = "Score cannot be negative")
    private Integer score;
    
    @Size(max = 1000, message = "Feedback must not exceed 1000 characters")
    private String feedback;
    
    // Response-only fields
    private String assignmentTitle;
    private String courseName;
    private String studentName;
    private String studentEmail;
    private LocalDateTime submittedAt;
    private LocalDateTime gradedAt;
    private String gradedByName;
    private Boolean isLate;
    private Boolean isGraded;
    private String letterGrade;
    private Double percentageScore;
    private Integer maxScore;
}
