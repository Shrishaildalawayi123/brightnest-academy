package com.shrishailacademy.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ValidationAndExceptionIntegrationTest {

  private static final String TENANT_HEADER = "X-Tenant-ID";
  private static final String DEFAULT_TENANT_KEY = "default";

  @Autowired
  private MockMvc mockMvc;

  @Test
  void registerShouldReturnBadRequestForInvalidEmail() throws Exception {
    String invalidPayload = """
        {
          "name": "A",
          "email": "invalid-email",
          "password": "weak",
          "phone": "9999999999"
        }
        """;

    mockMvc.perform(post("/api/auth/register")
        .header(TENANT_HEADER, DEFAULT_TENANT_KEY)
        .contentType(MediaType.APPLICATION_JSON)
        .content(invalidPayload))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Validation Error"))
        .andExpect(jsonPath("$.message").exists())
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void loginShouldReturnUnauthorizedForInvalidPassword() throws Exception {
    String invalidLoginPayload = """
        {
          "email": "does-not-exist@example.com",
          "password": "Wrong@123"
        }
        """;

    mockMvc.perform(post("/api/auth/login")
        .header(TENANT_HEADER, DEFAULT_TENANT_KEY)
        .contentType(MediaType.APPLICATION_JSON)
        .content(invalidLoginPayload))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.error").value("Unauthorized"))
        .andExpect(jsonPath("$.message").value("Invalid email or password."))
        .andExpect(jsonPath("$.timestamp").exists());
  }
}
