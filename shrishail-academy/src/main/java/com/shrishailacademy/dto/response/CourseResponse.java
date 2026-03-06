package com.shrishailacademy.dto.response;

import com.shrishailacademy.model.Course;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for Course responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponse {

    private Long id;
    private String title;
    private String description;
    private String duration;
    private String icon;
    private String color;
    private String subjectKey;
    private BigDecimal fee;
    private TeacherSummary teacher;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherSummary {
        private Long id;
        private String name;
        private String email;
        private String role;
    }

    public static CourseResponse fromEntity(Course course) {
        if (course == null)
            return null;
        return CourseResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .description(course.getDescription())
                .duration(course.getDuration())
                .icon(course.getIcon())
                .color(course.getColor())
                .subjectKey(course.getSubjectKey())
                .fee(course.getFee())
                .teacher(toTeacherSummary(course))
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }

    private static TeacherSummary toTeacherSummary(Course course) {
        if (course.getTeacher() == null) {
            return null;
        }
        return TeacherSummary.builder()
                .id(course.getTeacher().getId())
                .name(course.getTeacher().getName())
                .email(course.getTeacher().getEmail())
                .role(course.getTeacher().getRole() != null ? course.getTeacher().getRole().name() : null)
                .build();
    }
}
