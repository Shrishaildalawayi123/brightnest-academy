package com.shrishailacademy.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Validates critical environment variables at startup.
 * Active only in the "prod" profile — prevents launch with insecure defaults.
 */
@Configuration
@Profile("prod")
public class EnvironmentValidator {

    private static final Logger log = LoggerFactory.getLogger(EnvironmentValidator.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String datasourceUsername;

    @Value("${spring.datasource.password}")
    private String datasourcePassword;

    @Value("${security.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${spring.jpa.hibernate.ddl-auto:update}")
    private String ddlAuto;

    @Value("${admin.email:}")
    private String adminEmail;

    @Value("${admin.password:}")
    private String adminPassword;

    @PostConstruct
    public void validate() {
        log.info("Validating production environment configuration...");

        // JWT secret must not use the dev default and must be >=64 chars for HS512
        if ("BrightNestAcademy-Dev-Secret-Key-2026-CHANGE-THIS-IN-PRODUCTION-64chars!!".equals(jwtSecret)
                || jwtSecret == null || jwtSecret.length() < 64) {
            throw new IllegalStateException(
                    "FATAL: JWT_SECRET is not set or too short. HS512 requires >=64 character secret. Current length: "
                            + (jwtSecret == null ? 0 : jwtSecret.length()));
        }

        // Database should not be using default dev credentials
        if ("root".equals(datasourceUsername) && "root".equals(datasourcePassword)) {
            throw new IllegalStateException(
                    "FATAL: Database is using default 'root/root' credentials. Configure dedicated DB_USER/DB_PASS.");
        }

        // Database URL must use SSL in production
        if (datasourceUrl != null && !datasourceUrl.contains("useSSL=true")) {
            log.warn(
                    "\u26a0 Database connection does not enforce SSL. Add useSSL=true&requireSSL=true to datasource URL.");
        }

        // Ensure cookies are secure over HTTPS
        if (!cookieSecure) {
            throw new IllegalStateException(
                    "FATAL: security.cookie.secure=false in production. Set COOKIE_SECURE=true.");
        }

        // DDL auto should be 'validate' or 'none' in prod
        if ("update".equals(ddlAuto) || "create".equals(ddlAuto) || "create-drop".equals(ddlAuto)) {
            throw new IllegalStateException(
                    "FATAL: spring.jpa.hibernate.ddl-auto='" + ddlAuto
                            + "' is unsafe for production. Use 'validate' or 'none'.");
        }

        // Verify the datasource URL is not pointing to localhost
        if (datasourceUrl != null && (datasourceUrl.contains("127.0.0.1") || datasourceUrl.contains("localhost"))) {
            log.warn("⚠ Database URL points to localhost/127.0.0.1. Ensure this is intentional for production.");
        }

        // Admin credentials must not use dev defaults in production
        if ("admin@brightnest.com".equals(adminEmail) || "Admin@123".equals(adminPassword)) {
            log.warn(
                    "\u26a0 Admin credentials appear to be dev defaults. Set strong ADMIN_EMAIL/ADMIN_PASSWORD env vars.");
        }

        log.info("Production environment validation complete — all critical checks passed.");
    }
}
