package com.shrishailacademy.integration;

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

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security headers validation — production must return:
 * - Strict-Transport-Security
 * - X-Content-Type-Options: nosniff
 * - X-Frame-Options: DENY
 * - Content-Security-Policy
 * - X-XSS-Protection
 * - Referrer-Policy
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityHeadersIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicEndpointShouldReturnSecurityHeaders() throws Exception {
        mockMvc.perform(get("/api/courses")
                .header("X-Tenant-ID", "default"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().exists("X-Frame-Options"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().exists("X-XSS-Protection"))
                .andExpect(header().exists("Cache-Control"));
    }

    @Test
    void cspHeaderShouldBePresent() throws Exception {
        mockMvc.perform(get("/api/courses")
                .header("X-Tenant-ID", "default"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Security-Policy"));
    }

    @Test
    void referrerPolicyShouldBePresent() throws Exception {
        mockMvc.perform(get("/api/courses")
                .header("X-Tenant-ID", "default"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Referrer-Policy"));
    }

    @Test
    void loginResponseShouldSetCookiesWithCorrectAttributes() throws Exception {
        String email = "cookie-" + UUID.randomUUID() + "@example.com";

        // Register first
        mockMvc.perform(post("/api/auth/register")
                .header("X-Tenant-ID", "default")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"CookieTest","email":"%s","password":"Cookie@123!","phone":"9999999999"}
                        """.formatted(email)))
                .andExpect(status().isOk());

        // Login
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .header("X-Tenant-ID", "default")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"Cookie@123!"}
                        """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();

        // Verify cookies exist
        Cookie authCookie = result.getResponse().getCookie("AUTH_TOKEN");
        Cookie csrfCookie = result.getResponse().getCookie("XSRF-TOKEN");
        Cookie refreshCookie = result.getResponse().getCookie("REFRESH_TOKEN");

        assert authCookie != null : "AUTH_TOKEN cookie must be set";
        assert csrfCookie != null : "XSRF-TOKEN cookie must be set";
        assert refreshCookie != null : "REFRESH_TOKEN cookie must be set";
        assert authCookie.isHttpOnly() : "AUTH_TOKEN must be HttpOnly";
        assert !csrfCookie.isHttpOnly() : "XSRF-TOKEN must NOT be HttpOnly (frontend needs it)";
    }

    @Test
    void errorResponseShouldReturnSecurityHeaders() throws Exception {
        mockMvc.perform(get("/api/users")
                .header("X-Tenant-ID", "default")) // No auth
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().exists("X-Frame-Options"));
    }
}
