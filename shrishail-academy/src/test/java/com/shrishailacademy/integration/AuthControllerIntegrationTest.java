package com.shrishailacademy.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginShouldSucceedForRegisteredUser() throws Exception {
        String email = "login-" + UUID.randomUUID() + "@example.com";
        String password = "Student@123";

        String registerPayload = """
                {
                  \"name\": \"Login Student\",
                  \"email\": \"%s\",
                  \"password\": \"%s\",
                  \"phone\": \"9876543210\"
                }
                """.formatted(email, password);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerPayload))
                .andExpect(status().isOk());

        String loginPayload = """
                {
                  \"email\": \"%s\",
                  \"password\": \"%s\"
                }
                """.formatted(email, password);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("ROLE_STUDENT"));
    }

    @Test
    void loginShouldFailWithInvalidPassword() throws Exception {
        String email = "badpass-" + UUID.randomUUID() + "@example.com";
        String password = "Student@123";

        String registerPayload = """
                {
                  \"name\": \"Bad Pass Student\",
                  \"email\": \"%s\",
                  \"password\": \"%s\",
                  \"phone\": \"9876543210\"
                }
                """.formatted(email, password);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerPayload))
                .andExpect(status().isOk());

        String loginPayload = """
                {
                  \"email\": \"%s\",
                  \"password\": \"WrongPassword@123\"
                }
                """.formatted(email);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void loginShouldFailWhenUserDoesNotExist() throws Exception {
        String email = "missing-" + UUID.randomUUID() + "@example.com";
        String loginPayload = """
                {
                        \"email\": \"%s\",
                        \"password\": \"Student@123\"
                }
                """.formatted(email);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }
}
