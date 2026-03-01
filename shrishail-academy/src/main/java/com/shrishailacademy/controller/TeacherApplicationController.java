package com.shrishailacademy.controller;

import com.shrishailacademy.dto.TeacherApplicationRequest;
import com.shrishailacademy.service.ResumeStorageService;
import com.shrishailacademy.service.TeacherApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Teacher Application Controller
 * Public: submit educator recruitment application (with optional resume upload)
 * Admin: view applications, update status
 */
@RestController
@RequestMapping("/api/teacher-applications")
public class TeacherApplicationController {

    private final TeacherApplicationService teacherApplicationService;
    private final ResumeStorageService resumeStorageService;

    public TeacherApplicationController(TeacherApplicationService teacherApplicationService,
            ResumeStorageService resumeStorageService) {
        this.teacherApplicationService = teacherApplicationService;
        this.resumeStorageService = resumeStorageService;
    }

    /**
     * Submit via JSON (no resume) — backward compatible with team.html form.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> submitApplication(
            @Valid @RequestBody TeacherApplicationRequest request) {
        teacherApplicationService.submitApplication(request);
        return ResponseEntity.ok(Map.of("success", "true",
                "message",
                "Thank you for your interest in joining BrightNest Academy! We've received your application and will get back to you within 5 business days."));
    }

    /**
     * Submit via multipart form (with optional resume file) — careers.html form.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> submitApplicationWithResume(
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String subjectExpertise,
            @RequestParam(required = false) String qualification,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String teachingMode,
            @RequestParam(required = false) String experience,
            @RequestParam(required = false) String motivation,
            @RequestParam(required = false) MultipartFile resume) {

        TeacherApplicationRequest request = new TeacherApplicationRequest();
        request.setFullName(fullName);
        request.setEmail(email);
        request.setPhone(phone);
        request.setSubjectExpertise(subjectExpertise);
        request.setQualification(qualification);
        request.setCity(city);
        request.setTeachingMode(teachingMode);
        request.setExperience(experience);
        request.setMotivation(motivation);

        String resumeFileName = null;
        String resumeFilePath = null;
        if (resume != null && !resume.isEmpty()) {
            String[] result = resumeStorageService.storeResume(resume);
            if (result != null) {
                resumeFileName = result[0];
                resumeFilePath = result[1];
            }
        }

        teacherApplicationService.submitApplication(request, resumeFileName, resumeFilePath);
        return ResponseEntity.ok(Map.of("success", "true",
                "message",
                "Thank you for applying! We've received your application and resume. Our team will review it and contact you within 5 business days."));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllApplications(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(teacherApplicationService.getAllApplications(status));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> updateStatus(@PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status = body.getOrDefault("status", "NEW");
        teacherApplicationService.updateStatus(id, status);
        return ResponseEntity.ok(Map.of("message", "Application status updated to " + status.toUpperCase()));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(teacherApplicationService.getStats());
    }
}
