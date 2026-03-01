package com.shrishailacademy.controller;

import com.shrishailacademy.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Admin Dashboard Analytics API
 * Provides real-time stats for the admin dashboard
 */
@RestController
@RequestMapping("/api/admin/analytics")
@PreAuthorize("hasRole('ADMIN')")
public class AnalyticsController {

        private final AnalyticsService analyticsService;

        public AnalyticsController(AnalyticsService analyticsService) {
                this.analyticsService = analyticsService;
        }

        @GetMapping("/dashboard")
        public ResponseEntity<Map<String, Object>> getDashboardStats() {
                return ResponseEntity.ok(analyticsService.getDashboardStats());
        }
}
