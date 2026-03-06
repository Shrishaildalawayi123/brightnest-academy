package com.shrishailacademy.service;

import com.shrishailacademy.dto.CounselingRequestDTO;
import com.shrishailacademy.exception.ResourceNotFoundException;
import com.shrishailacademy.model.CounselingRequest;
import com.shrishailacademy.model.Tenant;
import com.shrishailacademy.repository.CounselingRequestRepository;
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
class CounselingServiceTest {

    private static final Long TENANT_ID = 1L;

    @Mock
    private CounselingRequestRepository counselingRepo;

    @Mock
    private TenantService tenantService;

    @InjectMocks
    private CounselingService counselingService;

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
    void submitRequestShouldSanitizeAndSave() {
        CounselingRequestDTO dto = new CounselingRequestDTO();
        dto.setStudentName("<script>alert(1)</script> Student");
        dto.setStudentClass("10th");
        dto.setBoard("CBSE");
        dto.setParentPhone("9876543210");

        when(tenantService.requireCurrentTenant()).thenReturn(testTenant);
        when(counselingRepo.save(any(CounselingRequest.class))).thenAnswer(inv -> {
            CounselingRequest r = inv.getArgument(0);
            r.setId(1L);
            return r;
        });

        CounselingRequest saved = counselingService.submitRequest(dto);

        assertFalse(saved.getStudentName().contains("<script>"));
        assertEquals(CounselingRequest.Status.NEW, saved.getStatus());
        verify(counselingRepo).save(any(CounselingRequest.class));
    }

    @Test
    void getAllRequestsShouldReturnOrderedList() {
        when(counselingRepo.findByTenantIdOrderByCreatedAtDesc(TENANT_ID))
                .thenReturn(List.of(new CounselingRequest(), new CounselingRequest()));

        List<CounselingRequest> result = counselingService.getAllRequests();

        assertEquals(2, result.size());
    }

    @Test
    void getNewRequestsShouldFilterByNewStatus() {
        when(counselingRepo.findByTenantIdAndStatusOrderByCreatedAtDesc(TENANT_ID, CounselingRequest.Status.NEW))
                .thenReturn(List.of(new CounselingRequest()));

        List<CounselingRequest> result = counselingService.getNewRequests();

        assertEquals(1, result.size());
    }

    @Test
    void updateStatusShouldUpdateValidStatus() {
        CounselingRequest req = new CounselingRequest();
        req.setId(1L);
        req.setStatus(CounselingRequest.Status.NEW);

        when(counselingRepo.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(req));
        when(counselingRepo.save(any(CounselingRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        CounselingRequest updated = counselingService.updateStatus(1L, "CONTACTED");

        assertEquals(CounselingRequest.Status.CONTACTED, updated.getStatus());
    }

    @Test
    void updateStatusShouldThrowOnInvalidStatus() {
        CounselingRequest req = new CounselingRequest();
        req.setId(1L);
        when(counselingRepo.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(req));

        assertThrows(IllegalArgumentException.class,
                () -> counselingService.updateStatus(1L, "BOGUS"));
    }

    @Test
    void updateStatusShouldThrowWhenNotFound() {
        when(counselingRepo.findByIdAndTenantId(99L, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> counselingService.updateStatus(99L, "CONTACTED"));
    }

    @Test
    void getStatsShouldReturnCorrectCounts() {
        when(counselingRepo.countByTenantId(TENANT_ID)).thenReturn(15L);
        when(counselingRepo.countByTenantIdAndStatus(TENANT_ID, CounselingRequest.Status.NEW)).thenReturn(8L);
        when(counselingRepo.countByTenantIdAndStatus(TENANT_ID, CounselingRequest.Status.CONTACTED)).thenReturn(4L);
        when(counselingRepo.countByTenantIdAndStatus(TENANT_ID, CounselingRequest.Status.COMPLETED)).thenReturn(3L);

        Map<String, Object> stats = counselingService.getStats();

        assertEquals(15L, stats.get("total"));
        assertEquals(8L, stats.get("new"));
        assertEquals(4L, stats.get("contacted"));
        assertEquals(3L, stats.get("completed"));
    }
}
