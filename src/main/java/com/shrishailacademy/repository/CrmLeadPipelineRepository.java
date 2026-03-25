package com.shrishailacademy.repository;

import com.shrishailacademy.model.CrmLeadPipeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CrmLeadPipelineRepository extends JpaRepository<CrmLeadPipeline, Long> {

    List<CrmLeadPipeline> findByTenantId(Long tenantId);

    Optional<CrmLeadPipeline> findByTenantIdAndSourceAndSourceId(Long tenantId, String source, Long sourceId);
}