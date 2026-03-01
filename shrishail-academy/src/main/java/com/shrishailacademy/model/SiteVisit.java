package com.shrishailacademy.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Site Visit Entity - Tracks visitor analytics.
 * Records page visits for conversion and traffic analysis.
 */
@Entity
@Table(name = "site_visits", indexes = {
        @Index(name = "idx_visit_session", columnList = "session_id"),
        @Index(name = "idx_visit_page", columnList = "page_url"),
        @Index(name = "idx_visit_date", columnList = "visited_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SiteVisit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;

    @Column(name = "page_url", nullable = false, length = 500)
    private String pageUrl;

    @Column(length = 500)
    private String referrer;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "visited_at", nullable = false, updatable = false)
    private LocalDateTime visitedAt;
}
