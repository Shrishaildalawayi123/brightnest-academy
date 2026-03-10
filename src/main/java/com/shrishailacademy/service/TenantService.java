package com.shrishailacademy.service;

import com.shrishailacademy.exception.ResourceNotFoundException;
import com.shrishailacademy.model.Tenant;
import com.shrishailacademy.repository.TenantRepository;
import com.shrishailacademy.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantService {

    public static final String DEFAULT_TENANT_KEY = "default";

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public Tenant requireCurrentTenant() {
        Long tenantId = TenantContext.requireTenantId();
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", "id", tenantId));
    }

    @Transactional
    public Tenant ensureDefaultTenantExists() {
        return tenantRepository.findByTenantKey(DEFAULT_TENANT_KEY)
                .orElseGet(() -> tenantRepository.save(new Tenant(null, DEFAULT_TENANT_KEY, "Default Tenant")));
    }
}
