package com.shrishailacademy.controller;

import com.shrishailacademy.dto.ApiResponse;
import com.shrishailacademy.dto.ContactRequest;
import com.shrishailacademy.model.ContactMessage;
import com.shrishailacademy.repository.ContactMessageRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contact")
public class ContactController {

    private static final Logger log = LoggerFactory.getLogger(ContactController.class);

    @Autowired
    private ContactMessageRepository contactRepo;

    /**
     * Public endpoint - anyone can submit a contact form
     * POST /api/contact
     */
    @PostMapping
    public ResponseEntity<?> submitContactForm(@Valid @RequestBody ContactRequest request) {
        try {
            ContactMessage msg = new ContactMessage();
            msg.setName(request.getName());
            msg.setEmail(request.getEmail());
            msg.setPhone(request.getPhone());
            msg.setSubject(request.getSubject());
            msg.setMessage(request.getMessage());
            msg.setStatus(ContactMessage.Status.NEW);
            contactRepo.save(msg);

            log.info("New contact message from: {} ({})", request.getName(), request.getEmail());

            return ResponseEntity.ok(ApiResponse.success(
                    "Thank you for contacting us! We'll get back to you within 24 hours."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to submit message. Please try again."));
        }
    }

    /**
     * Admin views all contact messages
     * GET /api/contact
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllMessages() {
        return ResponseEntity.ok(contactRepo.findAllByOrderByCreatedAtDesc());
    }

    /**
     * Admin views unread messages
     * GET /api/contact/unread
     */
    @GetMapping("/unread")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUnreadMessages() {
        return ResponseEntity.ok(contactRepo.findByStatusOrderByCreatedAtDesc(ContactMessage.Status.NEW));
    }

    /**
     * Admin marks a message as read/replied
     * PUT /api/contact/{id}/status
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateMessageStatus(@PathVariable Long id, @RequestParam String status) {
        try {
            ContactMessage msg = contactRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Message not found"));
            msg.setStatus(ContactMessage.Status.valueOf(status.toUpperCase()));
            contactRepo.save(msg);
            return ResponseEntity.ok(ApiResponse.success("Message status updated"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid status. Use: NEW, READ, REPLIED, ARCHIVED"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Admin gets contact message stats
     * GET /api/contact/stats
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getContactStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", contactRepo.count());
        stats.put("unread", contactRepo.countByStatus(ContactMessage.Status.NEW));
        stats.put("read", contactRepo.countByStatus(ContactMessage.Status.READ));
        stats.put("replied", contactRepo.countByStatus(ContactMessage.Status.REPLIED));
        return ResponseEntity.ok(stats);
    }
}
