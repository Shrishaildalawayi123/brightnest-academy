package com.shrishailacademy.security;

import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoleBasedAccessIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Test
        void adminDashboardShouldRedirectToLoginWhenUnauthenticated() throws Exception {
                mockMvc.perform(get("/admin-dashboard.html")
                                .accept(MediaType.TEXT_HTML))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(header().string("Location", "/login.html"));
        }

        @Test
        void adminDashboardShouldRedirectToIndexWhenAuthenticatedAsStudent() throws Exception {
                String email = "student-" + UUID.randomUUID() + "@example.com";
                String password = "Student@123";

                String registerPayload = """
                                {
                                  \"name\": \"Student User\",
                                  \"email\": \"%s\",
                                  \"password\": \"%s\",
                                  \"phone\": \"9876543210\"
                                }
                                """.formatted(email, password);

                MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registerPayload))
                                .andExpect(status().isOk())
                                .andReturn();

                Cookie[] cookies = registerResult.getResponse().getCookies();

                mockMvc.perform(get("/admin-dashboard.html")
                                .accept(MediaType.TEXT_HTML)
                                .cookie(cookies))
                                .andExpect(status().is3xxRedirection())
                                .andExpect(header().string("Location", "/index.html"));
        }

        @Test
        void adminDashboardShouldLoadWhenAuthenticatedAsAdmin() throws Exception {
                String email = "admin-" + UUID.randomUUID() + "@example.com";
                String rawPassword = "Admin@123!";

                User admin = new User();
                admin.setName("Admin User");
                admin.setEmail(email);
                admin.setPhone("9876543210");
                admin.setRole(User.Role.ADMIN);
                admin.setPassword(passwordEncoder.encode(rawPassword));
                userRepository.save(admin);

                String loginPayload = """
                                {
                                  \"email\": \"%s\",
                                  \"password\": \"%s\"
                                }
                                """.formatted(email, rawPassword);

                MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginPayload))
                                .andExpect(status().isOk())
                                .andReturn();

                Cookie[] cookies = loginResult.getResponse().getCookies();

                mockMvc.perform(get("/admin-dashboard.html")
                                .accept(MediaType.TEXT_HTML)
                                .cookie(cookies))
                                .andExpect(status().isOk());
        }
}
