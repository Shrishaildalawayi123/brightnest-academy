package com.shrishailacademy.integration;

import com.shrishailacademy.model.Tenant;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.UserRepository;
import com.shrishailacademy.security.JwtTokenProvider;
import com.shrishailacademy.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActuatorSecurityIntegrationTest {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String DEFAULT_TENANT_KEY = "default";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private TenantService tenantService;

    @BeforeEach
    void setup() {
        userRepository.deleteAll();
    }

    @Test
    void healthIsPublicButMetricsRequiresAdmin() throws Exception {
        int healthStatus = mockMvc.perform(get("/actuator/health")
                .header(TENANT_HEADER, DEFAULT_TENANT_KEY))
                .andReturn()
                .getResponse()
                .getStatus();
        assertThat(healthStatus).isIn(200, 503);

        mockMvc.perform(get("/actuator/metrics")
                .header(TENANT_HEADER, DEFAULT_TENANT_KEY))
                .andExpect(status().isUnauthorized());

        String studentToken = bearer(User.Role.STUDENT);
        mockMvc.perform(get("/actuator/metrics")
                .header(TENANT_HEADER, DEFAULT_TENANT_KEY)
                .header(HttpHeaders.AUTHORIZATION, studentToken))
                .andExpect(status().isForbidden());

        String adminToken = bearer(User.Role.ADMIN);
        mockMvc.perform(get("/actuator/metrics")
                .header(TENANT_HEADER, DEFAULT_TENANT_KEY)
                .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isOk());
    }

    private String bearer(User.Role role) {
        Tenant defaultTenant = tenantService.ensureDefaultTenantExists();
        String email = role.name().toLowerCase() + "+" + UUID.randomUUID() + "@example.com";
        User user = new User();
        user.setTenant(defaultTenant);
        user.setName(role.name() + " User");
        user.setEmail(email);
        user.setPassword("Password@123");
        user.setRole(role);
        userRepository.save(user);
        return "Bearer "
                + jwtTokenProvider.generateTokenFromUsername(email, "ROLE_" + role.name(), defaultTenant.getId());
    }
}
