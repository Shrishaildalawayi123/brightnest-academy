package com.shrishailacademy.integration;

import com.shrishailacademy.integration.support.TenantAdminTestHelper;
import com.shrishailacademy.integration.support.TenantAdminSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContactAdminTenantIsolationIntegrationTest {

    private static final String TENANT_HEADER = TenantAdminTestHelper.TENANT_HEADER;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TenantAdminTestHelper tenantAdminTestHelper;

    @Test
    void contactAdminEndpointsShouldReturnTenantScopedData() throws Exception {
        String adminPassword = "Admin@123";
        TenantAdminSession tenantA = tenantAdminTestHelper
                .createTenantAdminSession("contact-a-", "contact-admin-a-", adminPassword);
        TenantAdminSession tenantB = tenantAdminTestHelper
                .createTenantAdminSession("contact-b-", "contact-admin-b-", adminPassword);

        tenantAdminTestHelper.submitContact(
                tenantA.tenant().getTenantKey(),
                "contact-a-1@example.com",
                "Tenant A Contact",
                "A contact message");

        tenantAdminTestHelper.submitContact(
                tenantB.tenant().getTenantKey(),
                "contact-b-1@example.com",
                "Tenant B Contact #1",
                "B contact message one");
        tenantAdminTestHelper.submitContact(
                tenantB.tenant().getTenantKey(),
                "contact-b-2@example.com",
                "Tenant B Contact #2",
                "B contact message two");

        mockMvc.perform(get("/api/contact/stats")
                        .header(TENANT_HEADER, tenantA.tenant().getTenantKey())
                        .cookie(tenantA.authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.unread").value(1))
                .andExpect(jsonPath("$.data.read").value(0));

        mockMvc.perform(get("/api/contact/stats")
                        .header(TENANT_HEADER, tenantB.tenant().getTenantKey())
                        .cookie(tenantB.authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.unread").value(2))
                .andExpect(jsonPath("$.data.read").value(0));

    }

    @Test
    void contactAdminEndpointsShouldRejectCrossTenantTokenHeaderMismatch() throws Exception {
        String adminPassword = "Admin@123";
        TenantAdminSession tenantA = tenantAdminTestHelper
                .createTenantAdminSession("contact-mismatch-a-", "contact-mismatch-admin-a-", adminPassword);
        TenantAdminSession tenantB = tenantAdminTestHelper
                .createTenantAdminSession("contact-mismatch-b-", "contact-mismatch-admin-b-", adminPassword);

        tenantAdminTestHelper.submitContact(
                tenantB.tenant().getTenantKey(),
                "contact-mismatch-b@example.com",
                "Mismatch Contact",
                "Mismatch data");

        mockMvc.perform(get("/api/contact")
                        .header(TENANT_HEADER, tenantB.tenant().getTenantKey())
                        .cookie(tenantA.authCookie()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/contact/unread")
                        .header(TENANT_HEADER, tenantB.tenant().getTenantKey())
                        .cookie(tenantA.authCookie()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/contact/stats")
                        .header(TENANT_HEADER, tenantB.tenant().getTenantKey())
                        .cookie(tenantA.authCookie()))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/contact/1/status")
                        .with(csrf())
                        .param("status", "READ")
                        .header(TENANT_HEADER, tenantB.tenant().getTenantKey())
                        .cookie(tenantA.authCookie()))
                .andExpect(status().isForbidden());
    }
}
