package com.shrishailacademy.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HealthControllerTest {
    private final HealthEndpoint endpoint = mock(HealthEndpoint.class);
    private final HealthController controller = new HealthController(endpoint, "tested-revision");

    @Test
    void returnsReadyAndReleaseIdentityWithoutDependencyDetails() {
        when(endpoint.healthForPath("readiness")).thenReturn(Health.up().withDetail("dbPassword", "hidden").build());
        var response = controller.healthCheck();
        assertEquals(200, response.getStatusCode().value());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("tested-revision", response.getBody().get("revision"));
        assertEquals("1.0.0", response.getBody().get("version"));
        assertFalse(response.getBody().containsKey("dbPassword"));
    }

    @Test
    void dependencyFailureIsNotReportedAsReady() {
        when(endpoint.healthForPath("readiness")).thenReturn(Health.down().build());
        assertEquals(503, controller.healthCheck().getStatusCode().value());
    }

    @Test
    void missingReadinessGroupFailsClosed() {
        assertEquals(503, controller.healthCheck().getStatusCode().value());
    }
}
