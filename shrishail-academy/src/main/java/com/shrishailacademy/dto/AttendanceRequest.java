package com.shrishailacademy.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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

        @NotNull(message = "Status is required")
        private String status; // PRESENT, ABSENT, LATE

        private String remarks;
    }
}
