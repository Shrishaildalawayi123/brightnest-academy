package com.shrishailacademy.integration;

import com.shrishailacademy.model.Course;
import com.shrishailacademy.model.Tenant;
import com.shrishailacademy.repository.CourseRepository;
import com.shrishailacademy.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicApiIntegrationTest {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String DEFAULT_TENANT_KEY = "default";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TenantService tenantService;

    @BeforeEach
    void ensureAtLeastOneCourse() {
        Tenant defaultTenant = tenantService.ensureDefaultTenantExists();
        if (!courseRepository.findAllByTenantId(defaultTenant.getId()).isEmpty()) {
            return;
        }

        Course course = new Course();
        course.setTenant(defaultTenant);
        course.setTitle("Seeded Public Course");
        course.setDescription("Public endpoint seed data");
        course.setDuration("1 month");
        course.setIcon("book");
        course.setColor("#336699");
        course.setFee(new BigDecimal("1000.00"));
        courseRepository.save(course);
    }

    @Test
    void healthEndpointShouldReturnUpStatus() throws Exception {
        mockMvc.perform(get("/health")
                .header(TENANT_HEADER, DEFAULT_TENANT_KEY))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.version").value("1.0.0"))
                .andExpect(jsonPath("$.timestamp").isNumber());
    }

    @Test
    void publicCoursesEndpointShouldBeAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/courses")
                .header(TENANT_HEADER, DEFAULT_TENANT_KEY))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void protectedEndpointShouldRejectAnonymousAccess() throws Exception {
        mockMvc.perform(get("/api/users/me")
                .header(TENANT_HEADER, DEFAULT_TENANT_KEY))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Unauthorized - please login"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
