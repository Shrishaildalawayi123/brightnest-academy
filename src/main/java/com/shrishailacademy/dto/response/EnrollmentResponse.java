package com.shrishailacademy.dto.response;

import com.shrishailacademy.model.Enrollment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for Enrollment responses - flattens user/course references.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentResponse {

    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private Long courseId;
    private String courseTitle;
    private String courseDuration;
    private BigDecimal courseFee;
    private String status;
    private LocalDateTime enrolledAt;

    public static EnrollmentResponse fromEntity(Enrollment enrollment) {
        if (enrollment == null)
            return null;
        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .userId(enrollment.getUser() != null ? enrollment.getUser().getId() : null)
                .userName(enrollment.getUser() != null ? enrollment.getUser().getName() : null)
                .userEmail(enrollment.getUser() != null ? enrollment.getUser().getEmail() : null)
                .courseId(enrollment.getCourse() != null ? enrollment.getCourse().getId() : null)
                .courseTitle(enrollment.getCourse() != null ? enrollment.getCourse().getTitle() : null)
                .courseDuration(enrollment.getCourse() != null ? enrollment.getCourse().getDuration() : null)
                .courseFee(enrollment.getCourse() != null ? enrollment.getCourse().getFee() : null)
                .status(enrollment.getStatus() != null ? enrollment.getStatus().name() : null)
                .enrolledAt(enrollment.getEnrolledAt())
                .build();
    }
}
