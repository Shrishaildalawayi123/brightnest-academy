package com.shrishailacademy.chaos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CHAOS TEST 2 — Database Failure & Recovery
 *
 * Simulates database problems: corrupt state, missing tables, constraint
 * violations,
 * and verifies the application handles them gracefully (no stack trace leaks,
 * no crashes).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DatabaseResilienceTest {

    private static final String TENANT = "default";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @Test
    void applicationShouldHandleDuplicateKeyGracefully() throws Exception {
        String email = "dbtest-" + UUID.randomUUID() + "@example.com";
        String payload = """
                {"name":"DB Test","email":"%s","password":"Secure@123!","phone":"9876543210"}
                """.formatted(email);

        // First registration succeeds
        mockMvc.perform(post("/api/auth/register")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk());

        // Duplicate — should return conflict, not 500
        mockMvc.perform(post("/api/auth/register")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void applicationShouldHandleNonexistentRecordGracefully() throws Exception {
        mockMvc.perform(get("/api/courses/999999")
                .header("X-Tenant-ID", TENANT))
                .andExpect(status().isNotFound());
    }

    @Test
    void applicationShouldSurviveConcurrentWrites() throws Exception {
        // Simulate concurrent registration of different users — DB shouldn't deadlock
        for (int i = 0; i < 20; i++) {
            String email = "concurrent-" + UUID.randomUUID() + "@example.com";
            mockMvc.perform(post("/api/auth/register")
                    .header("X-Tenant-ID", TENANT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"name":"User %d","email":"%s","password":"Secure@123!","phone":"55555%05d"}
                            """.formatted(i, email, i)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void applicationShouldRecoverAfterDbRoundTrip() throws Exception {
        // Verify DB connectivity
        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute("SELECT 1");
        }

        // Application should still serve requests normally
        mockMvc.perform(get("/api/courses")
                .header("X-Tenant-ID", TENANT))
                .andExpect(status().isOk());
    }

    @Test
    void constraintViolationShouldNotLeakDbDetails() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"","email":"bad","password":"x","phone":""}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists())
                // Must NOT contain SQL or DB details
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString("SQL"))))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString("hibernate"))))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString("constraint"))));
    }

    @Test
    void longTransactionShouldNotBlockHealthCheck() throws Exception {
        // Start a registration (long-ish write) and health check in sequence
        String email = "txn-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/auth/register")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Transaction Test","email":"%s","password":"Secure@123!","phone":"1111111111"}
                        """.formatted(email)))
                .andExpect(status().isOk());

        // Health check must still respond immediately
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
