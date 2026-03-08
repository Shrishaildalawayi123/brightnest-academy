package com.shrishailacademy.controller;

import com.shrishailacademy.dto.ApiResponse;
import com.shrishailacademy.dto.PaymentRequest;
import com.shrishailacademy.dto.response.PaymentResponse;
import com.shrishailacademy.exception.AccessDeniedException;
import com.shrishailacademy.model.Payment;
import com.shrishailacademy.model.User;
import com.shrishailacademy.service.PaymentService;
import com.shrishailacademy.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/api/payments", "/api/v1/payments"})
public class PaymentController {

    private final PaymentService paymentService;
    private final UserService userService;

    public PaymentController(PaymentService paymentService, UserService userService) {
        this.paymentService = paymentService;
        this.userService = userService;
    }

    /**
     * Student initiates a payment.
     */
    @PostMapping("/initiate")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse> initiatePayment(@Valid @RequestBody PaymentRequest request,
            Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        Payment payment = paymentService.initiatePayment(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Payment initiated", PaymentResponse.fromEntity(payment)));
    }

    /**
     * Admin confirms a payment.
     */
    @PostMapping("/{paymentId}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> confirmPayment(
            @PathVariable Long paymentId,
            @RequestParam(required = false) String gatewayPaymentId) {
        Payment payment = paymentService.confirmPayment(paymentId,
                gatewayPaymentId != null ? gatewayPaymentId : "ADMIN-CONFIRMED");
        return ResponseEntity.ok(ApiResponse.success("Payment confirmed", PaymentResponse.fromEntity(payment)));
    }

    /**
     * Admin marks payment as failed.
     */
    @PostMapping("/{paymentId}/fail")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> failPayment(
            @PathVariable Long paymentId,
            @RequestParam(required = false) String reason) {
        Payment payment = paymentService.failPayment(paymentId,
                reason != null ? reason : "Payment failed");
        return ResponseEntity.ok(ApiResponse.success("Payment marked as failed", PaymentResponse.fromEntity(payment)));
    }

    /**
     * Admin records manual/cash payment for a student.
     */
    @PostMapping("/manual/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> recordManualPayment(
            @PathVariable Long userId,
            @Valid @RequestBody PaymentRequest request,
            Authentication authentication) {
        User admin = userService.getUserByEmail(authentication.getName());
        Payment payment = paymentService.recordManualPayment(userId, request, admin.getId());
        return ResponseEntity
                .ok(ApiResponse.success("Manual payment recorded and confirmed", PaymentResponse.fromEntity(payment)));
    }

    /**
     * Student views their payment history.
     */
    @GetMapping("/my-payments")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<PaymentResponse>> getMyPayments(Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        List<PaymentResponse> payments = paymentService.getStudentPayments(user.getId()).stream()
                .map(PaymentResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(payments);
    }

    /**
     * Admin views all payments.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<PaymentResponse>> getAllPayments(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        Page<PaymentResponse> payments = paymentService.getAllPayments(pageable)
                .map(PaymentResponse::fromEntity);
        return ResponseEntity.ok(payments);
    }

    /**
     * Get single payment details (with IDOR protection for students).
     */
    @GetMapping("/{paymentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STUDENT')")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long paymentId, Authentication authentication) {
        User user = userService.getUserByEmail(authentication.getName());
        Payment payment = paymentService.getPaymentById(paymentId);
        // IDOR protection: students can only view their own payments
        if (!user.isAdmin() && !payment.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You can only view your own payments");
        }
        return ResponseEntity.ok(PaymentResponse.fromEntity(payment));
    }

    /**
     * Admin gets revenue statistics.
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> getRevenueStats() {
        Map<String, Object> stats = paymentService.getRevenueStats();
        return ResponseEntity.ok(ApiResponse.success("Revenue statistics", stats));
    }
}



