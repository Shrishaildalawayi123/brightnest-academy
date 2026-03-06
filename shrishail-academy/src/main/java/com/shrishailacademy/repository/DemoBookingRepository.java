package com.shrishailacademy.repository;

import com.shrishailacademy.model.DemoBooking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DemoBookingRepository extends JpaRepository<DemoBooking, Long> {

    // ========== TENANT-AWARE QUERIES ==========

    List<DemoBooking> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    List<DemoBooking> findByTenantIdAndStatusOrderByCreatedAtDesc(Long tenantId, DemoBooking.Status status);

    long countByTenantIdAndStatus(Long tenantId, DemoBooking.Status status);

    long countByTenantId(Long tenantId);

    Optional<DemoBooking> findByIdAndTenantId(Long id, Long tenantId);

    // ========== LEGACY (non-tenant) ==========

    List<DemoBooking> findAllByOrderByCreatedAtDesc();

    List<DemoBooking> findByStatusOrderByCreatedAtDesc(DemoBooking.Status status);

    long countByStatus(DemoBooking.Status status);
}
