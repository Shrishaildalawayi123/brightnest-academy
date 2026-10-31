package com.shrishailacademy.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Public readiness summary: dependency status and release identity, no component details. */
@RestController
public class HealthController {
    private final HealthEndpoint healthEndpoint;
    private final String revision;

    public HealthController(HealthEndpoint healthEndpoint, @Value("${RELEASE_REVISION:unknown}") String revision) {
        this.healthEndpoint = healthEndpoint;
        this.revision = revision;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        HealthComponent readiness = healthEndpoint.healthForPath("readiness");
        Status status = readiness == null ? Status.OUT_OF_SERVICE : readiness.getStatus();
        return ResponseEntity.status(Status.UP.equals(status) ? 200 : 503).body(Map.of(
                "status", status.getCode(),
                "message", "BrightNest Academy API is running",
                "timestamp", System.currentTimeMillis(),
                "version", "1.0.0",
                "revision", revision));
    }
}
