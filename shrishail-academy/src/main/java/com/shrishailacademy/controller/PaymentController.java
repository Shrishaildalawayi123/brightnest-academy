package com.shrishailacademy.controller;

import com.shrishailacademy.dto.ApiResponse;
import com.shrishailacademy.dto.PaymentRequest;
import com.shrishailacademy.model.Payment;
import com.shrishailacademy.model.User;
import com.shrishailacademy.service.PaymentService;
import com.shrishailacademy.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private UserService userService;

    /**
     * Student initiates a payment
     * POST /api/payments/initiate
     */
    @PostMapping("/initiate")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> initiatePayment(@Valid @RequestBody PaymentRequest request,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userService.getUserByEmail(email);
            Payment payment = paymentService.initiatePayment(user.getId(), request);
            return ResponseEntity.ok(ApiResponse.success("Payment initiated", payment));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Confirm a payment (webhook callback or admin confirmation)
     * POST /api/payments/{paymentId}/confirm
     */
    @PostMapping("/{paymentId}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> confirmPayment(
            @PathVariable Long paymentId,
            @RequestParam(required = false) String gatewayPaymentId) {
        try {
            Payment payment = paymentService.confirmPayment(paymentId,
                    gatewayPaymentId != null ? gatewayPaymentId : "ADMIN-CONFIRMED");
            return ResponseEntity.ok(ApiResponse.success("Payment confirmed", payment));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Mark payment as failed
     * POST /api/payments/{paymentId}/fail
     */
    @PostMapping("/{paymentId}/fail")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> failPayment(
            @PathVariable Long paymentId,
            @RequestParam(required = false) String reason) {
        try {
            Payment payment = paymentService.failPayment(paymentId,
                    reason != null ? reason : "Payment failed");
            return ResponseEntity.ok(ApiResponse.success("Payment marked as failed", payment));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Admin records manual/cash payment for a student
     * POST /api/payments/manual/{userId}
     */
    @PostMapping("/manual/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> recordManualPayment(
            @PathVariable Long userId,
            @Valid @RequestBody PaymentRequest request,
            Authentication authentication) {
        try {
            String email = authentication.getName();
            User admin = userService.getUserByEmail(email);
            Payment payment = paymentService.recordManualPayment(userId, request, admin.getId());
            return ResponseEntity.ok(ApiResponse.success("Manual payment recorded and confirmed", payment));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Student views their payment history
     * GET /api/payments/my-payments
     */
    @GetMapping("/my-payments")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getMyPayments(Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userService.getUserByEmail(email);
            List<Payment> payments = paymentService.getStudentPayments(user.getId());
            return ResponseEntity.ok(payments);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Admin views all payments
     * GET /api/payments
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Payment>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    /**
     * Get single payment details (with ownership check for STUDENT)
     * GET /api/payments/{paymentId}
     */
    @GetMapping("/{paymentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STUDENT')")
    public ResponseEntity<?> getPayment(@PathVariable Long paymentId, Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userService.getUserByEmail(email);
            Payment payment = paymentService.getPaymentById(paymentId);
            // IDOR protection: students can only view their own payments
            if (!user.isAdmin() && !payment.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
            }
            return ResponseEntity.ok(payment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Admin gets revenue statistics
     * GET /api/payments/stats
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getRevenueStats() {
        try {
            Map<String, Object> stats = paymentService.getRevenueStats();
            return ResponseEntity.ok(ApiResponse.success("Revenue statistics", stats));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
