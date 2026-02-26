package com.shrishailacademy.config;

import org.springframework.context.annotation.Configuration;

// CORS is fully handled by SecurityConfig.corsConfigurationSource()
// This file is intentionally left minimal to avoid double CORS configuration conflict.
@Configuration
public class WebConfig {
    // No additional CORS config needed here.
}
