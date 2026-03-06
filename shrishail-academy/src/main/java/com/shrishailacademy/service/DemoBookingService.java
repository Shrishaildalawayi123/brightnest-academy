package com.shrishailacademy.service;

import com.shrishailacademy.dto.DemoBookingRequest;
import com.shrishailacademy.exception.BusinessException;
import com.shrishailacademy.exception.ResourceNotFoundException;
import com.shrishailacademy.model.DemoBooking;
import com.shrishailacademy.repository.DemoBookingRepository;
import com.shrishailacademy.tenant.TenantContext;
import com.shrishailacademy.util.InputSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DemoBookingService {

    private static final Logger log = LoggerFactory.getLogger(DemoBookingService.class);

    private final DemoBookingRepository demoBookingRepository;
    private final TenantService tenantService;

    public DemoBookingService(DemoBookingRepository demoBookingRepository, TenantService tenantService) {
        this.demoBookingRepository = demoBookingRepository;
        this.tenantService = tenantService;
    }

    @Transactional
    public DemoBooking submitBooking(DemoBookingRequest request) {
        DemoBooking booking = new DemoBooking();
        booking.setTenant(tenantService.requireCurrentTenant());
        booking.setStudentName(InputSanitizer.sanitizeAndTruncate(request.getStudentName(), 100));
        booking.setParentName(InputSanitizer.sanitizeAndTruncateNullable(request.getParentName(), 100));
        booking.setEmail(InputSanitizer.sanitizeEmailAndTruncate(request.getEmail(), 100));
        booking.setPhone(InputSanitizer.sanitizeAndTruncate(request.getPhone(), 20));
        booking.setSubject(InputSanitizer.sanitizeAndTruncate(request.getSubject(), 50));
        booking.setGrade(InputSanitizer.sanitizeAndTruncateNullable(request.getGrade(), 30));
        booking.setBoard(InputSanitizer.sanitizeAndTruncateNullable(request.getBoard(), 30));
        booking.setRequirements(InputSanitizer.sanitizeAndTruncateNullable(request.getRequirements(), 500));
        booking.setMessage(InputSanitizer.sanitizeAndTruncateNullable(request.getMessage(), 1000));
        booking.setDemoFee(100);
        booking.setStatus(DemoBooking.Status.PENDING);

        try {
            booking.setClassMode(DemoBooking.ClassMode
                    .valueOf(InputSanitizer.sanitizeAndTruncate(request.getClassMode(), 20).toUpperCase()));
        } catch (Exception e) {
            booking.setClassMode(DemoBooking.ClassMode.ONLINE);
        }

        DemoBooking saved = demoBookingRepository.save(booking);
        log.info("DEMO_BOOKING_CREATED: id={}, student='{}', email='{}'",
                saved.getId(), saved.getStudentName(), saved.getEmail());
        return saved;
    }

    public List<DemoBooking> getAllBookings(String status) {
        Long tenantId = TenantContext.requireTenantId();
        if (status != null && !status.isEmpty()) {
            try {
                DemoBooking.Status s = DemoBooking.Status.valueOf(status.toUpperCase());
                return demoBookingRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(tenantId, s);
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid status", "INVALID_STATUS");
            }
        }
        return demoBookingRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional
    public DemoBooking updateStatus(Long id, String status) {
        Long tenantId = TenantContext.requireTenantId();
        DemoBooking booking = demoBookingRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("DemoBooking", "id", id));

        try {
            DemoBooking.Status newStatus = DemoBooking.Status.valueOf(status.toUpperCase());
            booking.setStatus(newStatus);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid status value", "INVALID_STATUS");
        }

        DemoBooking saved = demoBookingRepository.save(booking);
        log.info("DEMO_BOOKING_STATUS_UPDATED: id={}, status={}", id, status);
        return saved;
    }

    public Map<String, Object> getStats() {
        Long tenantId = TenantContext.requireTenantId();
        return Map.of(
                "total", demoBookingRepository.countByTenantId(tenantId),
                "pending", demoBookingRepository.countByTenantIdAndStatus(tenantId, DemoBooking.Status.PENDING),
                "scheduled", demoBookingRepository.countByTenantIdAndStatus(tenantId, DemoBooking.Status.SCHEDULED),
                "completed", demoBookingRepository.countByTenantIdAndStatus(tenantId, DemoBooking.Status.COMPLETED),
                "cancelled", demoBookingRepository.countByTenantIdAndStatus(tenantId, DemoBooking.Status.CANCELLED));
    }
}
