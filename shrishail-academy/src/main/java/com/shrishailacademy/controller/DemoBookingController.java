package com.shrishailacademy.controller;

import com.shrishailacademy.dto.DemoBookingRequest;
import com.shrishailacademy.model.DemoBooking;
import com.shrishailacademy.service.DemoBookingService;
import jakarta.validation.Valid;
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

    private final DemoBookingService demoBookingService;

    public DemoBookingController(DemoBookingService demoBookingService) {
        this.demoBookingService = demoBookingService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> submitBooking(@Valid @RequestBody DemoBookingRequest request) {
        DemoBooking booking = demoBookingService.submitBooking(request);
        return ResponseEntity.ok(Map.of(
                "message",
                "Demo class booked successfully! We'll contact you within 24 hours to schedule your session. Demo fee: ₹100 (adjustable in first month or refundable within 30 days).",
                "bookingId", booking.getId()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllBookings(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(demoBookingService.getAllBookings(status));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> updateStatus(@PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status = body.getOrDefault("status", "PENDING");
        demoBookingService.updateStatus(id, status);
        return ResponseEntity.ok(Map.of("message", "Booking status updated to " + status.toUpperCase()));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(demoBookingService.getStats());
    }
}
