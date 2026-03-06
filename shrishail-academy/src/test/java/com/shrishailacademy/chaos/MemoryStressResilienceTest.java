package com.shrishailacademy.chaos;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CHAOS TEST 5 — Memory Stress Resilience
 *
 * Sends oversized payloads, deeply nested JSON, and very long strings to verify
 * the application handles them gracefully without OutOfMemoryError or crashes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MemoryStressResilienceTest {

    private static final String TENANT = "default";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void oversizedNameFieldShouldBeRejected() throws Exception {
        String longName = "A".repeat(10_000);
        mockMvc.perform(post("/api/auth/register")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"%s","email":"oversize@example.com","password":"Secure@123!","phone":"9999999999"}
                        """.formatted(longName)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void oversizedJsonBodyShouldNotCrashServer() throws Exception {
        // 100KB payload of repeated data
        String bigBody = "{\"name\":\"" + "X".repeat(100_000)
                + "\",\"email\":\"big@x.com\",\"password\":\"P@ss1!\",\"phone\":\"1234567890\"}";
        mockMvc.perform(post("/api/auth/register")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(bigBody))
                .andExpect(status().is4xxClientError());

        // Server still healthy
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void deeplyNestedJsonShouldNotCauseStackOverflow() throws Exception {
        // Build deeply nested JSON: {"a":{"a":{"a":...}}}
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            sb.append("{\"a\":");
        }
        sb.append("1");
        for (int i = 0; i < 200; i++) {
            sb.append("}");
        }

        mockMvc.perform(post("/api/auth/register")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(sb.toString()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void veryLongUrlParameterShouldNotCrashServer() throws Exception {
        String longParam = "a".repeat(5_000);
        mockMvc.perform(get("/api/courses")
                .header("X-Tenant-ID", TENANT)
                .param("search", longParam))
                .andExpect(status().isOk());

        // Confirm server health after
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void repeatedLargeRequestsShouldNotExhaustMemory() throws Exception {
        String mediumBody = """
                {"name":"%s","email":"mem@example.com","password":"Secure@123!","phone":"1234567890"}
                """.formatted("Y".repeat(5_000));

        for (int i = 0; i < 30; i++) {
            mockMvc.perform(post("/api/auth/register")
                    .header("X-Tenant-ID", TENANT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mediumBody));
        }

        // GC should handle it — server must still respond
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void emptyBodyShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(""))
                .andExpect(status().isBadRequest());
    }
}
