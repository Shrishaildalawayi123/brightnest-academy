package com.shrishailacademy.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registerAndLoginShouldWorkWithH2BackedPersistence() throws Exception {
        String email = "student-" + UUID.randomUUID() + "@example.com";
        String password = "Student@123";

        String registerPayload = """
                {
                  "name": "Integration Student",
                  "email": "%s",
                  "password": "%s",
                  "phone": "9876543210"
                }
                """.formatted(email, password);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload))
                .andExpect(status().isOk())
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(containsString("AUTH_TOKEN="))))
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(containsString("REFRESH_TOKEN="))))
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(containsString("XSRF-TOKEN="))))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").isString())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("STUDENT"));

        String loginPayload = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(containsString("AUTH_TOKEN="))))
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(containsString("REFRESH_TOKEN="))))
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE, hasItem(containsString("XSRF-TOKEN="))))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").isString())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }
}
