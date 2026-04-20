package com.shrishailacademy.integration;

import com.shrishailacademy.integration.support.TenantAdminTestHelper;
import com.shrishailacademy.integration.support.TenantAdminSession;
import com.shrishailacademy.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserAdminTenantIsolationIntegrationTest {

    private static final String TENANT_HEADER = TenantAdminTestHelper.TENANT_HEADER;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TenantAdminTestHelper tenantAdminTestHelper;

    @Test
    void userAdminEndpointsShouldReturnTenantScopedUsers() throws Exception {
        String adminPassword = "Admin@123";
        TenantAdminSession tenantA = tenantAdminTestHelper
                .createTenantAdminSession("users-a-", "users-admin-a-", adminPassword);
        TenantAdminSession tenantB = tenantAdminTestHelper
                .createTenantAdminSession("users-b-", "users-admin-b-", adminPassword);

        User studentA = tenantAdminTestHelper.createUser(
                tenantA.tenant(),
                "Student A",
                "student-a-" + UUID.randomUUID() + "@example.com",
                "Student@123",
                User.Role.STUDENT);
        User teacherA = tenantAdminTestHelper.createUser(
                tenantA.tenant(),
                "Teacher A",
                "teacher-a-" + UUID.randomUUID() + "@example.com",
                "Teacher@123",
                User.Role.TEACHER);

        User studentB = tenantAdminTestHelper.createUser(
                tenantB.tenant(),
                "Student B",
                "student-b-" + UUID.randomUUID() + "@example.com",
                "Student@123",
                User.Role.STUDENT);
        User teacherB = tenantAdminTestHelper.createUser(
                tenantB.tenant(),
                "Teacher B",
                "teacher-b-" + UUID.randomUUID() + "@example.com",
                "Teacher@123",
                User.Role.TEACHER);

        mockMvc.perform(get("/api/users")
                        .header(TENANT_HEADER, tenantA.tenant().getTenantKey())
                        .cookie(tenantA.authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].email", hasItem(tenantA.admin().getEmail())))
                .andExpect(jsonPath("$.content[*].email", hasItem(studentA.getEmail())))
                .andExpect(jsonPath("$.content[*].email", hasItem(teacherA.getEmail())))
                .andExpect(jsonPath("$.content[*].email", not(hasItem(studentB.getEmail()))))
                .andExpect(jsonPath("$.content[*].email", not(hasItem(teacherB.getEmail()))));

        mockMvc.perform(get("/api/users/students")
                        .header(TENANT_HEADER, tenantA.tenant().getTenantKey())
                        .cookie(tenantA.authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", hasItem(studentA.getEmail())))
                .andExpect(jsonPath("$[*].email", not(hasItem(studentB.getEmail()))));

        mockMvc.perform(get("/api/users/faculty")
                        .header(TENANT_HEADER, tenantA.tenant().getTenantKey())
                        .cookie(tenantA.authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", hasItem(tenantA.admin().getEmail())))
                .andExpect(jsonPath("$[*].email", hasItem(teacherA.getEmail())))
                .andExpect(jsonPath("$[*].email", not(hasItem(teacherB.getEmail()))));
    }

    @Test
    void userAdminEndpointsShouldRejectCrossTenantTokenHeaderMismatch() throws Exception {
        String adminPassword = "Admin@123";
        TenantAdminSession tenantA = tenantAdminTestHelper
                .createTenantAdminSession("users-mismatch-a-", "users-mismatch-admin-a-", adminPassword);
        TenantAdminSession tenantB = tenantAdminTestHelper
                .createTenantAdminSession("users-mismatch-b-", "users-mismatch-admin-b-", adminPassword);

        User studentB = tenantAdminTestHelper.createUser(
                tenantB.tenant(),
                "Student B Mismatch",
                "student-b-mismatch-" + UUID.randomUUID() + "@example.com",
                "Student@123",
                User.Role.STUDENT);

        mockMvc.perform(get("/api/users")
                        .header(TENANT_HEADER, tenantB.tenant().getTenantKey())
                        .cookie(tenantA.authCookie()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users/students")
                        .header(TENANT_HEADER, tenantB.tenant().getTenantKey())
                        .cookie(tenantA.authCookie()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users/faculty")
                        .header(TENANT_HEADER, tenantB.tenant().getTenantKey())
                        .cookie(tenantA.authCookie()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/users")
                        .with(csrf())
                        .header(TENANT_HEADER, tenantB.tenant().getTenantKey())
                        .cookie(tenantA.authCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Cross Tenant User",
                                  "email":"cross-tenant-create@example.com",
                                  "password":"Strong@123",
                                  "phone":"9876543210",
                                  "role":"STUDENT"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/users/{id}", studentB.getId())
                        .with(csrf())
                        .header(TENANT_HEADER, tenantB.tenant().getTenantKey())
                        .cookie(tenantA.authCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"Updated Name"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/users/{id}", studentB.getId())
                        .with(csrf())
                        .header(TENANT_HEADER, tenantB.tenant().getTenantKey())
                        .cookie(tenantA.authCookie()))
                .andExpect(status().isForbidden());
    }
}
