package com.shrishailacademy.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Validates public form submission endpoints (contact, demo booking,
 * testimonials)
 * and that admin-only management endpoints require proper auth.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicFormSubmissionIntegrationTest {

    private static final String TENANT = "default";

    @Autowired
    private MockMvc mockMvc;

    private Cookie studentAuth;

    @BeforeEach
    void setup() throws Exception {
        String email = "formtest-" + UUID.randomUUID() + "@example.com";
        MvcResult reg = mockMvc.perform(post("/api/auth/register")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"FormTester","email":"%s","password":"Test@123!","phone":"7777777777"}
                        """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        studentAuth = reg.getResponse().getCookie("AUTH_TOKEN");
    }

    // ===== CONTACT =====

    @Test
    void contactSubmitShouldSucceedWithValidData() throws Exception {
        mockMvc.perform(post("/api/contact")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name":"Test User",
                          "email":"contact@example.com",
                          "subject":"Test Subject Here",
                          "message":"This is a valid test message with sufficient length."
                        }
                        """))
                .andExpect(status().isOk());
    }

    @Test
    void contactSubmitShouldRejectMissingFields() throws Exception {
        mockMvc.perform(post("/api/contact")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"X"}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void contactListShouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/contact")
                .header("X-Tenant-ID", TENANT))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void contactListShouldRejectStudent() throws Exception {
        mockMvc.perform(get("/api/contact")
                .header("X-Tenant-ID", TENANT)
                .cookie(studentAuth))
                .andExpect(status().isForbidden());
    }

    // ===== DEMO BOOKING =====

    @Test
    void demoBookingShouldSucceedWithValidData() throws Exception {
        mockMvc.perform(post("/api/demo-booking")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "studentName":"Demo Student",
                          "email":"demo@example.com",
                          "phone":"9876543210",
                          "subject":"Mathematics",
                          "classMode":"online"
                        }
                        """))
                .andExpect(status().isOk());
    }

    @Test
    void demoBookingShouldRejectMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/demo-booking")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"studentName":"X"}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void demoBookingListShouldRequireAuth() throws Exception {
        mockMvc.perform(get("/api/demo-booking")
                .header("X-Tenant-ID", TENANT))
                .andExpect(status().isUnauthorized());
    }

    // ===== TESTIMONIALS =====

    @Test
    void approvedTestimonialsShouldBePublic() throws Exception {
        mockMvc.perform(get("/api/testimonials")
                .header("X-Tenant-ID", TENANT))
                .andExpect(status().isOk());
    }

    @Test
    void createTestimonialShouldRequireAuth() throws Exception {
        mockMvc.perform(post("/api/testimonials")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"studentName":"X","review":"Great course experience!","rating":5}
                        """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void studentShouldNotManageTestimonials() throws Exception {
        mockMvc.perform(put("/api/testimonials/1/approve")
                .header("X-Tenant-ID", TENANT)
                .cookie(studentAuth))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/testimonials/1")
                .header("X-Tenant-ID", TENANT)
                .cookie(studentAuth))
                .andExpect(status().isForbidden());
    }
}
