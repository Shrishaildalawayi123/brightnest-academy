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
 * ContactMessage - Stores contact form submissions
 */
@Entity
@Table(name = "contact_messages")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Column(nullable = false, length = 100)
    private String email;

    @Size(max = 20)
    @Column(length = 20)
    private String phone;

    @NotBlank(message = "Subject is required")
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String subject;

    @NotBlank(message = "Message is required")
    @Size(max = 2000)
    @Column(nullable = false, length = 2000)
    private String message;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Status status = Status.NEW;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum Status {
        NEW, READ, REPLIED, ARCHIVED
    }
}
