package com.shrishailacademy.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    /**
     * Redirect root to index.html (homepage)
     */
    @GetMapping("/")
    public ResponseEntity<Void> home() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/index.html"))
                .build();
    }

    /**
     * API health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "BrightNest Academy API is running");
        response.put("timestamp", System.currentTimeMillis());
        response.put("version", "1.0.0");
        return ResponseEntity.ok(response);
    }
}


