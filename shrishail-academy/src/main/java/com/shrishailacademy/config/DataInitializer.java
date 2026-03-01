package com.shrishailacademy.config;

import com.shrishailacademy.model.Course;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.CourseRepository;
import com.shrishailacademy.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * DataInitializer - Runs at startup to ensure default admin and courses exist.
 * Admin credentials loaded from environment variables for security.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email:}")
    private String adminEmail;

    @Value("${admin.password:}")
    private String adminPassword;

    public DataInitializer(UserRepository userRepository,
            CourseRepository courseRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Create or update admin — credentials from env vars / application.properties
        if (hasAdminBootstrapCredentials()) {
            if (!userRepository.existsByEmail(adminEmail)) {
                User admin = new User();
                admin.setName("Admin");
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setPhone("9999999999");
                admin.setRole(User.Role.ADMIN);
                userRepository.save(admin);
                log.info("Default admin created: {}", adminEmail);
            } else {
                // Always sync the admin password with configured value
                User admin = userRepository.findByEmail(adminEmail).orElse(null);
                if (admin != null) {
                    admin.setPassword(passwordEncoder.encode(adminPassword));
                    userRepository.save(admin);
                    log.info("Admin password synced to configured value: {}", adminEmail);
                }
            }
        } else {
            log.warn("Admin bootstrap skipped: set ADMIN_EMAIL and ADMIN_PASSWORD to create/sync admin user.");
        }

        // Seed default courses if none exist
        if (courseRepository.count() == 0) {
            seedCourse("Mathematics",
                    "Master mathematical concepts from basics to advanced levels. Our structured curriculum covers algebra, geometry, calculus, and more with practical problem-solving techniques.",
                    "12 months", "📐", "#3B82F6", new BigDecimal("3000.00"));
            seedCourse("Science",
                    "Explore the wonders of Physics, Chemistry, and Biology through interactive learning. We make science fun with experiments and real-world applications.",
                    "12 months", "🔬", "#10B981", new BigDecimal("3500.00"));
            seedCourse("English",
                    "Develop strong communication skills with our comprehensive English program covering grammar, literature, writing, and spoken English.",
                    "10 months", "📚", "#8B5CF6", new BigDecimal("2500.00"));
            seedCourse("Kannada",
                    "Learn Karnataka's beautiful language with focus on reading, writing, and literature. Perfect for students preparing for board exams.",
                    "8 months", "ಕ", "#F59E0B", new BigDecimal("2000.00"));
            seedCourse("Hindi",
                    "Master Hindi language skills through comprehensive lessons in grammar, literature, and composition. Ideal for all proficiency levels.",
                    "10 months", "ह", "#EF4444", new BigDecimal("2500.00"));
            seedCourse("Sanskrit",
                    "Discover the ancient language of Sanskrit with expert guidance in grammar, slokas, and classical texts.",
                    "12 months", "संस्", "#EC4899", new BigDecimal("2000.00"));
            seedCourse("French",
                    "Learn French language with interactive sessions covering conversation, grammar, and French culture. DELF exam preparation available.",
                    "14 months", "🇫🇷", "#06B6D4", new BigDecimal("4000.00"));
            log.info("Default courses seeded: 7 courses with fees");
        } else {
            // Update existing courses that don't have fees set
            updateFeeIfMissing("Mathematics", new BigDecimal("3000.00"));
            updateFeeIfMissing("Science", new BigDecimal("3500.00"));
            updateFeeIfMissing("English", new BigDecimal("2500.00"));
            updateFeeIfMissing("Kannada", new BigDecimal("2000.00"));
            updateFeeIfMissing("Hindi", new BigDecimal("2500.00"));
            updateFeeIfMissing("Sanskrit", new BigDecimal("2000.00"));
            updateFeeIfMissing("French", new BigDecimal("4000.00"));
            log.info("Courses already exist: {} courses found (fees updated if missing)", courseRepository.count());
        }

        log.info("=".repeat(50));
        log.info("BrightNest Academy started successfully!");
        log.info("=".repeat(50));
    }

    private void updateFeeIfMissing(String title, BigDecimal fee) {
        courseRepository.findByTitle(title).ifPresent(course -> {
            if (course.getFee() == null) {
                course.setFee(fee);
                courseRepository.save(course);
            }
        });
    }

    private void seedCourse(String title, String description, String duration, String icon, String color,
            BigDecimal fee) {
        Course course = new Course();
        course.setTitle(title);
        course.setDescription(description);
        course.setDuration(duration);
        course.setIcon(icon);
        course.setColor(color);
        course.setFee(fee);
        courseRepository.save(course);
    }

    private boolean hasAdminBootstrapCredentials() {
        return adminEmail != null && !adminEmail.isBlank()
                && adminPassword != null && !adminPassword.isBlank();
    }
}
