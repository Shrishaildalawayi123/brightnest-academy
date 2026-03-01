package com.shrishailacademy.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import java.util.Arrays;

/**
 * Fail-fast checks to prevent deploying with unsafe default secrets.
 * Only applies when the 'prod' Spring profile is active.
 */
@Component
public class ProductionSafetyValidator {

    private static final Logger log = LoggerFactory.getLogger(ProductionSafetyValidator.class);

    private static final String DEFAULT_DEV_JWT_PREFIX = "BrightNestAcademy-Dev-Secret-Key";
    private static final String DEFAULT_ADMIN_PASSWORD = "Admin@123";

    private final Environment environment;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${admin.password:}")
    private String adminPassword;

    public ProductionSafetyValidator(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validate() {
        if (!Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            return;
        }

        if (jwtSecret == null || jwtSecret.isBlank() || jwtSecret.startsWith(DEFAULT_DEV_JWT_PREFIX)) {
            throw new IllegalStateException("Refusing to start in prod with default/blank JWT secret. Set JWT_SECRET.");
        }

        if (DEFAULT_ADMIN_PASSWORD.equals(adminPassword)) {
            throw new IllegalStateException(
                    "Refusing to start in prod with default admin password. Set ADMIN_PASSWORD.");
        }

        log.info("ProductionSafetyValidator: basic secret checks passed.");
    }
}
