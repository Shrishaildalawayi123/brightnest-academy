package com.shrishailacademy.chaos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CHAOS TEST 6 — API Failure Resilience
 *
 * Sends malformed JSON, incorrect content types, missing required fields,
 * and other invalid input to verify the API fails gracefully with proper
 * HTTP status codes and does not crash or leak internal details.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiFailureResilienceTest {

    private static final String TENANT = "default";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void malformedJsonShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json!!!"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void wrongContentTypeShouldReturn415() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.TEXT_PLAIN)
                .content("this is plain text"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void missingRequiredFieldsShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"missing-name@example.com"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void nullValuesShouldReturn400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":null,"email":null,"password":null,"phone":null}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void extraUnknownFieldsShouldBeIgnored() throws Exception {
        // Jackson defaults to ignoring unknown properties; should not crash
        mockMvc.perform(post("/api/auth/login")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"test@example.com","password":"Pass@123!","unknownField":"xyz","anotherField":999}
                        """))
                // Should process normally (401 because user doesn't exist)
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonexistentEndpointShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/nonexistent-endpoint")
                .header("X-Tenant-ID", TENANT))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOnPostOnlyEndpointShouldReturn405() throws Exception {
        mockMvc.perform(get("/api/auth/register")
                .header("X-Tenant-ID", TENANT))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void malformedJsonShouldNotLeakInternalDetails() throws Exception {
        String result = mockMvc.perform(post("/api/auth/register")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{{{{"))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Response must not contain stack traces or class names
        assertThat(result)
                .doesNotContain("com.shrishailacademy")
                .doesNotContain("java.lang.")
                .doesNotContain("at org.springframework")
                .doesNotContain("Exception");
    }

    @Test
    void apiShouldStayHealthyAfterMalformedRequestBurst() throws Exception {
        // Fire 20 malformed requests
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(post("/api/auth/register")
                    .header("X-Tenant-ID", TENANT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{bad:json:" + i + "}"));
        }

        // Server must still be healthy
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void loginWithEmptyCredentialsShouldReturn400Or401() throws Exception {
        int status = mockMvc.perform(post("/api/auth/login")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"","password":""}
                        """))
                .andReturn()
                .getResponse()
                .getStatus();
        assertThat(status).isIn(400, 401);
    }
}
