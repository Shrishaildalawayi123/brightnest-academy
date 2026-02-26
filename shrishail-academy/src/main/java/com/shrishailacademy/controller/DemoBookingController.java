package com.shrishailacademy.controller;

import com.shrishailacademy.dto.DemoBookingRequest;
import com.shrishailacademy.model.DemoBooking;
import com.shrishailacademy.repository.DemoBookingRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Demo Booking Controller
 * Public: submit demo booking request
 * Admin: view all bookings, update status
 */
@RestController
@RequestMapping("/api/demo-booking")
public class DemoBookingController {

    @Autowired
    private DemoBookingRepository demoBookingRepository;

    /**
     * POST /api/demo-booking - Public demo class booking form
     */
    @PostMapping
    public ResponseEntity<?> submitBooking(@Valid @RequestBody DemoBookingRequest request) {
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

        // Parse class mode
        try {
            booking.setClassMode(DemoBooking.ClassMode.valueOf(request.getClassMode().toUpperCase()));
        } catch (Exception e) {
            booking.setClassMode(DemoBooking.ClassMode.ONLINE);
        }

        demoBookingRepository.save(booking);

        return ResponseEntity.ok(Map.of(
                "message",
                "Demo class booked successfully! We'll contact you within 24 hours to schedule your session. Demo fee: ₹100 (adjustable in first month or refundable within 30 days).",
                "bookingId", booking.getId()));
    }

    /**
     * GET /api/demo-booking - Admin: list all demo bookings
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllBookings(@RequestParam(required = false) String status) {
        if (status != null && !status.isEmpty()) {
            try {
                DemoBooking.Status s = DemoBooking.Status.valueOf(status.toUpperCase());
                return ResponseEntity.ok(demoBookingRepository.findByStatusOrderByCreatedAtDesc(s));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid status"));
            }
        }
        return ResponseEntity.ok(demoBookingRepository.findAllByOrderByCreatedAtDesc());
    }

    /**
     * PUT /api/demo-booking/{id}/status - Admin: update booking status
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return demoBookingRepository.findById(id)
                .map(booking -> {
                    try {
                        DemoBooking.Status newStatus = DemoBooking.Status.valueOf(
                                body.getOrDefault("status", "PENDING").toUpperCase());
                        booking.setStatus(newStatus);
                        demoBookingRepository.save(booking);
                        return ResponseEntity.ok(Map.of("message", "Booking status updated to " + newStatus));
                    } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Invalid status value"));
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/demo-booking/stats - Admin: booking statistics
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getStats() {
        return ResponseEntity.ok(Map.of(
                "total", demoBookingRepository.count(),
                "pending", demoBookingRepository.countByStatus(DemoBooking.Status.PENDING),
                "scheduled", demoBookingRepository.countByStatus(DemoBooking.Status.SCHEDULED),
                "completed", demoBookingRepository.countByStatus(DemoBooking.Status.COMPLETED),
                "cancelled", demoBookingRepository.countByStatus(DemoBooking.Status.CANCELLED)));
    }
}
