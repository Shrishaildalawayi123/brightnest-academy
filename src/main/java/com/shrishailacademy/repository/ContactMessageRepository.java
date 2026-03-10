package com.shrishailacademy.repository;

import com.shrishailacademy.model.ContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {

    // ========== TENANT-AWARE QUERIES ==========

    List<ContactMessage> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    List<ContactMessage> findByTenantIdAndStatusOrderByCreatedAtDesc(Long tenantId, ContactMessage.Status status);

    long countByTenantIdAndStatus(Long tenantId, ContactMessage.Status status);

    long countByTenantId(Long tenantId);

    Optional<ContactMessage> findByIdAndTenantId(Long id, Long tenantId);

    // ========== LEGACY (non-tenant) ==========

    List<ContactMessage> findAllByOrderByCreatedAtDesc();

    List<ContactMessage> findByStatusOrderByCreatedAtDesc(ContactMessage.Status status);

    long countByStatus(ContactMessage.Status status);
}
