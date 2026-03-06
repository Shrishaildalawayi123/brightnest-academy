package com.shrishailacademy.integration;

import com.shrishailacademy.model.Course;
import com.shrishailacademy.model.Tenant;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.CourseRepository;
import com.shrishailacademy.repository.UserRepository;
import com.shrishailacademy.security.JwtTokenProvider;
import com.shrishailacademy.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthorizationStatusCodeIntegrationTest {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String DEFAULT_TENANT_KEY = "default";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private TenantService tenantService;

    @BeforeEach
    void setup() {
        courseRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void unauthenticatedRequestsReturn401() throws Exception {
        mockMvc.perform(get("/api/users")
                .header(TENANT_HEADER, DEFAULT_TENANT_KEY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentAccessingAdminEndpointGets403() throws Exception {
        String studentToken = bearer(User.Role.STUDENT);

        mockMvc.perform(get("/api/users")
                .header(TENANT_HEADER, DEFAULT_TENANT_KEY)
                .header(HttpHeaders.AUTHORIZATION, studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminAccessingStudentOnlyEndpointGets403() throws Exception {
        String adminToken = bearer(User.Role.ADMIN);

        Tenant defaultTenant = tenantService.ensureDefaultTenantExists();
        Course course = new Course();
        course.setTenant(defaultTenant);
        course.setTitle("Test Course");
        course.setDescription("Desc");
        course.setDuration("1 month");
        course.setIcon("icon");
        course.setColor("#000000");
        course.setFee(new BigDecimal("100.00"));
        courseRepository.save(course);

        mockMvc.perform(post("/api/enrollments/" + course.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .header(TENANT_HEADER, DEFAULT_TENANT_KEY)
                .header(HttpHeaders.AUTHORIZATION, adminToken))
                .andExpect(status().isForbidden());
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