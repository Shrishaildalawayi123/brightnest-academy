package com.shrishailacademy.dto.response;

import com.shrishailacademy.model.TeacherApplication;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherApplicationResponse {

    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String subjectExpertise;
    private String qualification;
    private String city;
    private String teachingMode;
    private String experience;
    private String motivation;
    private String resumeFileName;
    private String status;
    private LocalDateTime createdAt;

    public static TeacherApplicationResponse fromEntity(TeacherApplication application) {
        if (application == null) {
            return null;
        }

        return TeacherApplicationResponse.builder()
                .id(application.getId())
                .fullName(application.getFullName())
                .email(application.getEmail())
                .phone(application.getPhone())
                .subjectExpertise(application.getSubjectExpertise())
                .qualification(application.getQualification())
                .city(application.getCity())
                .teachingMode(application.getTeachingMode())
                .experience(application.getExperience())
                .motivation(application.getMotivation())
                .resumeFileName(application.getResumeFileName())
                .status(application.getStatus() != null ? application.getStatus().name() : null)
                .createdAt(application.getCreatedAt())
                .build();
    }
}