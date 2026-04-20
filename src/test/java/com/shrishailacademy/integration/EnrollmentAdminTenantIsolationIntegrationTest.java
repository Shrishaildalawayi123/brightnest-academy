package com.shrishailacademy.integration;

import com.shrishailacademy.integration.support.TenantAdminSession;
import com.shrishailacademy.integration.support.TenantAdminTestHelper;
import com.shrishailacademy.model.Course;
import com.shrishailacademy.model.Enrollment;
import com.shrishailacademy.model.Tenant;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.CourseRepository;
import com.shrishailacademy.repository.EnrollmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EnrollmentAdminTenantIsolationIntegrationTest {

    private static final String TENANT_HEADER = TenantAdminTestHelper.TENANT_HEADER;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TenantAdminTestHelper tenantAdminTestHelper;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Test
    void enrollmentAdminEndpointsShouldReturnTenantScopedDataAndAllowTenantAdminCancel() throws Exception {
        String adminPassword = "Admin@123";
        TenantAdminSession tenantA = tenantAdminTestHelper
                .createTenantAdminSession("enroll-a-", "enroll-admin-a-", adminPassword);
        TenantAdminSession tenantB = tenantAdminTestHelper
                .createTenantAdminSession("enroll-b-", "enroll-admin-b-", adminPassword);

        User studentA = tenantAdminTestHelper.createUser(
                tenantA.tenant(),
                "Enrollment Student A",
                "enroll-student-a-" + UUID.randomUUID() + "@example.com",
                "Student@123",
                User.Role.STUDENT);
        User studentB = tenantAdminTestHelper.createUser(
                tenantB.tenant(),
                "Enrollment Student B",
                "enroll-student-b-" + UUID.randomUUID() + "@example.com",
                "Student@123",
                User.Role.STUDENT);

        Course courseA = createCourse(tenantA.tenant(), "Enrollment Course A", "enroll-course-a");
        Course courseB = createCourse(tenantB.tenant(), "Enrollment Course B", "enroll-course-b");

        Enrollment enrollmentA = createEnrollment(tenantA.tenant(), studentA, courseA, Enrollment.Status.ACTIVE);
        Enrollment enrollmentB = createEnrollment(tenantB.tenant(), studentB, courseB, Enrollment.Status.ACTIVE);

        mockMvc.perform(get("/api/enrollments")
                        .header(TENANT_HEADER, tenantA.tenant().getTenantKey())
                        .cookie(tenantA.authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem(enrollmentA.getId().intValue())))
                .andExpect(jsonPath("$.content[*].id", not(hasItem(enrollmentB.getId().intValue()))));

        mockMvc.perform(get("/api/enrollments")
                        .header(TENANT_HEADER, tenantB.tenant().getTenantKey())
                        .cookie(tenantB.authCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", hasItem(enrollmentB.getId().intValue())))
                .andExpect(jsonPath("$.content[*].id", not(hasItem(enrollmentA.getId().intValue()))));

        mockMvc.perform(delete("/api/enrollments/{id}", enrollmentA.getId())
                        .header(TENANT_HEADER, tenantA.tenant().getTenantKey())
                        .header("X-CSRF-Token", tenantA.csrfCookie().getValue())
                        .cookie(tenantA.authCookie(), tenantA.csrfCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Enrollment cancelled"));

        Enrollment cancelled = enrollmentRepository.findById(enrollmentA.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(Enrollment.Status.CANCELLED, cancelled.getStatus());
    }

    @Test
    void enrollmentAdminEndpointsShouldRejectCrossTenantTokenHeaderMismatch() throws Exception {
        String adminPassword = "Admin@123";
        TenantAdminSession tenantA = tenantAdminTestHelper
                .createTenantAdminSession("enroll-mismatch-a-", "enroll-mismatch-admin-a-", adminPassword);
        TenantAdminSession tenantB = tenantAdminTestHelper
                .createTenantAdminSession("enroll-mismatch-b-", "enroll-mismatch-admin-b-", adminPassword);

        User studentB = tenantAdminTestHelper.createUser(
                tenantB.tenant(),
                "Mismatch Student B",
                "enroll-mismatch-student-b-" + UUID.randomUUID() + "@example.com",
                "Student@123",
                User.Role.STUDENT);

        Course courseB = createCourse(tenantB.tenant(), "Mismatch Course B", "enroll-mismatch-course-b");
        Enrollment enrollmentB = createEnrollment(tenantB.tenant(), studentB, courseB, Enrollment.Status.ACTIVE);

        mockMvc.perform(get("/api/enrollments")
                        .header(TENANT_HEADER, tenantB.tenant().getTenantKey())
                        .cookie(tenantA.authCookie()))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/enrollments/{id}", enrollmentB.getId())
                        .header(TENANT_HEADER, tenantB.tenant().getTenantKey())
                        .header("X-CSRF-Token", tenantA.csrfCookie().getValue())
                        .cookie(tenantA.authCookie(), tenantA.csrfCookie()))
                .andExpect(status().isForbidden());
    }

    private Course createCourse(Tenant tenant, String titlePrefix, String subjectKeyPrefix) {
        Course course = new Course();
        course.setTenant(tenant);
        course.setTitle(titlePrefix + "-" + UUID.randomUUID().toString().substring(0, 8));
        course.setSubjectKey(subjectKeyPrefix + "-" + UUID.randomUUID().toString().substring(0, 8));
        course.setDescription("Tenant-scoped enrollment test course");
        course.setDuration("3 months");
        course.setFee(new BigDecimal("1500.00"));
        return courseRepository.save(course);
    }

    private Enrollment createEnrollment(Tenant tenant, User student, Course course, Enrollment.Status status) {
        Enrollment enrollment = new Enrollment();
        enrollment.setTenant(tenant);
        enrollment.setUser(student);
        enrollment.setCourse(course);
        enrollment.setStatus(status);
        return enrollmentRepository.save(enrollment);
    }
}
