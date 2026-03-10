package com.shrishailacademy.repository;

import com.shrishailacademy.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Audit Log Repository - Data access for audit trail records.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // ========== TENANT-AWARE QUERIES ==========

    Page<AuditLog> findByTenantIdOrderByTimestampDesc(Long tenantId, Pageable pageable);

    Page<AuditLog> findByTenantIdAndUserId(Long tenantId, Long userId, Pageable pageable);

    Page<AuditLog> findByTenantIdAndAction(Long tenantId, String action, Pageable pageable);

    List<AuditLog> findByTenantIdAndTimestampBetween(Long tenantId, LocalDateTime start, LocalDateTime end);

    long countByTenantIdAndActionAndTimestampAfter(Long tenantId, String action, LocalDateTime after);

    // ========== LEGACY (non-tenant) ==========

    Page<AuditLog> findByUserId(Long userId, Pageable pageable);

    Page<AuditLog> findByAction(String action, Pageable pageable);

    List<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);

    long countByActionAndTimestampAfter(String action, LocalDateTime after);
}
