package com.shrishailacademy.repository;

import com.shrishailacademy.model.TeacherApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherApplicationRepository extends JpaRepository<TeacherApplication, Long> {

    // ========== TENANT-AWARE QUERIES ==========

    List<TeacherApplication> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    List<TeacherApplication> findByTenantIdAndStatusOrderByCreatedAtDesc(Long tenantId, TeacherApplication.Status status);

    long countByTenantIdAndStatus(Long tenantId, TeacherApplication.Status status);

    long countByTenantId(Long tenantId);

    Optional<TeacherApplication> findByIdAndTenantId(Long id, Long tenantId);

    // ========== LEGACY (non-tenant) ==========

    List<TeacherApplication> findAllByOrderByCreatedAtDesc();

    List<TeacherApplication> findByStatusOrderByCreatedAtDesc(TeacherApplication.Status status);

    long countByStatus(TeacherApplication.Status status);
}
