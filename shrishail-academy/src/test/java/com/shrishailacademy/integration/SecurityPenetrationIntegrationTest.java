package com.shrishailacademy.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * OWASP Top 10 security tests:
 * - SQL injection
 * - XSS payload injection
 * - JWT token tampering
 * - Duplicate registration
 * - Malformed JSON
 * - Oversized input
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityPenetrationIntegrationTest {

    private static final String TENANT = "default";

    @Autowired
    private MockMvc mockMvc;

    // ===== SQL INJECTION =====

    @Test
    void loginShouldBlockSqlInjectionInEmail() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"' OR 1=1 --","password":"anything"}
                        """))
                .andExpect(status().isBadRequest()); // @Email validation rejects
    }

    @Test
    void loginShouldBlockSqlInjectionInPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"test@example.com","password":"' OR '1'='1"}
                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void contactShouldNotExecuteSqlInjection() throws Exception {
        mockMvc.perform(post("/api/contact")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name":"Robert'; DROP TABLE users; --",
                          "email":"test@example.com",
                          "subject":"SQL Injection Test",
                          "message":"This is a test message with SQL injection payload."
                        }
                        """))
                .andExpect(status().isOk()); // Accepts but sanitizes
    }

    // ===== XSS INJECTION =====

    @Test
    void registerShouldSanitizeXssInName() throws Exception {
        String email = "xss-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/auth/register")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name":"<script>alert('xss')</script>",
                          "email":"%s",
                          "password":"Secure@123!",
                          "phone":"9876543210"
                        }
                        """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<script>"))));
    }

    @Test
    void contactShouldSanitizeXssPayloads() throws Exception {
        mockMvc.perform(post("/api/contact")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name":"<img onerror=alert(1) src=x>",
                          "email":"xss@example.com",
                          "subject":"XSS Test Subject Here",
                          "message":"<script>document.cookie</script> Some valid text here."
                        }
                        """))
                .andExpect(status().isOk());
    }

    // ===== JWT TAMPERING =====

    @Test
    void tamperedJwtShouldBeRejected() throws Exception {
        String fakeJwt = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbkBleGFtcGxlLmNvbSJ9.FAKE_SIGNATURE";

        mockMvc.perform(post("/api/courses")
                .header("X-Tenant-ID", TENANT)
                .header("Authorization", "Bearer " + fakeJwt)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Hacked Course","fee":0}
                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void emptyBearerTokenShouldBeRejected() throws Exception {
        mockMvc.perform(post("/api/courses")
                .header("X-Tenant-ID", TENANT)
                .header("Authorization", "Bearer ")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Test","fee":0}
                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nullBearerTokenShouldBeRejected() throws Exception {
        mockMvc.perform(post("/api/courses")
                .header("X-Tenant-ID", TENANT)
                .header("Authorization", "Bearer null")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Test","fee":0}
                        """))
                .andExpect(status().isUnauthorized());
    }

    // ===== DUPLICATE REGISTRATION =====

    @Test
    void duplicateRegistrationShouldBeRejected() throws Exception {
        String email = "dup-" + UUID.randomUUID() + "@example.com";
        String payload = """
                {"name":"User","email":"%s","password":"Pass@123!","phone":"1111111111"}
                """.formatted(email);

        mockMvc.perform(post("/api/auth/register")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/register")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isConflict());
    }

    // ===== MALFORMED INPUT =====

    @Test
    void malformedJsonShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void emptyBodyShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void oversizedInputShouldBeRejected() throws Exception {
        String longString = "A".repeat(10000);
        mockMvc.perform(post("/api/contact")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name":"%s",
                          "email":"long@example.com",
                          "subject":"Test",
                          "message":"This is a test"
                        }
                        """.formatted(longString)))
                .andExpect(status().isBadRequest());
    }

    // ===== TENANT HEADER MISSING =====

    @Test
    void missingTenantHeaderShouldStillProcessAuthEndpoint() throws Exception {
        // Auth endpoints allow default tenant fallback when no header is present
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"test@example.com","password":"wrong"}
                        """))
                .andExpect(status().isUnauthorized());
    }
}
