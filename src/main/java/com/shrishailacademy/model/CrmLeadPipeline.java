package com.shrishailacademy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "crm_lead_pipeline", indexes = {
        @Index(name = "idx_crm_pipeline_tenant", columnList = "tenant_id"),
        @Index(name = "idx_crm_pipeline_assignee", columnList = "assignee"),
        @Index(name = "idx_crm_pipeline_follow_up_at", columnList = "follow_up_at"),
        @Index(name = "idx_crm_pipeline_unique", columnList = "tenant_id,source,source_id", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CrmLeadPipeline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer"})
    private Tenant tenant;

    @Column(nullable = false, length = 30)
    private String source;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Size(max = 120)
    @Column(length = 120)
    private String assignee;

    @Column(name = "follow_up_at")
    private LocalDateTime followUpAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "follow_up_status", nullable = false, length = 20)
    private FollowUpStatus followUpStatus = FollowUpStatus.NONE;

    @Size(max = 1000)
    @Column(name = "follow_up_notes", length = 1000)
    private String followUpNotes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum FollowUpStatus {
        NONE, PENDING, COMPLETED, MISSED
    }
}