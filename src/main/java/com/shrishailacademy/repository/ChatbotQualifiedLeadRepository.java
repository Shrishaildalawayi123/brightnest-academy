package com.shrishailacademy.repository;

import com.shrishailacademy.model.ChatbotQualifiedLead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatbotQualifiedLeadRepository extends JpaRepository<ChatbotQualifiedLead, Long> {

    List<ChatbotQualifiedLead> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    java.util.Optional<ChatbotQualifiedLead> findFirstByTenantIdAndSessionIdOrderByCreatedAtDesc(Long tenantId,
            String sessionId);

    long countByTenantId(Long tenantId);

    Optional<ChatbotQualifiedLead> findByIdAndTenantId(Long id, Long tenantId);
}