package com.shrishailacademy.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProgressDto {
    private Long courseId;
    private String courseName;
    private Long totalSessions;
    private Long attendedSessions;
    private Double attendancePercent;
    private Long assignmentsTotal;
    private Long assignmentsSubmitted;
    private Double averageScore;
}
