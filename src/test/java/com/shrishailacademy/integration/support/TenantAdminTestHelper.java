package com.shrishailacademy.integration.support;

import com.shrishailacademy.model.Tenant;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.TenantRepository;
import com.shrishailacademy.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Component
public class TenantAdminTestHelper {

    public static final String TENANT_HEADER = "X-Tenant-ID";
    public static final String AUTH_COOKIE = "AUTH_TOKEN";
    public static final String CSRF_COOKIE = "XSRF-TOKEN";

    private final MockMvc mockMvc;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public TenantAdminTestHelper(
            MockMvc mockMvc,
            TenantRepository tenantRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.mockMvc = mockMvc;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Tenant createTenant(String tenantKeyPrefix) {
        Tenant tenant = new Tenant();
        tenant.setTenantKey(tenantKeyPrefix + UUID.randomUUID().toString().substring(0, 8));
        tenant.setName("Tenant " + UUID.randomUUID().toString().substring(0, 8));
        return tenantRepository.save(tenant);
    }

    public User createAdminUser(Tenant tenant, String email, String rawPassword) {
        return createUser(tenant, "Tenant Admin", email, rawPassword, User.Role.ADMIN);
    }

    public User createUser(Tenant tenant, String name, String email, String rawPassword, User.Role role) {
        User user = new User();
        user.setTenant(tenant);
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setPhone("9999999999");
        user.setRole(role);
        user.setEmailVerified(true);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        return userRepository.save(user);
    }

    public Cookie loginAndGetAuthCookie(String tenantKey, String email, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .header(TENANT_HEADER, tenantKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email":"%s",
                          "password":"%s"
                        }
                        """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie cookie = loginResult.getResponse().getCookie(AUTH_COOKIE);
        assertNotNull(cookie, "Expected AUTH_TOKEN cookie after successful login");
        return cookie;
    }

        public Cookie loginAndGetCsrfCookie(String tenantKey, String email, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
            .header(TENANT_HEADER, tenantKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email":"%s",
                  "password":"%s"
                }
                """.formatted(email, password)))
            .andExpect(status().isOk())
            .andReturn();

        Cookie cookie = loginResult.getResponse().getCookie(CSRF_COOKIE);
        assertNotNull(cookie, "Expected XSRF-TOKEN cookie after successful login");
        return cookie;
        }

        public Cookie[] loginAndGetAuthAndCsrfCookies(String tenantKey, String email, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
            .header(TENANT_HEADER, tenantKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "email":"%s",
                  "password":"%s"
                }
                """.formatted(email, password)))
            .andExpect(status().isOk())
            .andReturn();

        Cookie authCookie = loginResult.getResponse().getCookie(AUTH_COOKIE);
        Cookie csrfCookie = loginResult.getResponse().getCookie(CSRF_COOKIE);
        assertNotNull(authCookie, "Expected AUTH_TOKEN cookie after successful login");
        assertNotNull(csrfCookie, "Expected XSRF-TOKEN cookie after successful login");
        return new Cookie[] { authCookie, csrfCookie };
        }

    public void submitContact(String tenantKey, String email, String subject, String message) throws Exception {
        mockMvc.perform(post("/api/contact")
                .header(TENANT_HEADER, tenantKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name":"Tenant Contact",
                          "email":"%s",
                          "subject":"%s",
                          "message":"%s"
                        }
                        """.formatted(email, subject, message)))
                .andExpect(status().isOk());
    }

    public TenantAdminSession createTenantAdminSession(
            String tenantKeyPrefix,
            String adminEmailPrefix,
            String adminPassword) throws Exception {
        Tenant tenant = createTenant(tenantKeyPrefix);
        String adminEmail = adminEmailPrefix + UUID.randomUUID() + "@example.com";
        User admin = createAdminUser(tenant, adminEmail, adminPassword);
        Cookie[] cookies = loginAndGetAuthAndCsrfCookies(tenant.getTenantKey(), adminEmail, adminPassword);
        return new TenantAdminSession(tenant, admin, cookies[0], cookies[1]);
    }
}
