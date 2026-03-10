package com.shrishailacademy.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRequest {

    @NotNull(message = "Course ID is required")
    private Long courseId;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotEmpty(message = "At least one attendance record is required")
    private List<StudentAttendance> records;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentAttendance {
        @NotNull(message = "Student ID is required")
        private Long studentId;

        @NotBlank(message = "Status is required")
        @Size(max = 32, message = "Status must be at most 32 characters")
        @Pattern(regexp = "(?i)^(PRESENT|ABSENT|LATE)$", message = "Status must be PRESENT, ABSENT, or LATE")
        private String status; // PRESENT, ABSENT, LATE

        @Size(max = 500, message = "Remarks must be at most 500 characters")
        private String remarks;
    }
}
