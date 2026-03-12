package com.shrishailacademy.test;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility to generate BCrypt password hashes for testing
 */
public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        String password = "admin123";
        String hash = encoder.encode(password);
        
        System.out.println("========================================");
        System.out.println("Password: " + password);
        System.out.println("BCrypt Hash: " + hash);
        System.out.println("========================================");
        
        // Test the existing hash
        String existingHash = "$2a$10$xQGE5BhYV5YOB.YLhXGze.8FqLHKCKLLPWYVLLqhL5GQpLUzKqz4G";
        boolean matches = encoder.matches(password, existingHash);
        
        System.out.println("\nTesting existing hash:");
        System.out.println("Existing Hash: " + existingHash);
        System.out.println("Password 'admin123' matches: " + matches);
        System.out.println("========================================");
    }
}
