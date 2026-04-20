package com.shrishailacademy.integration;

import com.shrishailacademy.integration.support.TenantAdminTestHelper;
import com.shrishailacademy.integration.support.TenantAdminSession;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnalyticsTenantIsolationIntegrationTest {

    private static final String TENANT_HEADER = TenantAdminTestHelper.TENANT_HEADER;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TenantAdminTestHelper tenantAdminTestHelper;

    @Test
    void dashboardShouldReturnTenantScopedStatsForEachAdminTenant() throws Exception {
        String adminPassword = "Admin@123";
        TenantAdminSession tenantA = tenantAdminTestHelper
            .createTenantAdminSession("analytics-a-", "admin-a-", adminPassword);
        TenantAdminSession tenantB = tenantAdminTestHelper
            .createTenantAdminSession("analytics-b-", "admin-b-", adminPassword);

        tenantAdminTestHelper.submitContact(
            tenantA.tenant().getTenantKey(),
            "a1@example.com",
            "Analytics isolation A",
            "Tenant A contact data");
        tenantAdminTestHelper.submitContact(
            tenantB.tenant().getTenantKey(),
            "b1@example.com",
            "Analytics isolation B1",
            "Tenant B contact data #1");
        tenantAdminTestHelper.submitContact(
            tenantB.tenant().getTenantKey(),
            "b2@example.com",
            "Analytics isolation B2",
            "Tenant B contact data #2");

        Cookie adminACookie = tenantA.authCookie();
        Cookie adminBCookie = tenantB.authCookie();

        mockMvc.perform(get("/api/admin/analytics/dashboard")
                .header(TENANT_HEADER, tenantA.tenant().getTenantKey())
                        .cookie(adminACookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(1))
                .andExpect(jsonPath("$.totalContactMessages").value(1))
                .andExpect(jsonPath("$.unreadContactMessages").value(1));

        mockMvc.perform(get("/api/admin/analytics/dashboard")
                .header(TENANT_HEADER, tenantB.tenant().getTenantKey())
                        .cookie(adminBCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(1))
                .andExpect(jsonPath("$.totalContactMessages").value(2))
                .andExpect(jsonPath("$.unreadContactMessages").value(2));

        mockMvc.perform(get("/api/admin/analytics/dashboard")
                .header(TENANT_HEADER, tenantB.tenant().getTenantKey())
                        .cookie(adminACookie))
                .andExpect(status().isForbidden());
    }
}
