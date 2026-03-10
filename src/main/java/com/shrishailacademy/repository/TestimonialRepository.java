package com.shrishailacademy.repository;

import com.shrishailacademy.model.Testimonial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TestimonialRepository extends JpaRepository<Testimonial, Long> {

    // ========== TENANT-AWARE QUERIES ==========

    List<Testimonial> findByTenantIdAndApprovedTrueOrderByCreatedAtDesc(Long tenantId);

    List<Testimonial> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    long countByTenantIdAndApproved(Long tenantId, boolean approved);

    Optional<Testimonial> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByIdAndTenantId(Long id, Long tenantId);

    void deleteByIdAndTenantId(Long id, Long tenantId);

    // ========== LEGACY (non-tenant) ==========

    List<Testimonial> findByApprovedTrueOrderByCreatedAtDesc();

    List<Testimonial> findAllByOrderByCreatedAtDesc();

    long countByApproved(boolean approved);
}
