package com.shrishailacademy.controller;

import com.shrishailacademy.dto.ApiResponse;
import com.shrishailacademy.model.Testimonial;
import com.shrishailacademy.service.TestimonialService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/testimonials")
public class TestimonialController {

    private final TestimonialService testimonialService;

    public TestimonialController(TestimonialService testimonialService) {
        this.testimonialService = testimonialService;
    }

    @GetMapping
    public ResponseEntity<List<Testimonial>> getApprovedTestimonials() {
        return ResponseEntity.ok(testimonialService.getApprovedTestimonials());
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Testimonial>> getAllTestimonials() {
        return ResponseEntity.ok(testimonialService.getAllTestimonials());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> addTestimonial(@Valid @RequestBody Testimonial testimonial) {
        Testimonial saved = testimonialService.addTestimonial(testimonial);
        return ResponseEntity.ok(ApiResponse.success("Testimonial added", saved));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> toggleApproval(@PathVariable Long id) {
        Testimonial t = testimonialService.toggleApproval(id);
        return ResponseEntity.ok(ApiResponse.success(
                t.isApproved() ? "Testimonial approved" : "Testimonial unapproved"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteTestimonial(@PathVariable Long id) {
        testimonialService.deleteTestimonial(id);
        return ResponseEntity.ok(ApiResponse.success("Testimonial deleted"));
    }
}
