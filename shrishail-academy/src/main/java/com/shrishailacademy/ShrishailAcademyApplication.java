package com.shrishailacademy;

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

    public static void main(String[] args) {
        SpringApplication.run(ShrishailAcademyApplication.class, args);
        System.out.println("\n========================================");
        System.out.println("BrightNest Academy API Started!");
        System.out.println("========================================");
        System.out.println("Health: /health");
        System.out.println("========================================\n");
    }
}
