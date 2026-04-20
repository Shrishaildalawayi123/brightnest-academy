package com.shrishailacademy.integration;

import com.shrishailacademy.integration.support.TenantAdminSession;
import com.shrishailacademy.integration.support.TenantAdminTestHelper;
import com.shrishailacademy.model.Course;
import com.shrishailacademy.model.Payment;
import com.shrishailacademy.model.Tenant;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.CourseRepository;
import com.shrishailacademy.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentAdminTenantIsolationIntegrationTest {

    private static final String TENANT_HEADER = TenantAdminTestHelper.TENANT_HEADER;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TenantAdminTestHelper tenantAdminTestHelper;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void paymentAdminEndpointsShouldReturnTenantScopedData() throws Exception {
        String adminPassword = "Admin@123";
        TenantAdminSession tenantA = tenantAdminTestHelper
                .createTenantAdminSession("pay-a-", "pay-admin-a-", adminPassword);
        TenantAdminSession tenantB = tenantAdminTestHelper
                .createTenantAdminSession("pay-b-", "pay-admin-b-", adminPassword);

        User studentA = tenantAdminTestHelper.createUser(
                tenantA.tenant(),
                "Payment Student A",
                "pay-student-a-" + UUID.randomUUID() + "@example.com",
                "Student@123",
                User.Role.STUDENT);
        User studentB = tenantAdminTestHelper.createUser(
                tenantB.tenant(),
                "Payment Student B",
                "pay-student-b-" + UUID.randomUUID() + "@example.com",
                "Student@123",
                User.Role.STUDENT);

        Course courseA = createCourse(tenantA.tenant(), "Payment Course A", "pay-course-a");
        Course courseB = createCourse(tenantB.tenant(), "Payment Course B", "pay-course-b");

        Payment paymentA = createPayment(
                tenantA.tenant(),
                studentA,
                courseA,
                new BigDecimal("1000.00"),
                Payment.Status.SUCCESS,
                "PAY-A-TXN-" + UUID.randomUUID());

        Payment paymentB = createPayment(
                tenantB.tenant(),
                studentB,
                courseB,
                new BigDecimal("2000.00"),
                Payment.Status.SUCCESS,
                "PAY-B-TXN-" + UUID.randomUUID());

        mockMvc.perform(get("/api/payments")
                        .header(TENANT_HEADER, tenantA.tenant().getTenantKey())
                        .cookie(tenantA.authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem(paymentA.getId().intValue())))
                .andExpect(jsonPath("$.content[*].id", not(hasItem(paymentB.getId().intValue()))));

        mockMvc.perform(get("/api/payments")
                        .header(TENANT_HEADER, tenantB.tenant().getTenantKey())
                        .cookie(tenantB.authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem(paymentB.getId().intValue())))
                .andExpect(jsonPath("$.content[*].id", not(hasItem(paymentA.getId().intValue()))));

        mockMvc.perform(get("/api/payments/stats")
                        .header(TENANT_HEADER, tenantA.tenant().getTenantKey())
                        .cookie(tenantA.authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalRevenue").value(1000.0))
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.pendingCount").value(0))
                .andExpect(jsonPath("$.data.failedCount").value(0));

        mockMvc.perform(get("/api/payments/stats")
                        .header(TENANT_HEADER, tenantB.tenant().getTenantKey())
                        .cookie(tenantB.authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalRevenue").value(2000.0))
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.pendingCount").value(0))
                .andExpect(jsonPath("$.data.failedCount").value(0));
    }

    @Test
    void paymentAdminEndpointsShouldRejectCrossTenantTokenHeaderMismatch() throws Exception {
        String adminPassword = "Admin@123";
        TenantAdminSession tenantA = tenantAdminTestHelper
                .createTenantAdminSession("pay-mismatch-a-", "pay-mismatch-admin-a-", adminPassword);
        TenantAdminSession tenantB = tenantAdminTestHelper
                .createTenantAdminSession("pay-mismatch-b-", "pay-mismatch-admin-b-", adminPassword);

        mockMvc.perform(get("/api/payments")
                        .header(TENANT_HEADER, tenantB.tenant().getTenantKey())
                        .cookie(tenantA.authCookie()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/payments/stats")
                        .header(TENANT_HEADER, tenantB.tenant().getTenantKey())
                        .cookie(tenantA.authCookie()))
                .andExpect(status().isForbidden());
    }

        private Course createCourse(Tenant tenant, String title, String subjectKey) {
        Course course = new Course();
                course.setTenant(tenant);
                course.setTitle(title + "-" + UUID.randomUUID().toString().substring(0, 8));
                course.setSubjectKey(subjectKey + "-" + UUID.randomUUID().toString().substring(0, 8));
                course.setDescription("Tenant-scoped payment test course");
                course.setDuration("3 months");
                course.setFee(new BigDecimal("1000.00"));
                return courseRepository.save(course);
    }

    private Payment createPayment(
                        Tenant tenant,
                        User user,
                        Course course,
            BigDecimal amount,
            Payment.Status status,
            String transactionId) {
        Payment payment = new Payment();
                payment.setTenant(tenant);
                payment.setUser(user);
                payment.setCourse(course);
        payment.setAmount(amount);
        payment.setStatus(status);
        payment.setPaymentMethod(Payment.PaymentMethod.UPI);
        payment.setReceiptNumber("RCPT-" + UUID.randomUUID());
        payment.setTransactionId(transactionId);
                payment.setPaidAt(status == Payment.Status.SUCCESS ? LocalDateTime.now() : null);
        return paymentRepository.save(payment);
    }
}
