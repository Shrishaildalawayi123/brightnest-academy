package com.shrishailacademy.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * TeacherApplication - Educator recruitment form submissions
 */
@Entity
@Table(name = "teacher_applications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Full name is required")
    @Size(max = 100)
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Column(nullable = false, length = 100)
    private String email;

    @NotBlank(message = "Phone number is required")
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String phone;

    @NotBlank(message = "Subject expertise is required")
    @Size(max = 200)
    @Column(name = "subject_expertise", nullable = false, length = 200)
    private String subjectExpertise;

    @Size(max = 200)
    @Column(length = 200)
    private String qualification;

    @Size(max = 100)
    @Column(length = 100)
    private String city;

    @Column(name = "teaching_mode", length = 20)
    private String teachingMode;

    @Size(max = 500)
    @Column(length = 500)
    private String experience;

    @Size(max = 1000)
    @Column(length = 1000)
    private String motivation;

    @Size(max = 255)
    @Column(name = "resume_file_name", length = 255)
    private String resumeFileName;

    @Size(max = 500)
    @Column(name = "resume_file_path", length = 500)
    private String resumeFilePath;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Status status = Status.NEW;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum Status {
        NEW, REVIEWED, CONTACTED, HIRED, REJECTED
    }
}
