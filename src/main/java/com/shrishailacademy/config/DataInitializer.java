package com.shrishailacademy.config;

import com.shrishailacademy.model.Course;
import com.shrishailacademy.model.Tenant;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.CourseRepository;
import com.shrishailacademy.repository.UserRepository;
import com.shrishailacademy.service.TenantService;
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
    private static final String DEFAULT_TEACHER_PASSWORD = "Teacher@123!";

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantService tenantService;

    @Value("${admin.email:}")
    private String adminEmail;

    @Value("${admin.password:}")
    private String adminPassword;

    public DataInitializer(UserRepository userRepository,
            CourseRepository courseRepository,
            PasswordEncoder passwordEncoder,
            TenantService tenantService) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantService = tenantService;
    }

    @Override
    public void run(String... args) {
        Tenant defaultTenant = tenantService.ensureDefaultTenantExists();
        Long tenantId = defaultTenant.getId();

        // Create or update admin — credentials from env vars / application.properties
        if (hasAdminBootstrapCredentials()) {
            if (!userRepository.existsByEmailAndTenantId(adminEmail, tenantId)) {
                User admin = new User();
                admin.setTenant(defaultTenant);
                admin.setName("Admin");
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setPhone("9999999999");
                admin.setRole(User.Role.ADMIN);
                userRepository.save(admin);
                log.info("Default admin created: {}", adminEmail);
            } else {
                // Always sync the admin password with configured value
                User admin = userRepository.findByEmailAndTenantId(adminEmail, tenantId).orElse(null);
                if (admin != null) {
                    admin.setPassword(passwordEncoder.encode(adminPassword));
                    userRepository.save(admin);
                    log.info("Admin password synced to configured value: {}", adminEmail);
                }
            }
        } else {
            log.warn("Admin bootstrap skipped: set ADMIN_EMAIL and ADMIN_PASSWORD to create/sync admin user.");
        }

        User bharati = ensureTeacher(defaultTenant, "Bharati R Satappagol", "bharati@brightnest-academy.com",
                "6363464005");
        User chetana = ensureTeacher(defaultTenant, "Chetana", "chetana@brightnest-academy.com", "9000000001");
        User mahadev = ensureTeacher(defaultTenant, "Mahadev S", "mahadev@brightnest-academy.com", "9000000002");
        User pooja = ensureTeacher(defaultTenant, "Pooja", "pooja@brightnest-academy.com", "9000000003");
        User nagesh = ensureTeacher(defaultTenant, "Nagesh Kumar M U", "nagesh@brightnest-academy.com",
                "9000000004");
        User preeti = ensureTeacher(defaultTenant, "Preeti R S", "preeti@brightnest-academy.com", "9000000005");
        User prema = ensureTeacher(defaultTenant, "Prema G", "prema@brightnest-academy.com", "9000000006");
        User shrishail = ensureTeacher(defaultTenant, "Mr. Shrishail Dalawayi",
                "shrishail@brightnest-academy.com", "9000000007");

        ensureCourse(defaultTenant, "Mathematics", "maths",
                "Master mathematical concepts from basics to advanced levels. Our structured curriculum covers algebra, geometry, calculus, and more with practical problem-solving techniques.",
                "12 months", "📐", "#3B82F6", new BigDecimal("3000.00"), nagesh);
        ensureCourse(defaultTenant, "Science", "science",
                "Explore the wonders of Physics, Chemistry, and Biology through interactive learning. We make science fun with experiments and real-world applications.",
                "12 months", "🔬", "#10B981", new BigDecimal("3500.00"), pooja);
        ensureCourse(defaultTenant, "English", "english",
                "Develop strong communication skills with our comprehensive English program covering grammar, literature, writing, and spoken English.",
                "10 months", "📚", "#8B5CF6", new BigDecimal("2500.00"), chetana);
        ensureCourse(defaultTenant, "Kannada", "kannada",
                "Learn Karnataka's beautiful language with focus on reading, writing, and literature. Perfect for students preparing for board exams.",
                "8 months", "ಕ", "#F59E0B", new BigDecimal("2000.00"), bharati);
        ensureCourse(defaultTenant, "Hindi", "hindi",
                "Master Hindi language skills through comprehensive lessons in grammar, literature, and composition. Ideal for all proficiency levels.",
                "10 months", "ह", "#EF4444", new BigDecimal("2500.00"), mahadev);
        ensureCourse(defaultTenant, "Sanskrit", "sanskrit",
                "Discover the ancient language of Sanskrit with expert guidance in grammar, slokas, and classical texts.",
                "12 months", "संस्", "#EC4899", new BigDecimal("2000.00"), bharati);
        ensureCourse(defaultTenant, "German", "german",
                "Learn German language with interactive sessions covering conversation, grammar, and cultural fluency for school learners.",
                "14 months", "🇩🇪", "#06B6D4", new BigDecimal("4000.00"), shrishail);

        log.info("Course and faculty catalog ensured for tenant {} with {} courses", tenantId,
                courseRepository.findAllByTenantId(tenantId).size());

        log.info("=".repeat(50));
        log.info("BrightNest Academy started successfully!");
        log.info("=".repeat(50));
    }

    private User ensureTeacher(Tenant tenant, String name, String email, String phone) {
        return userRepository.findByEmailAndTenantId(email, tenant.getId())
                .map(existing -> {
                    boolean changed = false;
                    if (!name.equals(existing.getName())) {
                        existing.setName(name);
                        changed = true;
                    }
                    if (existing.getRole() != User.Role.TEACHER) {
                        existing.setRole(User.Role.TEACHER);
                        changed = true;
                    }
                    if (phone != null && !phone.equals(existing.getPhone())) {
                        existing.setPhone(phone);
                        changed = true;
                    }
                    return changed ? userRepository.save(existing) : existing;
                })
                .orElseGet(() -> {
                    User teacher = new User();
                    teacher.setTenant(tenant);
                    teacher.setName(name);
                    teacher.setEmail(email);
                    teacher.setPassword(passwordEncoder.encode(DEFAULT_TEACHER_PASSWORD));
                    teacher.setPhone(phone);
                    teacher.setRole(User.Role.TEACHER);
                    return userRepository.save(teacher);
                });
    }

    private void ensureCourse(Tenant tenant, String title, String subjectKey, String description, String duration,
            String icon, String color, BigDecimal fee, User teacher) {
        Course course = courseRepository.findBySubjectKeyAndTenantId(subjectKey, tenant.getId())
                .or(() -> courseRepository.findByTitleAndTenantId(title, tenant.getId()))
                .orElseGet(Course::new);

        course.setTenant(tenant);
        course.setTitle(title);
        course.setSubjectKey(subjectKey);
        course.setDescription(description);
        course.setDuration(duration);
        course.setIcon(icon);
        course.setColor(color);
        course.setFee(fee);
        course.setTeacher(teacher);
        courseRepository.save(course);
    }

    private boolean hasAdminBootstrapCredentials() {
        return adminEmail != null && !adminEmail.isBlank()
                && adminPassword != null && !adminPassword.isBlank();
    }
}
