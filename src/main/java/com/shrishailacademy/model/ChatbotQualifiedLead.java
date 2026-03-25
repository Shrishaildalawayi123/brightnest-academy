package com.shrishailacademy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chatbot_qualified_leads", indexes = {
        @Index(name = "idx_chatbot_qualified_tenant", columnList = "tenant_id"),
        @Index(name = "idx_chatbot_qualified_session", columnList = "session_id"),
        @Index(name = "idx_chatbot_qualified_status", columnList = "status"),
        @Index(name = "idx_chatbot_qualified_created", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotQualifiedLead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer"})
    private Tenant tenant;

    @NotBlank
    @Size(max = 80)
    @Column(name = "session_id", nullable = false, length = 80)
    private String sessionId;

    @Size(max = 100)
    @Column(name = "lead_name", length = 100)
    private String leadName;

    @Size(max = 100)
    @Column(length = 100)
    private String email;

    @Size(max = 20)
    @Column(length = 20)
    private String phone;

    @Size(max = 30)
    @Column(length = 30)
    private String grade;

    @Size(max = 50)
    @Column(length = 50)
    private String board;

    @Size(max = 100)
    @Column(name = "subject_interest", length = 100)
    private String subjectInterest;

    @Size(max = 500)
    @Column(name = "user_intent_message", length = 500)
    private String userIntentMessage;

    @Size(max = 1000)
    @Column(name = "recommended_plan", length = 1000)
    private String recommendedPlan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.NEW;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum Status {
        NEW, CONTACTED, ENROLLED, CLOSED
    }
}