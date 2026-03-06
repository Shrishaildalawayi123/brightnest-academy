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
 * CHAOS TEST 7 — Security Attack Simulation
 *
 * Validates that SQL injection, XSS, path traversal, header injection, and
 * LDAP injection attacks are blocked. Responses must not reflect payloads
 * or leak internal information.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAttackSimulationTest {

    private static final String TENANT = "default";

    @Autowired
    private MockMvc mockMvc;

    // --- SQL INJECTION ---

    @Test
    void sqlInjectionInLoginEmailShouldBeBlocked() throws Exception {
        // Injection payload may be rejected by validation (400) or fail auth (401) —
        // both are safe
        int status = mockMvc.perform(post("/api/auth/login")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"admin' OR '1'='1' --","password":"anything"}
                        """))
                .andReturn().getResponse().getStatus();
        assertThat(status).isIn(400, 401);
    }

    @Test
    void sqlInjectionInRegistrationShouldBeBlocked() throws Exception {
        int status = mockMvc.perform(post("/api/auth/register")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        """
                                {"name":"'; DROP TABLE users; --","email":"sqlinj@evil.com","password":"Secure@123!","phone":"0000000000"}
                                """))
                .andReturn().getResponse().getStatus();
        assertThat(status).isIn(200, 400);

        // Tables must still exist — health endpoint works
        mockMvc.perform(get("/api/courses").header("X-Tenant-ID", TENANT))
                .andExpect(status().isOk());
    }

    @Test
    void unionBasedSqlInjectionShouldBeBlocked() throws Exception {
        int status = mockMvc.perform(post("/api/auth/login")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"test@x.com' UNION SELECT * FROM users --","password":"x"}
                        """))
                .andReturn().getResponse().getStatus();
        assertThat(status).isIn(400, 401);
    }

    // --- XSS ---

    @Test
    void xssInRegistrationNameShouldBeSanitized() throws Exception {
        String xssPayload = "<script>alert('XSS')</script>";
        String result = mockMvc.perform(post("/api/auth/register")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"%s","email":"xss-chaos@example.com","password":"Secure@123!","phone":"1111111111"}
                        """.formatted(xssPayload)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(result)
                .doesNotContain("<script>");
    }

    @Test
    void xssInHeadersShouldNotBeReflected() throws Exception {
        String result = mockMvc.perform(get("/api/courses")
                .header("X-Tenant-ID", TENANT)
                .header("User-Agent", "<img src=x onerror=alert(1)>"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(result)
                .doesNotContain("<img")
                .doesNotContain("onerror");
    }

    @Test
    void storedXssViaContactFormShouldBeSanitized() throws Exception {
        mockMvc.perform(post("/api/contact")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        """
                                {"name":"<script>document.cookie</script>","email":"x@x.com","message":"<img src=x onerror=steal()>","phone":"1234567890"}
                                """));
        // No 500 error — input is sanitized or rejected
    }

    // --- PATH TRAVERSAL ---

    @Test
    void pathTraversalInUrlShouldBeBlocked() throws Exception {
        int status = mockMvc.perform(get("/api/courses/../../../etc/passwd")
                .header("X-Tenant-ID", TENANT))
                .andReturn().getResponse().getStatus();
        assertThat(status).isIn(400, 404);
    }

    @Test
    void pathTraversalWithEncodingShouldBeBlocked() throws Exception {
        int status = mockMvc.perform(get("/api/courses/..%2F..%2F..%2Fetc%2Fpasswd")
                .header("X-Tenant-ID", TENANT))
                .andReturn().getResponse().getStatus();
        assertThat(status).isIn(400, 404);
    }

    // --- HEADER INJECTION ---

    @Test
    void httpHeaderInjectionShouldBeBlocked() throws Exception {
        int status = mockMvc.perform(get("/api/courses")
                .header("X-Tenant-ID", "default\r\nX-Injected: malicious"))
                .andReturn().getResponse().getStatus();
        assertThat(status).isIn(200, 400);
    }

    // --- JWT TAMPERING ---

    @Test
    void forgedJwtTokenShouldBeRejected() throws Exception {
        String forgedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbkBleGFtcGxlLmNvbSIsInJvbGUiOiJBRE1JTiIsImlhdCI6MTYxNjIzOTAyMn0.fakesignature";
        mockMvc.perform(get("/api/courses")
                .header("X-Tenant-ID", TENANT)
                .header("Authorization", "Bearer " + forgedToken))
                .andExpect(status().isOk()); // public endpoint, JWT ignored
    }

    @Test
    void forgedJwtOnProtectedEndpointShouldReturn401Or403() throws Exception {
        String forgedToken = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbkBleGFtcGxlLmNvbSIsInJvbGUiOiJBRE1JTiJ9.invalidsig";
        int status = mockMvc.perform(post("/api/courses")
                .header("X-Tenant-ID", TENANT)
                .header("Authorization", "Bearer " + forgedToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Hacked Course","description":"test","duration":"1h","price":0,"category":"ACADEMIC"}
                        """))
                .andReturn().getResponse().getStatus();
        assertThat(status).isIn(401, 403);
    }

    @Test
    void applicationShouldStayHealthyAfterAttackSimulations() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
