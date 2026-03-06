package com.shrishailacademy.service;

import com.shrishailacademy.dto.DemoBookingRequest;
import com.shrishailacademy.exception.BusinessException;
import com.shrishailacademy.exception.ResourceNotFoundException;
import com.shrishailacademy.model.DemoBooking;
import com.shrishailacademy.model.Tenant;
import com.shrishailacademy.repository.DemoBookingRepository;
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
class DemoBookingServiceTest {

    private static final Long TENANT_ID = 1L;

    @Mock
    private DemoBookingRepository demoBookingRepository;

    @Mock
    private TenantService tenantService;

    @InjectMocks
    private DemoBookingService demoBookingService;

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
    void submitBookingShouldSanitizeAndSetDefaults() {
        DemoBookingRequest request = new DemoBookingRequest();
        request.setStudentName("Jane Doe");
        request.setEmail("jane@example.com");
        request.setPhone("9876543210");
        request.setSubject("Mathematics");
        request.setClassMode("ONLINE");

        when(tenantService.requireCurrentTenant()).thenReturn(testTenant);
        when(demoBookingRepository.save(any(DemoBooking.class))).thenAnswer(inv -> {
            DemoBooking b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });

        DemoBooking saved = demoBookingService.submitBooking(request);

        assertEquals(DemoBooking.Status.PENDING, saved.getStatus());
        assertEquals(100, saved.getDemoFee());
        assertEquals(DemoBooking.ClassMode.ONLINE, saved.getClassMode());
        verify(demoBookingRepository).save(any(DemoBooking.class));
    }

    @Test
    void submitBookingShouldDefaultToOnlineForInvalidClassMode() {
        DemoBookingRequest request = new DemoBookingRequest();
        request.setStudentName("Student");
        request.setEmail("s@e.com");
        request.setPhone("1234567890");
        request.setSubject("Science");
        request.setClassMode("INVALID_MODE");

        when(tenantService.requireCurrentTenant()).thenReturn(testTenant);
        when(demoBookingRepository.save(any(DemoBooking.class))).thenAnswer(inv -> {
            DemoBooking b = inv.getArgument(0);
            b.setId(2L);
            return b;
        });

        DemoBooking saved = demoBookingService.submitBooking(request);

        assertEquals(DemoBooking.ClassMode.ONLINE, saved.getClassMode());
    }

    @Test
    void submitBookingShouldSanitizeXssInStudentName() {
        DemoBookingRequest request = new DemoBookingRequest();
        request.setStudentName("<script>alert('xss')</script> Student");
        request.setEmail("s@e.com");
        request.setPhone("1234567890");
        request.setSubject("Math");
        request.setClassMode("OFFLINE");

        when(tenantService.requireCurrentTenant()).thenReturn(testTenant);
        when(demoBookingRepository.save(any(DemoBooking.class))).thenAnswer(inv -> {
            DemoBooking b = inv.getArgument(0);
            b.setId(3L);
            return b;
        });

        DemoBooking saved = demoBookingService.submitBooking(request);

        assertFalse(saved.getStudentName().contains("<script>"));
    }

    @Test
    void getAllBookingsShouldReturnAllWhenNoStatusFilter() {
        when(demoBookingRepository.findByTenantIdOrderByCreatedAtDesc(TENANT_ID))
                .thenReturn(List.of(new DemoBooking(), new DemoBooking()));

        List<DemoBooking> result = demoBookingService.getAllBookings(null);

        assertEquals(2, result.size());
        verify(demoBookingRepository).findByTenantIdOrderByCreatedAtDesc(TENANT_ID);
    }

    @Test
    void getAllBookingsShouldFilterByValidStatus() {
        when(demoBookingRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(TENANT_ID, DemoBooking.Status.PENDING))
                .thenReturn(List.of(new DemoBooking()));

        List<DemoBooking> result = demoBookingService.getAllBookings("PENDING");

        assertEquals(1, result.size());
    }

    @Test
    void getAllBookingsShouldThrowOnInvalidStatus() {
        assertThrows(BusinessException.class,
                () -> demoBookingService.getAllBookings("INVALID"));
    }

    @Test
    void updateStatusShouldUpdateValidStatus() {
        DemoBooking booking = new DemoBooking();
        booking.setId(1L);
        booking.setStatus(DemoBooking.Status.PENDING);

        when(demoBookingRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(booking));
        when(demoBookingRepository.save(any(DemoBooking.class))).thenAnswer(inv -> inv.getArgument(0));

        DemoBooking updated = demoBookingService.updateStatus(1L, "SCHEDULED");

        assertEquals(DemoBooking.Status.SCHEDULED, updated.getStatus());
    }

    @Test
    void updateStatusShouldThrowOnInvalidStatus() {
        DemoBooking booking = new DemoBooking();
        booking.setId(1L);
        when(demoBookingRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(booking));

        assertThrows(BusinessException.class,
                () -> demoBookingService.updateStatus(1L, "BOGUS"));
    }

    @Test
    void updateStatusShouldThrowWhenNotFound() {
        when(demoBookingRepository.findByIdAndTenantId(99L, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> demoBookingService.updateStatus(99L, "COMPLETED"));
    }

    @Test
    void getStatsShouldReturnCorrectCounts() {
        when(demoBookingRepository.countByTenantId(TENANT_ID)).thenReturn(20L);
        when(demoBookingRepository.countByTenantIdAndStatus(TENANT_ID, DemoBooking.Status.PENDING)).thenReturn(10L);
        when(demoBookingRepository.countByTenantIdAndStatus(TENANT_ID, DemoBooking.Status.SCHEDULED)).thenReturn(5L);
        when(demoBookingRepository.countByTenantIdAndStatus(TENANT_ID, DemoBooking.Status.COMPLETED)).thenReturn(3L);
        when(demoBookingRepository.countByTenantIdAndStatus(TENANT_ID, DemoBooking.Status.CANCELLED)).thenReturn(2L);

        Map<String, Object> stats = demoBookingService.getStats();

        assertEquals(20L, stats.get("total"));
        assertEquals(10L, stats.get("pending"));
        assertEquals(5L, stats.get("scheduled"));
        assertEquals(3L, stats.get("completed"));
        assertEquals(2L, stats.get("cancelled"));
    }
}
