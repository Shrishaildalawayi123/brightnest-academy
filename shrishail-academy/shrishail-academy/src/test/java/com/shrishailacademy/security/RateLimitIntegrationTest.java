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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
                "rate.limit.api.max=3",
                "rate.limit.api.window-seconds=60",
                "rate.limit.login.max=2",
                "rate.limit.login.window-seconds=60"
})
class RateLimitIntegrationTest {

        private static final String TENANT_HEADER = "X-Tenant-ID";
        private static final String DEFAULT_TENANT_KEY = "default";

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
                                .header(TENANT_HEADER, DEFAULT_TENANT_KEY)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginPayload));
                first.andExpect(status().isUnauthorized());

                ResultActions second = mockMvc.perform(post("/api/auth/login")
                                .header(TENANT_HEADER, DEFAULT_TENANT_KEY)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginPayload));
                second.andExpect(status().isUnauthorized());

                ResultActions third = mockMvc.perform(post("/api/auth/login")
                                .header(TENANT_HEADER, DEFAULT_TENANT_KEY)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginPayload));
                third.andExpect(status().isTooManyRequests());
        }

        @Test
        void apiShouldReturn429AfterConfiguredThreshold() throws Exception {
                ResultActions first = mockMvc.perform(get("/api/courses")
                                .header(TENANT_HEADER, DEFAULT_TENANT_KEY));
                first.andExpect(status().isOk());

                ResultActions second = mockMvc.perform(get("/api/courses")
                                .header(TENANT_HEADER, DEFAULT_TENANT_KEY));
                second.andExpect(status().isOk());

                ResultActions third = mockMvc.perform(get("/api/courses")
                                .header(TENANT_HEADER, DEFAULT_TENANT_KEY));
                third.andExpect(status().isOk());

                ResultActions fourth = mockMvc.perform(get("/api/courses")
                                .header(TENANT_HEADER, DEFAULT_TENANT_KEY));
                fourth.andExpect(status().isTooManyRequests());
        }
}
