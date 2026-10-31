package com.shrishailacademy.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

class ProductionConfigurationTest {
    EnvironmentValidator valid() {
        var v = new EnvironmentValidator();
        set(v, "jwtSecret", "x".repeat(64));
        set(v, "datasourceUrl", "jdbc:mysql://db/brightnest?useSSL=true&requireSSL=true");
        set(v, "datasourceUsername", "app");
        set(v, "datasourcePassword", "test-only");
        set(v, "cookieSecure", true);
        set(v, "ddlAuto", "validate");
        set(v, "rateLimitBackend", "redis");
        set(v, "resumeS3Enabled", true);
        set(v, "resumeS3Bucket", "test-bucket");
        set(v, "adminEmail", "");
        set(v, "adminPassword", "");
        return v;
    }

    void set(Object target, String field, Object value) {
        ReflectionTestUtils.setField(target, field, value);
    }

    @Test
    void safeProductionConfigAllowsStartupWithAndWithoutExplicitAdminBootstrap() {
        var v = valid();
        assertDoesNotThrow(v::validate);
        set(v, "adminEmail", "admin@example.com");
        set(v, "adminPassword", "test-only-strong-bootstrap");
        assertDoesNotThrow(v::validate);
    }

    @ParameterizedTest
    @CsvSource({
        "jwtSecret,short",
        "cookieSecure,false",
        "ddlAuto,update",
        "ddlAuto,create",
        "ddlAuto,create-drop",
        "rateLimitBackend,inmemory",
        "resumeS3Enabled,false",
        "resumeS3Bucket,' '",
        "adminEmail,unpaired@example.com"
    })
    void rejectsConfigurationThatWouldMakeAReleaseUnsafe(String field, String value) {
        var v = valid();
        set(
                v,
                field,
                field.equals("cookieSecure") || field.equals("resumeS3Enabled")
                        ? Boolean.parseBoolean(value)
                        : value);
        assertThrows(IllegalStateException.class, v::validate);
    }

    @Test
    void rootDatabasePairAndMissingJwtAreRejected() {
        var v = valid();
        set(v, "datasourceUsername", "root");
        set(v, "datasourcePassword", "root");
        assertThrows(IllegalStateException.class, v::validate);
        v = valid();
        set(v, "jwtSecret", null);
        assertThrows(IllegalStateException.class, v::validate);
    }

    @Test
    void startupSafetyRunsOnlyInProductionAndRejectsDefaultAccounts() {
        var env = new MockEnvironment();
        var v = new ProductionSafetyValidator(env);
        assertDoesNotThrow(v::validate);
        env.setActiveProfiles("prod");
        set(v, "jwtSecret", "x".repeat(64));
        set(v, "dbUser", "app");
        set(v, "dbPass", "test-only");
        assertDoesNotThrow(v::validate);
        set(v, "dbUser", "root");
        assertThrows(IllegalStateException.class, v::validate);
        set(v, "dbUser", "app");
        set(v, "dbPass", "root");
        assertThrows(IllegalStateException.class, v::validate);
        set(v, "dbPass", "test-only");
        set(v, "adminPassword", "Admin@123");
        assertThrows(IllegalStateException.class, v::validate);
    }
}
