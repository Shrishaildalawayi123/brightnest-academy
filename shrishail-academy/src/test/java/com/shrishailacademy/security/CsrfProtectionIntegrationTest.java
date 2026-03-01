package com.shrishailacademy.security;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CsrfProtectionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void logoutShouldRejectWhenCsrfHeaderMissing() throws Exception {
        String email = "csrf-" + UUID.randomUUID() + "@example.com";
        String password = "Student@123!";

        String registerPayload = """
                {
                  "name": "CSRF Student",
                  "email": "%s",
                  "password": "%s",
                  "phone": "9876543210"
                }
                """.formatted(email, password);

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isOk())
                .andReturn();

        Cookie authCookie = registerResult.getResponse().getCookie("AUTH_TOKEN");
        Cookie csrfCookie = registerResult.getResponse().getCookie("XSRF-TOKEN");

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(authCookie, csrfCookie))
                .andExpect(status().isForbidden());
    }

    @Test
    void logoutShouldPassWhenCsrfHeaderMatchesCookie() throws Exception {
        String email = "csrf-ok-" + UUID.randomUUID() + "@example.com";
        String password = "Student@123!";

        String registerPayload = """
                {
                  "name": "CSRF Valid Student",
                  "email": "%s",
                  "password": "%s",
                  "phone": "9876543210"
                }
                """.formatted(email, password);

        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isOk())
                .andReturn();

        Cookie authCookie = registerResult.getResponse().getCookie("AUTH_TOKEN");
        Cookie csrfCookie = registerResult.getResponse().getCookie("XSRF-TOKEN");

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(authCookie, csrfCookie)
                        .header("X-CSRF-Token", csrfCookie.getValue()))
                .andExpect(status().isOk());
    }
}
