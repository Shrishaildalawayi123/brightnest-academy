package com.shrishailacademy.controller;

import com.shrishailacademy.dto.TeacherApplicationRequest;
import com.shrishailacademy.model.TeacherApplication;
import com.shrishailacademy.repository.TeacherApplicationRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Teacher Application Controller
 * Public: submit educator recruitment application
 * Admin: view applications, update status
 */
@RestController
@RequestMapping("/api/teacher-applications")
public class TeacherApplicationController {

    @Autowired
    private TeacherApplicationRepository teacherApplicationRepository;

    /**
     * POST /api/teacher-applications - Public educator application form
     */
    @PostMapping
    public ResponseEntity<?> submitApplication(@Valid @RequestBody TeacherApplicationRequest request) {
        TeacherApplication application = new TeacherApplication();
        application.setFullName(request.getFullName());
        application.setEmail(request.getEmail());
        application.setPhone(request.getPhone());
        application.setSubjectExpertise(request.getSubjectExpertise());
        application.setExperience(request.getExperience());
        application.setMotivation(request.getMotivation());
        application.setStatus(TeacherApplication.Status.NEW);

        teacherApplicationRepository.save(application);

        return ResponseEntity.ok(Map.of(
                "message",
                "Thank you for your interest in joining BrightNest Academy! We've received your application and will get back to you within 5 business days."));
    }

    /**
     * GET /api/teacher-applications - Admin: list all applications
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllApplications(@RequestParam(required = false) String status) {
        if (status != null && !status.isEmpty()) {
            try {
                TeacherApplication.Status s = TeacherApplication.Status.valueOf(status.toUpperCase());
                return ResponseEntity.ok(teacherApplicationRepository.findByStatusOrderByCreatedAtDesc(s));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid status"));
            }
        }
        return ResponseEntity.ok(teacherApplicationRepository.findAllByOrderByCreatedAtDesc());
    }

    /**
     * PUT /api/teacher-applications/{id}/status - Admin: update application status
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return teacherApplicationRepository.findById(id)
                .map(app -> {
                    try {
                        TeacherApplication.Status newStatus = TeacherApplication.Status.valueOf(
                                body.getOrDefault("status", "NEW").toUpperCase());
                        app.setStatus(newStatus);
                        teacherApplicationRepository.save(app);
                        return ResponseEntity
                                .ok(Map.of("message", "Application status updated to " + newStatus));
                    } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Invalid status value"));
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/teacher-applications/stats - Admin: application statistics
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getStats() {
        return ResponseEntity.ok(Map.of(
                "total", teacherApplicationRepository.count(),
                "new", teacherApplicationRepository.countByStatus(TeacherApplication.Status.NEW),
                "reviewed", teacherApplicationRepository.countByStatus(TeacherApplication.Status.REVIEWED),
                "contacted", teacherApplicationRepository.countByStatus(TeacherApplication.Status.CONTACTED),
                "hired", teacherApplicationRepository.countByStatus(TeacherApplication.Status.HIRED)));
    }
}
