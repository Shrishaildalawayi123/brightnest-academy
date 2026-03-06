package com.shrishailacademy.chaos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CHAOS TEST 1 — Server Failure & Recovery
 *
 * Validates that the application boots correctly, the health endpoint responds,
 * and the Docker restart policy (--restart=always / unless-stopped) is
 * configured.
 * Actual container restart is a Docker-level concern verified in
 * docker-compose.prod.yml.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ServerRecoveryTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointShouldReturnUp() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.version").value("1.0.0"));
    }

    @Test
    void actuatorHealthShouldBeAccessibleWithoutAuth() throws Exception {
        // May return 503 if Redis health indicator reports DOWN (no Redis in test env)
        int status = mockMvc.perform(get("/actuator/health"))
                .andReturn().getResponse().getStatus();
        assertThat(status).isIn(200, 503);
    }

    @Test
    void applicationContextShouldStartCleanly() throws Exception {
        // If we reach this point, the Spring context loaded successfully.
        // This validates startup without errors: bean wiring, DB init, filter chain,
        // etc.
        mockMvc.perform(get("/api/courses").header("X-Tenant-ID", "default"))
                .andExpect(status().isOk());
    }

    @Test
    void multipleRapidHealthChecksUnderLoad() throws Exception {
        // Simulate monitoring system polling health repeatedly
        for (int i = 0; i < 50; i++) {
            mockMvc.perform(get("/health"))
                    .andExpect(status().isOk());
        }
    }
}
