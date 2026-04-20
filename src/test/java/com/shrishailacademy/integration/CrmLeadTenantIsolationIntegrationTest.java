package com.shrishailacademy.integration;

import com.shrishailacademy.integration.support.TenantAdminTestHelper;
import com.shrishailacademy.integration.support.TenantAdminSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CrmLeadTenantIsolationIntegrationTest {

    private static final String TENANT_HEADER = TenantAdminTestHelper.TENANT_HEADER;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TenantAdminTestHelper tenantAdminTestHelper;

    @Test
    void crmLeadEndpointsShouldReturnTenantScopedDataForAdmin() throws Exception {
        String adminPassword = "Admin@123";
        TenantAdminSession tenantA = tenantAdminTestHelper
                .createTenantAdminSession("crm-a-", "crm-admin-a-", adminPassword);
        TenantAdminSession tenantB = tenantAdminTestHelper
                .createTenantAdminSession("crm-b-", "crm-admin-b-", adminPassword);

        tenantAdminTestHelper.submitContact(
                tenantA.tenant().getTenantKey(),
                "crm-a-1@example.com",
                "CRM Tenant A",
                "Lead A only");

        tenantAdminTestHelper.submitContact(
                tenantB.tenant().getTenantKey(),
                "crm-b-1@example.com",
                "CRM Tenant B #1",
                "Lead B one");
        tenantAdminTestHelper.submitContact(
                tenantB.tenant().getTenantKey(),
                "crm-b-2@example.com",
                "CRM Tenant B #2",
                "Lead B two");

        mockMvc.perform(get("/api/admin/leads")
                        .header(TENANT_HEADER, tenantA.tenant().getTenantKey())
                        .cookie(tenantA.authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].source").value("contact"))
                .andExpect(jsonPath("$.data[0].email").value("crm-a-1@example.com"));

        mockMvc.perform(get("/api/admin/leads")
                        .header(TENANT_HEADER, tenantB.tenant().getTenantKey())
                        .cookie(tenantB.authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].email", containsString("crm-b-")));

        mockMvc.perform(get("/api/admin/leads/stats")
                        .header(TENANT_HEADER, tenantA.tenant().getTenantKey())
                        .cookie(tenantA.authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalLeads").value(1))
                .andExpect(jsonPath("$.data.bySource.contact").value(1));

        mockMvc.perform(get("/api/admin/leads/stats")
                        .header(TENANT_HEADER, tenantB.tenant().getTenantKey())
                        .cookie(tenantB.authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalLeads").value(2))
                .andExpect(jsonPath("$.data.bySource.contact").value(2));

        mockMvc.perform(get("/api/admin/leads/stats")
                        .header(TENANT_HEADER, tenantB.tenant().getTenantKey())
                        .cookie(tenantA.authCookie()))
                .andExpect(status().isForbidden());
    }
}
