package com.shrishailacademy.config;

import com.shrishailacademy.service.TenantService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class TenantBootstrap implements ApplicationRunner {

    private final TenantService tenantService;

    public TenantBootstrap(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @Override
    public void run(ApplicationArguments args) {
        tenantService.ensureDefaultTenantExists();
    }
}
