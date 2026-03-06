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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtLifecycleIntegrationTest {

        private static final String TENANT_HEADER = "X-Tenant-ID";
        private static final String DEFAULT_TENANT_KEY = "default";

        @Autowired
        private MockMvc mockMvc;

        @Test
        void registerRefreshAndLogoutLifecycleWorks() throws Exception {
                String email = "jwt-" + UUID.randomUUID() + "@example.com";
                String registerPayload = """
                                {
                                  "name": "JWT User",
                                  "email": "%s",
                                  "password": "JwtUser@123",
                                  "phone": "1112223333"
                                }
                                """.formatted(email);

                MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                                .header(TENANT_HEADER, DEFAULT_TENANT_KEY)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registerPayload))
                                .andExpect(status().isOk())
                                .andExpect(cookie().exists("AUTH_TOKEN"))
                                .andExpect(cookie().exists("REFRESH_TOKEN"))
                                .andReturn();

                Cookie refreshCookie = registerResult.getResponse().getCookie("REFRESH_TOKEN");
                Cookie authCookie = registerResult.getResponse().getCookie("AUTH_TOKEN");
                Cookie csrfCookie = registerResult.getResponse().getCookie("XSRF-TOKEN");

                mockMvc.perform(post("/api/auth/refresh")
                                .header(TENANT_HEADER, DEFAULT_TENANT_KEY)
                                .cookie(refreshCookie))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.token").isString());

                mockMvc.perform(post("/api/auth/logout")
                                .header(TENANT_HEADER, DEFAULT_TENANT_KEY)
                                .cookie(authCookie, csrfCookie)
                                .header("X-CSRF-Token", csrfCookie.getValue()))
                                .andExpect(status().isOk());
        }
}
