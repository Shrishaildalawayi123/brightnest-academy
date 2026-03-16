package com.shrishailacademy.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Tracks marketing WhatsApp click-throughs for lead attribution.
 */
@Entity
@Table(name = "whatsapp_leads", indexes = {
        @Index(name = "idx_whatsapp_timestamp", columnList = "timestamp"),
        @Index(name = "idx_whatsapp_source_page", columnList = "source_page")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WhatsappLead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @NotBlank
    @Size(max = 200)
    @Column(name = "source_page", nullable = false, length = 200)
    private String sourcePage;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 20)
    private DeviceType deviceType;

    public enum DeviceType {
        DESKTOP, MOBILE, TABLET
    }
}
