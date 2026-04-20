package com.shrishailacademy.integration;

import com.shrishailacademy.integration.support.TenantAdminSession;
import com.shrishailacademy.integration.support.TenantAdminTestHelper;
import com.shrishailacademy.model.TeacherApplication;
import com.shrishailacademy.repository.TeacherApplicationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TeacherApplicationTenantIsolationIntegrationTest {

    private static final String TENANT_HEADER = TenantAdminTestHelper.TENANT_HEADER;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TenantAdminTestHelper tenantAdminTestHelper;

        @Autowired
        private TeacherApplicationRepository teacherApplicationRepository;

    @Test
    void teacherApplicationAdminEndpointsShouldReturnTenantScopedData() throws Exception {
        String adminPassword = "Admin@123";
        TenantAdminSession tenantA = tenantAdminTestHelper
                .createTenantAdminSession("ta-a-", "ta-admin-a-", adminPassword);
        TenantAdminSession tenantB = tenantAdminTestHelper
                .createTenantAdminSession("ta-b-", "ta-admin-b-", adminPassword);

        submitTeacherApplication(tenantA.tenant().getTenantKey(), "ta-a-1@example.com", "Teacher A One");
        submitTeacherApplication(tenantB.tenant().getTenantKey(), "ta-b-1@example.com", "Teacher B One");
        submitTeacherApplication(tenantB.tenant().getTenantKey(), "ta-b-2@example.com", "Teacher B Two");

        TeacherApplication tenantAApplication = teacherApplicationRepository
                .findByTenantIdOrderByCreatedAtDesc(tenantA.tenant().getId())
                .stream()
                .findFirst()
                .orElseThrow();

        mockMvc.perform(get("/api/teacher-applications")
                        .header(TENANT_HEADER, tenantA.tenant().getTenantKey())
                        .cookie(tenantA.authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email").value("ta-a-1@example.com"));

        mockMvc.perform(get("/api/teacher-applications")
                        .header(TENANT_HEADER, tenantB.tenant().getTenantKey())
                        .cookie(tenantB.authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(put("/api/teacher-applications/{id}/status", tenantAApplication.getId())
                                                                                                .param("status", "REVIEWED")
                        .header(TENANT_HEADER, tenantA.tenant().getTenantKey())
                        .header("X-CSRF-Token", tenantA.csrfCookie().getValue())
                        .cookie(tenantA.authCookie(), tenantA.csrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Application status updated to REVIEWED"));

        mockMvc.perform(get("/api/teacher-applications/stats")
                        .header(TENANT_HEADER, tenantA.tenant().getTenantKey())
                        .cookie(tenantA.authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.new").value(0))
                .andExpect(jsonPath("$.reviewed").value(1));

        mockMvc.perform(get("/api/teacher-applications/stats")
                        .header(TENANT_HEADER, tenantB.tenant().getTenantKey())
                        .cookie(tenantB.authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.new").value(2));
    }

    @Test
    void teacherApplicationAdminEndpointsShouldRejectCrossTenantTokenHeaderMismatch() throws Exception {
        String adminPassword = "Admin@123";
        TenantAdminSession tenantA = tenantAdminTestHelper
                .createTenantAdminSession("ta-mismatch-a-", "ta-mismatch-admin-a-", adminPassword);
        TenantAdminSession tenantB = tenantAdminTestHelper
                .createTenantAdminSession("ta-mismatch-b-", "ta-mismatch-admin-b-", adminPassword);

        submitTeacherApplication(
                tenantB.tenant().getTenantKey(),
                "ta-mismatch-b@example.com",
                "Teacher Mismatch");

        mockMvc.perform(get("/api/teacher-applications")
                        .header(TENANT_HEADER, tenantB.tenant().getTenantKey())
                        .cookie(tenantA.authCookie()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/teacher-applications/stats")
                        .header(TENANT_HEADER, tenantB.tenant().getTenantKey())
                        .cookie(tenantA.authCookie()))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/teacher-applications/1/status")
                                                                                                .param("status", "REVIEWED")
                        .header(TENANT_HEADER, tenantB.tenant().getTenantKey())
                                                                                                .header("X-CSRF-Token", tenantA.csrfCookie().getValue())
                                                                                                .cookie(tenantA.authCookie(), tenantA.csrfCookie()))
                .andExpect(status().isForbidden());
    }

    private void submitTeacherApplication(String tenantKey, String email, String fullName) throws Exception {
        mockMvc.perform(post("/api/teacher-applications")
                        .header(TENANT_HEADER, tenantKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName":"%s",
                                  "email":"%s",
                                  "phone":"9876543210",
                                  "subjectExpertise":"Mathematics",
                                  "qualification":"MSc",
                                  "city":"Pune",
                                  "teachingMode":"ONLINE",
                                  "experience":"5 years",
                                  "motivation":"I enjoy teaching"
                                }
                                """.formatted(fullName, email)))
                .andExpect(status().isOk());
    }
}
