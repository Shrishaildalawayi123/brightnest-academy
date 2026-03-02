package com.shrishailacademy.controller;

import com.shrishailacademy.dto.TeacherApplicationRequest;
import com.shrishailacademy.dto.StatusUpdateRequest;
import com.shrishailacademy.service.ResumeStorageService;
import com.shrishailacademy.service.TeacherApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
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
            @Valid @ModelAttribute TeacherApplicationRequest request,
            BindingResult bindingResult,
            @RequestPart(value = "resume", required = false) MultipartFile resume) {

        if (bindingResult != null && bindingResult.hasErrors()) {
            FieldError first = bindingResult.getFieldErrors().stream().findFirst().orElse(null);
            String message = (first != null && first.getDefaultMessage() != null)
                    ? first.getDefaultMessage()
                    : "Validation failed";
            return ResponseEntity.badRequest().body(Map.of(
                    "success", "false",
                    "message", message));
        }

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
            @Valid @RequestBody StatusUpdateRequest request) {
        String status = request.status();
        teacherApplicationService.updateStatus(id, status);
        return ResponseEntity.ok(Map.of("message", "Application status updated to " + status.toUpperCase()));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(teacherApplicationService.getStats());
    }
}
