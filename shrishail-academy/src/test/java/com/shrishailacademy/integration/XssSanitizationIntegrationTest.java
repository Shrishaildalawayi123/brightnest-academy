package com.shrishailacademy.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class XssSanitizationIntegrationTest {

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
    private ObjectMapper objectMapper;

    @Autowired
    private TenantService tenantService;

    @BeforeEach
    void setup() {
        courseRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void courseCreationSanitizesStoredFields() throws Exception {
        String adminToken = bearer(User.Role.ADMIN);

        String payload = """
                {
                  "title": "<script>alert('t')</script>Title",
                  "description": "<script>alert(1)</script>",
                  "duration": "<b>10 weeks</b>",
                  "icon": "<img src=x onerror=alert(1)>",
                  "color": "<red>",
                  "fee": 99.99
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/courses")
                .header(TENANT_HEADER, DEFAULT_TENANT_KEY)
                .header(HttpHeaders.AUTHORIZATION, adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").exists())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        Long courseId = root.path("data").path("id").asLong();

        Course saved = courseRepository.findById(courseId).orElseThrow();
        assertThat(saved.getTitle()).doesNotContain("<", ">");
        assertThat(saved.getDescription()).doesNotContain("<", ">");
        assertThat(saved.getDuration()).doesNotContain("<", ">");
        assertThat(saved.getIcon()).doesNotContain("<", ">");
        assertThat(saved.getColor()).doesNotContain("<", ">");
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