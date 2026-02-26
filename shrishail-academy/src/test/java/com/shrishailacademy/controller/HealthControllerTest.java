package com.shrishailacademy.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealthControllerTest {

    private final HealthController healthController = new HealthController();

    @Test
    void homeShouldRedirectToIndexHtml() {
        RedirectView redirectView = healthController.home();
        assertEquals("/index.html", redirectView.getUrl());
    }

    @Test
    void healthCheckShouldReturnUpStatusPayload() {
        ResponseEntity<Map<String, Object>> response = healthController.healthCheck();

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("BrightNest Academy API is running", response.getBody().get("message"));
        assertEquals("1.0.0", response.getBody().get("version"));
        assertTrue((Long) response.getBody().get("timestamp") > 0L);
    }
}
