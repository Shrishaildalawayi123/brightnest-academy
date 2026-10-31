package com.shrishailacademy.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.shrishailacademy.integration.support.TenantAdminTestHelper;
import com.shrishailacademy.model.User;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AcademicOwnershipIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired TenantAdminTestHelper helper;

    @ParameterizedTest
    @ValueSource(
            strings = {
                "/api/assignment-submissions/student/",
                "/api/v1/submissions/student/",
                "/api/v1/session-attendance/student/"
            })
    void studentsCannotReadAPeerEvenWithinTheSameTenant(String route) throws Exception {
        var tenant = helper.createTenant("academic-");
        String suffix = UUID.randomUUID().toString();
        var own =
                helper.createUser(
                        tenant,
                        "Student",
                        "own-" + suffix + "@example.com",
                        "Student@123",
                        User.Role.STUDENT);
        var peer =
                helper.createUser(
                        tenant,
                        "Peer",
                        "peer-" + suffix + "@example.com",
                        "Student@123",
                        User.Role.STUDENT);
        var cookie =
                helper.loginAndGetAuthCookie(tenant.getTenantKey(), own.getEmail(), "Student@123");
        mvc.perform(get(route + own.getId()).cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        mvc.perform(get(route + peer.getId()).cookie(cookie)).andExpect(status().isForbidden());
        var other = helper.createTenant("other-academic-");
        mvc.perform(
                        get(route + own.getId())
                                .cookie(cookie)
                                .header("X-Tenant-ID", other.getTenantKey()))
                .andExpect(status().isForbidden());
        mvc.perform(get(route + own.getId())).andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedWritesStillRequireCsrfAndStudentsCannotGrade() throws Exception {
        var tenant = helper.createTenant("academic-write-");
        var student =
                helper.createUser(
                        tenant,
                        "Student",
                        "grade-" + UUID.randomUUID() + "@example.com",
                        "Student@123",
                        User.Role.STUDENT);
        var cookies =
                helper.loginAndGetAuthAndCsrfCookies(
                        tenant.getTenantKey(), student.getEmail(), "Student@123");
        mvc.perform(
                        post("/api/assignment-submissions/1/grade")
                                .cookie(cookies)
                                .contentType("application/json")
                                .content(
                                        "{\"assignmentId\":1,\"studentId\":1,\"submissionText\":\"My"
                                            + " solution\",\"score\":90}"))
                .andExpect(status().isForbidden());
        mvc.perform(
                        post("/api/assignment-submissions/1/grade")
                                .cookie(cookies)
                                .header("X-CSRF-Token", cookies[1].getValue())
                                .contentType("application/json")
                                .content(
                                        "{\"assignmentId\":1,\"studentId\":1,\"submissionText\":\"My"
                                            + " solution\",\"score\":90}"))
                .andExpect(status().isForbidden());
    }
}
