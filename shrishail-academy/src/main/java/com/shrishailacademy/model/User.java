package com.shrishailacademy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * User Entity - Represents both Students and Admins
 * 
 * Roles:
 * - ADMIN: Can manage courses and view all students
 * - STUDENT: Can enroll in courses and view their dashboard
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @JsonIgnore
    @NotBlank(message = "Password is required")
    @Column(nullable = false, length = 255)
    private String password;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Role role = Role.STUDENT;

    @JsonIgnore
    @Column(name = "refresh_token", length = 255)
    private String refreshToken;

    @JsonIgnore
    @Column(name = "refresh_token_expiry")
    private LocalDateTime refreshTokenExpiry;

    // Relationships - @JsonIgnore prevents circular serialization
    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Enrollment> enrollments = new ArrayList<>();

    /**
     * User Roles
     */
    public enum Role {
        ADMIN,
        STUDENT
    }

    /**
     * Helper method to check if user is admin
     */
    public boolean isAdmin() {
        return this.role == Role.ADMIN;
    }

    /**
     * Helper method to check if user is student
     */
    public boolean isStudent() {
        return this.role == Role.STUDENT;
    }
}
