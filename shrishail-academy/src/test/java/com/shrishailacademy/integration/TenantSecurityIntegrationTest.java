package com.shrishailacademy.integration;

import com.shrishailacademy.model.Tenant;
import com.shrishailacademy.repository.TenantRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TenantSecurityIntegrationTest {

    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String DEFAULT_TENANT_KEY = "default";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void logoutShouldRejectTenantHeaderMismatchWhenJwtIsPresent() throws Exception {
        String email = "tenant-mismatch-" + UUID.randomUUID() + "@example.com";
        String password = "Student@123";

        String registerPayload = """
                {
                  "name": "Tenant Security Student",
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

        String authToken = extractCookieValue(registerResult.getResponse().getHeaders(HttpHeaders.SET_COOKIE),
                "AUTH_TOKEN");

        Tenant anotherTenant = tenantRepository.findByTenantKey("another-tenant")
                .orElseGet(() -> {
                    Tenant created = new Tenant();
                    created.setTenantKey("another-tenant");
                    created.setName("Another Tenant");
                    return tenantRepository.save(created);
                });

        mockMvc.perform(post("/api/auth/logout")
                .header(TENANT_HEADER, anotherTenant.getTenantKey())
                .cookie(new Cookie("AUTH_TOKEN", authToken))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(containsString("Tenant header does not match token tenant")));
    }

    private String extractCookieValue(List<String> setCookieHeaders, String cookieName) {
        String prefix = cookieName + "=";
        return setCookieHeaders.stream()
                .filter(header -> header.startsWith(prefix))
                .map(header -> header.substring(prefix.length()).split(";", 2)[0])
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing cookie: " + cookieName));
    }
}
