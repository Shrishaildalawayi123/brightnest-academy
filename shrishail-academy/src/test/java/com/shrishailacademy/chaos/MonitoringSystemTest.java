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
 * CHAOS TEST 9 — Monitoring System Validation
 *
 * Verifies that Actuator health/info endpoints are accessible for monitoring,
 * sensitive actuator endpoints are admin-protected, and the custom /health
 * endpoint provides the data monitoring tools need (Prometheus, Grafana,
 * UptimeRobot).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MonitoringSystemTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointShouldHaveAllMonitoringFields() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void actuatorHealthShouldBePubliclyAccessible() throws Exception {
        // Returns 200 (UP) or 503 (DOWN) if Redis unavailable — both are valid
        // The key point: no 401/403 (no auth required)
        int status = mockMvc.perform(get("/actuator/health"))
                .andReturn().getResponse().getStatus();
        assertThat(status).isIn(200, 503);
    }

    @Test
    void actuatorInfoShouldBePubliclyAccessible() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    @Test
    void sensitiveActuatorEndpointsShouldRequireAuth() throws Exception {
        // /actuator/env, /actuator/beans, /actuator/configprops should be admin-only
        int envStatus = mockMvc.perform(get("/actuator/env"))
                .andReturn().getResponse().getStatus();
        assertThat(envStatus).isIn(401, 403, 404);

        int beansStatus = mockMvc.perform(get("/actuator/beans"))
                .andReturn().getResponse().getStatus();
        assertThat(beansStatus).isIn(401, 403, 404);

        int configStatus = mockMvc.perform(get("/actuator/configprops"))
                .andReturn().getResponse().getStatus();
        assertThat(configStatus).isIn(401, 403, 404);
    }

    @Test
    void healthEndpointResponseTimeShouldBeFast() throws Exception {
        long start = System.currentTimeMillis();
        mockMvc.perform(get("/health")).andExpect(status().isOk());
        long duration = System.currentTimeMillis() - start;

        // Health check must respond within 2 seconds
        assertThat(duration).isLessThan(2000);
    }

    @Test
    void actuatorHealthShouldReturnValidJson() throws Exception {
        // Actuator uses application/vnd.spring-boot.actuator.v3+json content type
        String body = mockMvc.perform(get("/actuator/health"))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).contains("status");
    }

    @Test
    void multipleMonitoringPollsShouldSucceed() throws Exception {
        // Simulate monitoring tool polling every second
        for (int i = 0; i < 30; i++) {
            mockMvc.perform(get("/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"));
        }
    }

    @Test
    void healthEndpointShouldNotRequireAuthentication() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk());
    }

    @Test
    void actuatorHealthShouldNotLeakSensitiveDetails() throws Exception {
        String response = mockMvc.perform(get("/actuator/health"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Should not expose DB connection strings, passwords, or internal paths
        assertThat(response)
                .doesNotContain("password")
                .doesNotContain("jdbc:")
                .doesNotContain("secret");
    }
}
