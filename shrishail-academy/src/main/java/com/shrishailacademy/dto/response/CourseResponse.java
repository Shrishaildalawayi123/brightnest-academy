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
    private BigDecimal fee;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

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
                .fee(course.getFee())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }
}
