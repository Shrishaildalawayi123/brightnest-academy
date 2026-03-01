package com.shrishailacademy.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "rate.limit.login.max=2",
        "rate.limit.login.window-seconds=60"
})
class RateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginShouldReturn429AfterConfiguredThreshold() throws Exception {
        String loginPayload = """
                {
                  "email": "invalid@example.com",
                  "password": "Wrong@123"
                }
                """;

        ResultActions first = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload));
        first.andExpect(status().isUnauthorized());

        ResultActions second = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload));
        second.andExpect(status().isUnauthorized());

        ResultActions third = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload));
        third.andExpect(status().isTooManyRequests());
    }
}
