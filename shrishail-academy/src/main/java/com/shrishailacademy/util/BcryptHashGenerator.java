package com.shrishailacademy.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BcryptHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        String password = "admin123";
        String hash = encoder.encode(password);
        
        System.out.println("Password: " + password);
        System.out.println("BCrypt Hash: " + hash);
        System.out.println();
        
        // Verify the hash works
        boolean matches = encoder.matches(password, hash);
        System.out.println("Hash verification: " + (matches ? "SUCCESS" : "FAIL"));
        
        // Check against existing hashes from database
        String[] existingHashes = {
            "$2a$10$xQGE5BhYV5YOB.YLhXGze.8FqLHKCKLLPWYVLLqhL5GQpLUzKqz4G",  // testadmin@test.com
            "$2a$10$99wDgO94VoU3lqAmYAv.Lulrclpq7wCdQe1cLXV6dIuRnafyygTVO",  // admin@brightnest.com
            "$2a$10$hARXG0VVg3r1ci3uNU44Ue3xDjOxKrWLzRh2wfd9Hjis4XALWwEp."   // admin@example.com
        };
        
        String[] emails = {
            "testadmin@test.com",
            "admin@brightnest.com",
            "admin@example.com"
        };
        
        System.out.println("\nVerifying existing database hashes:");
        for (int i = 0; i < existingHashes.length; i++) {
            boolean match = encoder.matches(password, existingHashes[i]);
            System.out.println(emails[i] + ": " + (match ? "MATCHES" : "NO MATCH"));
        }
    }
}
