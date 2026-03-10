package com.shrishailacademy.controller;

import com.shrishailacademy.dto.ApiResponse;
import com.shrishailacademy.dto.DemoBookingRequest;
import com.shrishailacademy.dto.StatusUpdateRequest;
import com.shrishailacademy.model.DemoBooking;
import com.shrishailacademy.service.DemoBookingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Demo Booking Controller
 * Public: submit demo booking request
 * Admin: view all bookings, update status
 */
@RestController
@RequestMapping({"/api/demo-booking", "/api/v1/demo-booking"})
public class DemoBookingController {

    private final DemoBookingService demoBookingService;

    public DemoBookingController(DemoBookingService demoBookingService) {
        this.demoBookingService = demoBookingService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse> submitBooking(@Valid @RequestBody DemoBookingRequest request) {
        DemoBooking booking = demoBookingService.submitBooking(request);
        return ResponseEntity.ok(ApiResponse.success(
            "Demo class booked successfully! We'll contact you within 24 hours to schedule your session. Demo fee: INR 100 (adjustable in first month or refundable within 30 days).",
                booking.getId()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getAllBookings(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.success("Bookings retrieved", demoBookingService.getAllBookings(status)));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> updateStatus(@PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request) {
        String status = request.status();
        demoBookingService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Booking status updated to " + status.toUpperCase()));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getStats() {
        return ResponseEntity.ok(ApiResponse.success("Stats retrieved", demoBookingService.getStats()));
    }
}





