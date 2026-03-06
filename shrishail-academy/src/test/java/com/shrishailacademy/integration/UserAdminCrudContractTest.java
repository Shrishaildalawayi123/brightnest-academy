package com.shrishailacademy.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shrishailacademy.model.Tenant;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.UserRepository;
import com.shrishailacademy.security.JwtTokenProvider;
import com.shrishailacademy.service.TenantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserAdminCrudContractTest {

        private static final String TENANT_HEADER = "X-Tenant-ID";
        private static final String DEFAULT_TENANT_KEY = "default";

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private JwtTokenProvider jwtTokenProvider;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private TenantService tenantService;

        @BeforeEach
        void setup() {
                userRepository.deleteAll();
        }

        @Test
        void adminCanCreateUpdateAndDeleteUser() throws Exception {
                String adminToken = bearer(User.Role.ADMIN);
                String email = "user-" + UUID.randomUUID() + "@example.com";

                String createPayload = """
                                {
                                  "name": "Student One",
                                  "email": "%s",
                                  "password": "Student@123",
                                  "phone": "1234567890",
                                  "role": "STUDENT"
                                }
                                """.formatted(email);

                MvcResult createResult = mockMvc.perform(post("/api/users")
                                .header(TENANT_HEADER, DEFAULT_TENANT_KEY)
                                .header(HttpHeaders.AUTHORIZATION, adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createPayload))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.email").value(email))
                                .andReturn();

                JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsByteArray())
                                .path("data");
                long userId = created.path("id").asLong();

                String updatePayload = """
                                {
                                  "name": "Updated User",
                                  "phone": "9876543210"
                                }
                                """;

                mockMvc.perform(put("/api/users/" + userId)
                                .header(TENANT_HEADER, DEFAULT_TENANT_KEY)
                                .header(HttpHeaders.AUTHORIZATION, adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updatePayload))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.name").value("Updated User"));

                mockMvc.perform(delete("/api/users/" + userId)
                                .header(TENANT_HEADER, DEFAULT_TENANT_KEY)
                                .header(HttpHeaders.AUTHORIZATION, adminToken))
                                .andExpect(status().isOk());

                assertThat(userRepository.findById(userId)).isEmpty();
        }

        private String bearer(User.Role role) {
                Tenant defaultTenant = tenantService.ensureDefaultTenantExists();
                String email = role.name().toLowerCase() + "+" + UUID.randomUUID() + "@example.com";
                User user = new User();
                user.setTenant(defaultTenant);
                user.setName(role.name() + " User");
                user.setEmail(email);
                user.setPassword("Password@123");
                user.setRole(role);
                userRepository.save(user);
                return "Bearer " + jwtTokenProvider.generateTokenFromUsername(email, "ROLE_" + role.name(),
                                defaultTenant.getId());
        }
}