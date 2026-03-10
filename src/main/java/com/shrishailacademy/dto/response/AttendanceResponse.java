package com.shrishailacademy.dto.response;

import com.shrishailacademy.model.Attendance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for Attendance responses - flattens user/course references.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponse {

    private Long id;
    private Long userId;
    private String userName;
    private Long courseId;
    private String courseTitle;
    private LocalDate attendanceDate;
    private String status;
    private String remarks;
    private Long markedById;
    private String markedByName;
    private LocalDateTime createdAt;

    public static AttendanceResponse fromEntity(Attendance attendance) {
        if (attendance == null)
            return null;
        return AttendanceResponse.builder()
                .id(attendance.getId())
                .userId(attendance.getUser() != null ? attendance.getUser().getId() : null)
                .userName(attendance.getUser() != null ? attendance.getUser().getName() : null)
                .courseId(attendance.getCourse() != null ? attendance.getCourse().getId() : null)
                .courseTitle(attendance.getCourse() != null ? attendance.getCourse().getTitle() : null)
                .attendanceDate(attendance.getAttendanceDate())
                .status(attendance.getStatus() != null ? attendance.getStatus().name() : null)
                .remarks(attendance.getRemarks())
                .markedById(attendance.getMarkedBy() != null ? attendance.getMarkedBy().getId() : null)
                .markedByName(attendance.getMarkedBy() != null ? attendance.getMarkedBy().getName() : null)
                .createdAt(attendance.getCreatedAt())
                .build();
    }
}
