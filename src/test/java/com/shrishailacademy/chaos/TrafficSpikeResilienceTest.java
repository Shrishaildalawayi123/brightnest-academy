package com.shrishailacademy.chaos;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CHAOS TEST 4 — Traffic Spike Resilience
 *
 * Simulates a traffic spike: many login attempts and API calls in rapid
 * succession.
 * Verifies rate limiter activates (HTTP 429), Retry-After header is present,
 * and the server remains responsive throughout.
 *
 * Tests are ordered to control rate-limit bucket consumption (all tests share
 * one IP).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestPropertySource(properties = {
        "rate.limit.login.max=5",
        "rate.limit.login.window-seconds=60",
        "rate.limit.api.max=50",
        "rate.limit.api.window-seconds=60"
})
class TrafficSpikeResilienceTest {

    private static final String TENANT = "default";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Order(1)
    void loginSpikeShouldTriggerRateLimitAndReturnWellFormedResponse() throws Exception {
        String loginPayload = """
                {"email":"spike-user@example.com","password":"Wrong@123"}
                """;

        // Fire login requests: first 5 should get 401 (bad credentials), then 429
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .header("X-Tenant-ID", TENANT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginPayload))
                    .andExpect(status().isUnauthorized());
        }

        // 6th request should be rate limited with well-formed response
        mockMvc.perform(post("/api/auth/login")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @Order(2)
    void serverShouldRemainResponsiveAfterRateLimitActivation() throws Exception {
        // Health check should still be fast and responsive even after rate limiting
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @Order(3)
    void massRegistrationSpikeShouldNotCrashServer() throws Exception {
        // Rapid registrations — server must remain stable
        // Registration is NOT login-rate-limited, so these should all succeed
        for (int i = 0; i < 15; i++) {
            String email = "spike-reg-" + UUID.randomUUID() + "@example.com";
            mockMvc.perform(post("/api/auth/register")
                    .header("X-Tenant-ID", TENANT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            {"name":"Spike User %d","email":"%s","password":"Secure@123!","phone":"77777%05d"}
                            """.formatted(i, email, i)));
        }

        // Server should still be healthy
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
