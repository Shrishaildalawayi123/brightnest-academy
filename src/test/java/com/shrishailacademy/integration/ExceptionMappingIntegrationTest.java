package com.shrishailacademy.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExceptionMappingIntegrationTest {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String DEFAULT_TENANT_KEY = "default";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unsupportedMethodReturns405() throws Exception {
        mockMvc.perform(get("/api/auth/login")
                .header(TENANT_HEADER, DEFAULT_TENANT_KEY)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void missingRouteReturns404() throws Exception {
        mockMvc.perform(get("/api/does-not-exist")
                .header(TENANT_HEADER, DEFAULT_TENANT_KEY))
                .andExpect(status().isNotFound());
    }

    @Test
    void badJsonReturns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .header(TENANT_HEADER, DEFAULT_TENANT_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid}"))
                .andExpect(status().isBadRequest());
    }
}