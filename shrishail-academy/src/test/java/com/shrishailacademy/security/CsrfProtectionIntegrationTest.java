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

  private static final String TENANT_HEADER = "X-Tenant-ID";
  private static final String DEFAULT_TENANT_KEY = "default";

  @Autowired
  private MockMvc mockMvc;

  @Test
  void logoutShouldSucceedWithoutCsrfHeader() throws Exception {
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
        .header(TENANT_HEADER, DEFAULT_TENANT_KEY)
        .contentType(MediaType.APPLICATION_JSON)
        .content(registerPayload))
        .andExpect(status().isOk())
        .andReturn();

    Cookie authCookie = registerResult.getResponse().getCookie("AUTH_TOKEN");
    Cookie csrfCookie = registerResult.getResponse().getCookie("XSRF-TOKEN");

    // Logout is CSRF-exempt — users must always be able to log out
    mockMvc.perform(post("/api/auth/logout")
        .header(TENANT_HEADER, DEFAULT_TENANT_KEY)
        .cookie(authCookie, csrfCookie))
        .andExpect(status().isOk());
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
        .header(TENANT_HEADER, DEFAULT_TENANT_KEY)
        .contentType(MediaType.APPLICATION_JSON)
        .content(registerPayload))
        .andExpect(status().isOk())
        .andReturn();

    Cookie authCookie = registerResult.getResponse().getCookie("AUTH_TOKEN");
    Cookie csrfCookie = registerResult.getResponse().getCookie("XSRF-TOKEN");

    mockMvc.perform(post("/api/auth/logout")
        .header(TENANT_HEADER, DEFAULT_TENANT_KEY)
        .cookie(authCookie, csrfCookie)
        .header("X-CSRF-Token", csrfCookie.getValue()))
        .andExpect(status().isOk());
  }
}
