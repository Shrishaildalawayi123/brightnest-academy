package com.shrishailacademy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * BrightNest Academy - Main Spring Boot Application
 * 
 * Education Institute Management System
 * 
 * @author BrightNest Academy Team
 * @version 1.0.0
 */
@SpringBootApplication
public class ShrishailAcademyApplication {

    private static final Logger log = LoggerFactory.getLogger(ShrishailAcademyApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(ShrishailAcademyApplication.class, args);

        log.info("========================================");
        log.info("BrightNest Academy API Started!");
        log.info("========================================");
        log.info("Health Check: /health");
        log.info("API Documentation: /swagger-ui.html");
        log.info("Actuator: /actuator (ADMIN only)");
        log.info("========================================");
    }
}
