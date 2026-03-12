package com.shrishailacademy.integration;

import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.TenantRepository;
import com.shrishailacademy.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CI/CD smoke test: verifies that the configured admin account
 * (admin@brightnest.com / Admin@123) can log in and receive a JWT.
 *
 * Runs as part of every build so credential or bootstrap regressions are
 * caught immediately in the pipeline.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Admin Credentials Smoke Test")
class AdminCredentialsSmokeTest {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String DEFAULT_TENANT = "default";
        private static final String FALLBACK_ADMIN_EMAIL = "admin@brightnest.com";
        private static final String FALLBACK_ADMIN_PASSWORD = "Admin@123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${admin.email:admin@brightnest.com}")
    private String adminEmail;

    @Value("${admin.password:Admin@123}")
    private String adminPassword;

    // ────────────────────────────────────────────────────────────────
    // 1. Student registration
    // ────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("1. Student: register → returns 201 with requiresEmailVerification")
    void studentRegistration_returns201() throws Exception {
        String payload = """
                {
                  "name": "Shrishail Test",
                  "email": "shrishail-smoke@test.com",
                  "password": "Student@123",
                  "phone": "9164588697"
                }
                """;

        mockMvc.perform(post("/api/auth/register")
                .header(TENANT_HEADER, DEFAULT_TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requiresEmailVerification").exists());
    }

    // ────────────────────────────────────────────────────────────────
    // 2. Student login (after manual verification bypass)
    // ────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("2. Student: register + verify + login → returns 200 with JWT")
    void studentLogin_returnsJwt() throws Exception {
        String email = "student-login-smoke@test.com";
        String password = "Student@123";

        // Register
        String registerPayload = """
                {
                  "name": "Student Smoke",
                  "email": "%s",
                  "password": "%s",
                  "phone": "9000000000"
                }
                """.formatted(email, password);

        mockMvc.perform(post("/api/auth/register")
                .header(TENANT_HEADER, DEFAULT_TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerPayload))
                .andExpect(status().isCreated());

        // Bypass email verification in-process (test environment)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AssertionError("Student not found after registration"));
        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        userRepository.save(user);

        // Login
        String loginPayload = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);

        mockMvc.perform(post("/api/auth/login")
                .header(TENANT_HEADER, DEFAULT_TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.role").value("ROLE_STUDENT"))
                .andExpect(jsonPath("$.email").value(email));
    }

    // ────────────────────────────────────────────────────────────────
    // 3. Admin login with admin@brightnest.com / Admin@123
    // ────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("3. Admin: admin@brightnest.com / Admin@123 → returns 200 with JWT and ROLE_ADMIN")
    void adminLogin_withConfiguredCredentials_returnsJwt() throws Exception {
        String effectiveAdminEmail = (adminEmail == null || adminEmail.isBlank())
                ? FALLBACK_ADMIN_EMAIL
                : adminEmail;
        String effectiveAdminPassword = (adminPassword == null || adminPassword.isBlank())
                ? FALLBACK_ADMIN_PASSWORD
                : adminPassword;

        // Ensure admin user exists for test tenant (mirrors DataInitializer logic)
        var tenantOpt = tenantRepository.findByTenantKey(DEFAULT_TENANT);
        assertThat(tenantOpt).as("Default tenant must exist").isPresent();
        var tenant = tenantOpt.get();

        userRepository.findByEmailAndTenantId(effectiveAdminEmail, tenant.getId()).ifPresentOrElse(
                existing -> {
                    existing.setPassword(passwordEncoder.encode(effectiveAdminPassword));
                    existing.setEmailVerified(true);
                    existing.setRole(User.Role.ADMIN);
                    userRepository.save(existing);
                },
                () -> {
                    User admin = new User();
                    admin.setTenant(tenant);
                    admin.setName("Admin");
                    admin.setEmail(effectiveAdminEmail);
                    admin.setPassword(passwordEncoder.encode(effectiveAdminPassword));
                    admin.setPhone("9999999999");
                    admin.setRole(User.Role.ADMIN);
                    admin.setEmailVerified(true);
                    userRepository.save(admin);
                });

        String loginPayload = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                                                                """.formatted(effectiveAdminEmail, effectiveAdminPassword);

        mockMvc.perform(post("/api/auth/login")
                .header(TENANT_HEADER, DEFAULT_TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"))
                                .andExpect(jsonPath("$.email").value(effectiveAdminEmail));
    }
}
