package com.shrishailacademy.service;

import com.shrishailacademy.dto.DemoBookingRequest;
import com.shrishailacademy.exception.BusinessException;
import com.shrishailacademy.exception.ResourceNotFoundException;
import com.shrishailacademy.model.DemoBooking;
import com.shrishailacademy.repository.DemoBookingRepository;
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

    public DemoBookingService(DemoBookingRepository demoBookingRepository) {
        this.demoBookingRepository = demoBookingRepository;
    }

    @Transactional
    public DemoBooking submitBooking(DemoBookingRequest request) {
        DemoBooking booking = new DemoBooking();
        booking.setStudentName(request.getStudentName());
        booking.setParentName(request.getParentName());
        booking.setEmail(request.getEmail());
        booking.setPhone(request.getPhone());
        booking.setSubject(request.getSubject());
        booking.setGrade(request.getGrade());
        booking.setBoard(request.getBoard());
        booking.setRequirements(request.getRequirements());
        booking.setMessage(request.getMessage());
        booking.setDemoFee(100);
        booking.setStatus(DemoBooking.Status.PENDING);

        try {
            booking.setClassMode(DemoBooking.ClassMode.valueOf(request.getClassMode().toUpperCase()));
        } catch (Exception e) {
            booking.setClassMode(DemoBooking.ClassMode.ONLINE);
        }

        DemoBooking saved = demoBookingRepository.save(booking);
        log.info("DEMO_BOOKING_CREATED: id={}, student='{}', email='{}'",
                saved.getId(), request.getStudentName(), request.getEmail());
        return saved;
    }

    public List<DemoBooking> getAllBookings(String status) {
        if (status != null && !status.isEmpty()) {
            try {
                DemoBooking.Status s = DemoBooking.Status.valueOf(status.toUpperCase());
                return demoBookingRepository.findByStatusOrderByCreatedAtDesc(s);
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid status", "INVALID_STATUS");
            }
        }
        return demoBookingRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public DemoBooking updateStatus(Long id, String status) {
        DemoBooking booking = demoBookingRepository.findById(id)
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
        return Map.of(
                "total", demoBookingRepository.count(),
                "pending", demoBookingRepository.countByStatus(DemoBooking.Status.PENDING),
                "scheduled", demoBookingRepository.countByStatus(DemoBooking.Status.SCHEDULED),
                "completed", demoBookingRepository.countByStatus(DemoBooking.Status.COMPLETED),
                "cancelled", demoBookingRepository.countByStatus(DemoBooking.Status.CANCELLED));
    }
}
