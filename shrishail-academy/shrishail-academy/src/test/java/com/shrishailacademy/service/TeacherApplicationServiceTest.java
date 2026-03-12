package com.shrishailacademy.service;

import com.shrishailacademy.dto.TeacherApplicationRequest;
import com.shrishailacademy.exception.BusinessException;
import com.shrishailacademy.exception.ResourceNotFoundException;
import com.shrishailacademy.model.TeacherApplication;
import com.shrishailacademy.model.Tenant;
import com.shrishailacademy.repository.TeacherApplicationRepository;
import com.shrishailacademy.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherApplicationServiceTest {

    private static final Long TENANT_ID = 1L;

    @Mock
    private TeacherApplicationRepository teacherApplicationRepository;

    @Mock
    private TenantService tenantService;

    @InjectMocks
    private TeacherApplicationService teacherApplicationService;

    private Tenant testTenant;

    @BeforeEach
    void setUp() {
        TenantContext.set(TENANT_ID, "test");
        testTenant = new Tenant();
        testTenant.setId(TENANT_ID);
        testTenant.setTenantKey("test");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void submitApplicationShouldSanitizeAndSaveWithoutResume() {
        TeacherApplicationRequest request = new TeacherApplicationRequest();
        request.setFullName("Jane Teacher");
        request.setEmail("jane@example.com");
        request.setPhone("9876543210");
        request.setSubjectExpertise("Mathematics");

        when(tenantService.requireCurrentTenant()).thenReturn(testTenant);
        when(teacherApplicationRepository.save(any(TeacherApplication.class))).thenAnswer(inv -> {
            TeacherApplication a = inv.getArgument(0);
            a.setId(1L);
            return a;
        });

        TeacherApplication saved = teacherApplicationService.submitApplication(request);

        assertEquals(TeacherApplication.Status.NEW, saved.getStatus());
        assertNull(saved.getResumeFileName());
        assertNull(saved.getResumeFilePath());
        verify(teacherApplicationRepository).save(any(TeacherApplication.class));
    }

    @Test
    void submitApplicationShouldSaveWithResume() {
        TeacherApplicationRequest request = new TeacherApplicationRequest();
        request.setFullName("John Teacher");
        request.setEmail("john@example.com");
        request.setPhone("1234567890");
        request.setSubjectExpertise("Physics");

        when(tenantService.requireCurrentTenant()).thenReturn(testTenant);
        when(teacherApplicationRepository.save(any(TeacherApplication.class))).thenAnswer(inv -> {
            TeacherApplication a = inv.getArgument(0);
            a.setId(2L);
            return a;
        });

        TeacherApplication saved = teacherApplicationService.submitApplication(
                request, "resume.pdf", "/uploads/resumes/resume.pdf");

        assertEquals("resume.pdf", saved.getResumeFileName());
        assertEquals("/uploads/resumes/resume.pdf", saved.getResumeFilePath());
    }

    @Test
    void submitApplicationShouldSanitizeXss() {
        TeacherApplicationRequest request = new TeacherApplicationRequest();
        request.setFullName("<script>alert(1)</script> Evil");
        request.setEmail("evil@example.com");
        request.setPhone("1234567890");
        request.setSubjectExpertise("Hacking");

        when(tenantService.requireCurrentTenant()).thenReturn(testTenant);
        when(teacherApplicationRepository.save(any(TeacherApplication.class))).thenAnswer(inv -> {
            TeacherApplication a = inv.getArgument(0);
            a.setId(3L);
            return a;
        });

        TeacherApplication saved = teacherApplicationService.submitApplication(request);

        assertFalse(saved.getFullName().contains("<script>"));
    }

    @Test
    void getAllApplicationsShouldReturnAllWhenNoFilter() {
        when(teacherApplicationRepository.findByTenantIdOrderByCreatedAtDesc(TENANT_ID))
                .thenReturn(List.of(new TeacherApplication(), new TeacherApplication()));

        List<TeacherApplication> result = teacherApplicationService.getAllApplications(null);

        assertEquals(2, result.size());
    }

    @Test
    void getAllApplicationsShouldFilterByValidStatus() {
        when(teacherApplicationRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(TENANT_ID, TeacherApplication.Status.NEW))
                .thenReturn(List.of(new TeacherApplication()));

        List<TeacherApplication> result = teacherApplicationService.getAllApplications("NEW");

        assertEquals(1, result.size());
    }

    @Test
    void getAllApplicationsShouldThrowOnInvalidStatus() {
        assertThrows(BusinessException.class,
                () -> teacherApplicationService.getAllApplications("INVALID"));
    }

    @Test
    void updateStatusShouldUpdateValidStatus() {
        TeacherApplication app = new TeacherApplication();
        app.setId(1L);
        app.setStatus(TeacherApplication.Status.NEW);

        when(teacherApplicationRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(app));
        when(teacherApplicationRepository.save(any(TeacherApplication.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        TeacherApplication updated = teacherApplicationService.updateStatus(1L, "REVIEWED");

        assertEquals(TeacherApplication.Status.REVIEWED, updated.getStatus());
    }

    @Test
    void updateStatusShouldThrowOnInvalidStatus() {
        TeacherApplication app = new TeacherApplication();
        app.setId(1L);
        when(teacherApplicationRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(app));

        assertThrows(BusinessException.class,
                () -> teacherApplicationService.updateStatus(1L, "INVALID"));
    }

    @Test
    void updateStatusShouldThrowWhenNotFound() {
        when(teacherApplicationRepository.findByIdAndTenantId(99L, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> teacherApplicationService.updateStatus(99L, "REVIEWED"));
    }

    @Test
    void getStatsShouldReturnCorrectCounts() {
        when(teacherApplicationRepository.countByTenantId(TENANT_ID)).thenReturn(25L);
        when(teacherApplicationRepository.countByTenantIdAndStatus(TENANT_ID, TeacherApplication.Status.NEW)).thenReturn(10L);
        when(teacherApplicationRepository.countByTenantIdAndStatus(TENANT_ID, TeacherApplication.Status.REVIEWED)).thenReturn(7L);
        when(teacherApplicationRepository.countByTenantIdAndStatus(TENANT_ID, TeacherApplication.Status.CONTACTED)).thenReturn(5L);
        when(teacherApplicationRepository.countByTenantIdAndStatus(TENANT_ID, TeacherApplication.Status.HIRED)).thenReturn(3L);

        Map<String, Object> stats = teacherApplicationService.getStats();

        assertEquals(25L, stats.get("total"));
        assertEquals(10L, stats.get("new"));
        assertEquals(7L, stats.get("reviewed"));
        assertEquals(5L, stats.get("contacted"));
        assertEquals(3L, stats.get("hired"));
    }
}
