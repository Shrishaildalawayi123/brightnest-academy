package com.shrishailacademy.controller;

import com.shrishailacademy.dto.ApiResponse;
import com.shrishailacademy.dto.ContactRequest;
import com.shrishailacademy.service.ContactService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/contact")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    /**
     * Public endpoint - anyone can submit a contact form
     */
    @PostMapping
    public ResponseEntity<ApiResponse> submitContactForm(@Valid @RequestBody ContactRequest request) {
        contactService.submitMessage(request);
        return ResponseEntity.ok(ApiResponse.success(
                "Thank you for contacting us! We'll get back to you within 24 hours."));
    }

    /**
     * Admin views all contact messages
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getAllMessages() {
        return ResponseEntity.ok(ApiResponse.success("Messages retrieved", contactService.getAllMessages()));
    }

    /**
     * Admin views unread messages
     */
    @GetMapping("/unread")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getUnreadMessages() {
        return ResponseEntity.ok(ApiResponse.success("Unread messages retrieved", contactService.getUnreadMessages()));
    }

    /**
     * Admin marks a message as read/replied
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> updateMessageStatus(@PathVariable Long id, @RequestParam String status) {
        contactService.updateMessageStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Message status updated"));
    }

    /**
     * Admin gets contact message stats
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getContactStats() {
        return ResponseEntity.ok(ApiResponse.success("Stats retrieved", contactService.getStats()));
    }
}
