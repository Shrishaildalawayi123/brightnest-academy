package com.shrishailacademy.controller;

import com.shrishailacademy.dto.ApiResponse;
import com.shrishailacademy.dto.CounselingRequestDTO;
import com.shrishailacademy.service.CounselingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/counseling", "/api/v1/counseling"})
public class CounselingController {

    private final CounselingService counselingService;

    public CounselingController(CounselingService counselingService) {
        this.counselingService = counselingService;
    }

    /**
     * Public endpoint - anyone can submit a counseling callback request.
     */
    @PostMapping
    public ResponseEntity<ApiResponse> submitCounselingRequest(
            @Valid @RequestBody CounselingRequestDTO request) {
        counselingService.submitRequest(request);
        return ResponseEntity.ok(ApiResponse.success(
                "Thank you! Our academic advisor will call you within 24 hours."));
    }

    /**
     * Admin: view all counseling requests.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getAllRequests() {
        return ResponseEntity.ok(ApiResponse.success("Requests retrieved", counselingService.getAllRequests()));
    }

    /**
     * Admin: view only new/pending requests.
     */
    @GetMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getNewRequests() {
        return ResponseEntity.ok(ApiResponse.success("New requests retrieved", counselingService.getNewRequests()));
    }

    /**
     * Admin: update status of a counseling request.
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> updateStatus(@PathVariable Long id, @RequestParam String status) {
        counselingService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Counseling request status updated"));
    }

    /**
     * Admin: get counseling request stats.
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getStats() {
        return ResponseEntity.ok(ApiResponse.success("Stats retrieved", counselingService.getStats()));
    }
}



