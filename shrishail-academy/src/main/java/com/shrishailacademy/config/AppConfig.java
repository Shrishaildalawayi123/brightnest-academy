package com.shrishailacademy.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Application-wide feature configuration.
 * Enables caching and async processing for audit logs.
 */
@Configuration
@EnableAsync
@EnableCaching
public class AppConfig {
}
