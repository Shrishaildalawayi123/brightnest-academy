package com.shrishailacademy.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/**
 * Tenant represents a single academy/customer in the SaaS platform.
 *
 * tenantKey is the external identifier used by subdomain or X-Tenant-ID header.
 */
@Entity
@Table(name = "tenants", indexes = {
        @Index(name = "idx_tenant_key", columnList = "tenant_key", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tenant extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 50)
    @Column(name = "tenant_key", nullable = false, length = 50, unique = true)
    private String tenantKey;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String name;

    @Size(max = 255)
    @Column(length = 255)
    private String domain;

    @Size(max = 50)
    @Column(length = 50)
    private String plan;

    /**
     * Convenience constructor for backward compatibility (id, tenantKey, name).
     */
    public Tenant(Long id, String tenantKey, String name) {
        this.id = id;
        this.tenantKey = tenantKey;
        this.name = name;
    }
}
