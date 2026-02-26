package com.shrishailacademy.controller;

import com.shrishailacademy.dto.ApiResponse;
import com.shrishailacademy.model.Testimonial;
import com.shrishailacademy.repository.TestimonialRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/testimonials")
public class TestimonialController {

    @Autowired
    private TestimonialRepository testimonialRepo;

    /**
     * Public: Get all approved testimonials (shown on website)
     * GET /api/testimonials
     */
    @GetMapping
    public ResponseEntity<List<Testimonial>> getApprovedTestimonials() {
        return ResponseEntity.ok(testimonialRepo.findByApprovedTrueOrderByCreatedAtDesc());
    }

    /**
     * Admin: Get ALL testimonials (including unapproved)
     * GET /api/testimonials/all
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Testimonial>> getAllTestimonials() {
        return ResponseEntity.ok(testimonialRepo.findAllByOrderByCreatedAtDesc());
    }

    /**
     * Admin: Add a new testimonial
     * POST /api/testimonials
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addTestimonial(@Valid @RequestBody Testimonial testimonial) {
        try {
            Testimonial saved = testimonialRepo.save(testimonial);
            return ResponseEntity.ok(ApiResponse.success("Testimonial added", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Admin: Approve/unapprove a testimonial
     * PUT /api/testimonials/{id}/approve
     */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleApproval(@PathVariable Long id) {
        try {
            Testimonial t = testimonialRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Testimonial not found"));
            t.setApproved(!t.isApproved());
            testimonialRepo.save(t);
            return ResponseEntity.ok(ApiResponse.success(
                    t.isApproved() ? "Testimonial approved" : "Testimonial unapproved"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Admin: Delete a testimonial
     * DELETE /api/testimonials/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteTestimonial(@PathVariable Long id) {
        try {
            testimonialRepo.deleteById(id);
            return ResponseEntity.ok(ApiResponse.success("Testimonial deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
