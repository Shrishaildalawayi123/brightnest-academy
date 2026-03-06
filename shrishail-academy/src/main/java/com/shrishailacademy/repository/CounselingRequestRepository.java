package com.shrishailacademy.repository;

import com.shrishailacademy.model.CounselingRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CounselingRequestRepository extends JpaRepository<CounselingRequest, Long> {

    // ========== TENANT-AWARE QUERIES ==========

    List<CounselingRequest> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    List<CounselingRequest> findByTenantIdAndStatusOrderByCreatedAtDesc(Long tenantId, CounselingRequest.Status status);

    long countByTenantIdAndStatus(Long tenantId, CounselingRequest.Status status);

    long countByTenantId(Long tenantId);

    Optional<CounselingRequest> findByIdAndTenantId(Long id, Long tenantId);

    // ========== LEGACY (non-tenant) ==========

    List<CounselingRequest> findAllByOrderByCreatedAtDesc();

    List<CounselingRequest> findByStatusOrderByCreatedAtDesc(CounselingRequest.Status status);

    long countByStatus(CounselingRequest.Status status);
}
