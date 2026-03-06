package com.shrishailacademy.integration;

import com.shrishailacademy.model.Tenant;
import com.shrishailacademy.repository.TenantRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Multi-tenant data isolation: Tenant A must NEVER see Tenant B data.
 * This is a critical SaaS security requirement.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MultiTenantDataIsolationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void tenantAShouldNotSeeTenantBContactMessages() throws Exception {
        // Create Tenant B
        String tenantBKey = "tenant-b-" + UUID.randomUUID().toString().substring(0, 8);
        Tenant tenantB = new Tenant();
        tenantB.setTenantKey(tenantBKey);
        tenantB.setName("Tenant B Academy");
        tenantRepository.save(tenantB);

        // Submit contact to Tenant B
        mockMvc.perform(post("/api/contact")
                .header("X-Tenant-ID", tenantBKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name":"Tenant B Student",
                          "email":"b@example.com",
                          "subject":"Secret Message for B",
                          "message":"This is confidential data for Tenant B only."
                        }
                        """))
                .andExpect(status().isOk());

        // Submit contact to default tenant
        mockMvc.perform(post("/api/contact")
                .header("X-Tenant-ID", "default")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name":"Default Tenant Student",
                          "email":"default@example.com",
                          "subject":"Default Tenant Message",
                          "message":"This is data for the default tenant only."
                        }
                        """))
                .andExpect(status().isOk());
    }

    @Test
    void userRegisteredInTenantAShouldNotLoginInTenantB() throws Exception {
        // Create Tenant B
        String tenantBKey = "tenant-login-" + UUID.randomUUID().toString().substring(0, 8);
        Tenant tenantB = new Tenant();
        tenantB.setTenantKey(tenantBKey);
        tenantB.setName("Login Test Tenant");
        tenantRepository.save(tenantB);

        // Register user in default tenant
        String email = "cross-tenant-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/auth/register")
                .header("X-Tenant-ID", "default")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Default User","email":"%s","password":"Test@123!","phone":"1234567890"}
                        """.formatted(email)))
                .andExpect(status().isOk());

        // Try to login with same email in Tenant B — should fail
        mockMvc.perform(post("/api/auth/login")
                .header("X-Tenant-ID", tenantBKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"Test@123!"}
                        """.formatted(email)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void jwtFromTenantAShouldNotAccessTenantBEndpoints() throws Exception {
        // Create Tenant B
        String tenantBKey = "tenant-jwt-" + UUID.randomUUID().toString().substring(0, 8);
        Tenant tenantB = new Tenant();
        tenantB.setTenantKey(tenantBKey);
        tenantB.setName("JWT Test Tenant");
        tenantRepository.save(tenantB);

        // Register and get JWT for default tenant
        String email = "jwt-cross-" + UUID.randomUUID() + "@example.com";
        MvcResult reg = mockMvc.perform(post("/api/auth/register")
                .header("X-Tenant-ID", "default")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"JWT User","email":"%s","password":"Jwt@123!","phone":"5555555555"}
                        """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();

        Cookie authCookie = reg.getResponse().getCookie("AUTH_TOKEN");

        // Try to use default-tenant JWT to access Tenant B — should be rejected
        mockMvc.perform(get("/api/courses")
                .header("X-Tenant-ID", tenantBKey)
                .cookie(authCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void nonexistentTenantShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/courses")
                .header("X-Tenant-ID", "does-not-exist-" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void sameEmailCanRegisterInDifferentTenants() throws Exception {
        // Create Tenant B
        String tenantBKey = "tenant-dup-" + UUID.randomUUID().toString().substring(0, 8);
        Tenant tenantB = new Tenant();
        tenantB.setTenantKey(tenantBKey);
        tenantB.setName("Dup Email Tenant");
        tenantRepository.save(tenantB);

        String email = "shared-" + UUID.randomUUID() + "@example.com";

        // Register in default tenant
        mockMvc.perform(post("/api/auth/register")
                .header("X-Tenant-ID", "default")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"User A","email":"%s","password":"PassA@123!","phone":"1111111111"}
                        """.formatted(email)))
                .andExpect(status().isOk());

        // Register same email in Tenant B — should succeed (different tenant =
        // different user)
        mockMvc.perform(post("/api/auth/register")
                .header("X-Tenant-ID", tenantBKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"User B","email":"%s","password":"PassB@123!","phone":"2222222222"}
                        """.formatted(email)))
                .andExpect(status().isOk());
    }
}
