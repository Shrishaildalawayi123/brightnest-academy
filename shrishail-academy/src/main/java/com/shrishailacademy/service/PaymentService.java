package com.shrishailacademy.service;

import com.shrishailacademy.dto.PaymentRequest;
import com.shrishailacademy.exception.*;
import com.shrishailacademy.model.*;
import com.shrishailacademy.repository.CourseRepository;
import com.shrishailacademy.repository.EnrollmentRepository;
import com.shrishailacademy.repository.PaymentRepository;
import com.shrishailacademy.repository.UserRepository;
import com.shrishailacademy.tenant.TenantContext;
import com.shrishailacademy.util.InputSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final NotificationService notificationService;
    private final TenantService tenantService;

    public PaymentService(PaymentRepository paymentRepository,
            UserRepository userRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            NotificationService notificationService,
            TenantService tenantService) {
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.notificationService = notificationService;
        this.tenantService = tenantService;
    }

    /**
     * Initiate a payment for a course enrollment.
     * Validates amount matches course fee server-side. Prevents duplicate
     * successful payments.
     */
    @Transactional
    public Payment initiatePayment(Long userId, PaymentRequest request) {
        Long tenantId = TenantContext.requireTenantId();

        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Course course = courseRepository.findByIdAndTenantId(request.getCourseId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", request.getCourseId()));

        // Prevent duplicate successful payments (idempotency)
        if (paymentRepository.existsByUserIdAndCourseIdAndTenantIdAndStatus(userId, course.getId(), tenantId,
                Payment.Status.SUCCESS)) {
            throw new DuplicateResourceException("Payment", "userId+courseId", userId + "+" + course.getId());
        }

        // Prevent duplicate pending payments via transactionId
        String sanitizedTransactionId = InputSanitizer.sanitizeAndTruncateNullable(request.getTransactionId(), 100);
        if (sanitizedTransactionId != null && !sanitizedTransactionId.isBlank()) {
            paymentRepository.findByTransactionIdAndTenantId(sanitizedTransactionId, tenantId).ifPresent(existing -> {
                throw new DuplicateResourceException("Payment", "transactionId", sanitizedTransactionId);
            });
        }

        // Server-side amount validation against course fee
        if (course.getFee() == null) {
            throw new PaymentException("Course fee not configured. Please contact admin.");
        }
        if (course.getFee().compareTo(request.getAmount()) != 0) {
            throw new PaymentException("Payment amount does not match course fee: ₹" + course.getFee());
        }

        // Parse payment method safely
        Payment.PaymentMethod method;
        try {
            method = Payment.PaymentMethod.valueOf(
                    request.getPaymentMethod() != null
                            ? InputSanitizer.sanitize(request.getPaymentMethod()).toUpperCase()
                            : "UPI");
        } catch (IllegalArgumentException e) {
            method = Payment.PaymentMethod.UPI;
        }

        Enrollment enrollment = enrollmentRepository
                .findByUserIdAndCourseIdAndTenantId(userId, course.getId(), tenantId)
                .orElse(null);

        String receiptNumber = generateReceiptNumber();

        Payment payment = new Payment();
        payment.setTenant(tenantService.requireCurrentTenant());
        payment.setUser(user);
        payment.setCourse(course);
        payment.setEnrollment(enrollment);
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(method);
        payment.setTransactionId(sanitizedTransactionId);
        payment.setReceiptNumber(receiptNumber);
        payment.setRemarks(InputSanitizer.sanitizeNullable(request.getRemarks()));
        payment.setStatus(Payment.Status.PENDING);

        Payment saved = paymentRepository.save(payment);
        log.info("PAYMENT_INITIATED: receipt={} user={} course='{}' amount=₹{}",
                receiptNumber, user.getEmail(), course.getTitle(), request.getAmount());

        return saved;
    }

    /**
     * Confirm a payment. Enforces valid state transitions: only PENDING → SUCCESS.
     * Auto-enrolls the student if not already enrolled.
     */
    @Transactional
    public Payment confirmPayment(Long paymentId, String gatewayPaymentId) {
        Long tenantId = TenantContext.requireTenantId();
        Payment payment = paymentRepository.findByIdAndTenantId(paymentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        if (payment.getStatus() == Payment.Status.SUCCESS) {
            throw new InvalidStateTransitionException("Payment", "SUCCESS", "SUCCESS");
        }
        if (payment.getStatus() != Payment.Status.PENDING) {
            throw new InvalidStateTransitionException("Payment",
                    payment.getStatus().name(), "SUCCESS");
        }

        payment.setStatus(Payment.Status.SUCCESS);
        payment.setGatewayPaymentId(InputSanitizer.sanitizeAndTruncate(gatewayPaymentId, 100));
        payment.setPaidAt(LocalDateTime.now());

        // Auto-enroll if not already enrolled
        if (payment.getEnrollment() == null) {
            Optional<Enrollment> existingEnrollment = enrollmentRepository
                    .findByUserIdAndCourseIdAndTenantId(payment.getUser().getId(), payment.getCourse().getId(),
                            tenantId);

            if (existingEnrollment.isPresent()) {
                Enrollment enrollment = existingEnrollment.get();
                enrollment.setStatus(Enrollment.Status.ACTIVE);
                enrollmentRepository.save(enrollment);
                payment.setEnrollment(enrollment);
            } else {
                Enrollment newEnrollment = new Enrollment();
                newEnrollment.setTenant(tenantService.requireCurrentTenant());
                newEnrollment.setUser(payment.getUser());
                newEnrollment.setCourse(payment.getCourse());
                newEnrollment.setStatus(Enrollment.Status.ACTIVE);
                Enrollment saved = enrollmentRepository.save(newEnrollment);
                payment.setEnrollment(saved);
            }
        } else {
            Enrollment enrollment = payment.getEnrollment();
            enrollment.setStatus(Enrollment.Status.ACTIVE);
            enrollmentRepository.save(enrollment);
        }

        Payment saved = paymentRepository.save(payment);

        log.info("PAYMENT_CONFIRMED: receipt={} user={} amount=₹{}",
                payment.getReceiptNumber(), payment.getUser().getEmail(), payment.getAmount());

        // Send notification (failures here must not break payment flow)
        try {
            notificationService.sendPaymentConfirmation(saved);
        } catch (Exception e) {
            log.error("Failed to send payment confirmation notification for receipt={}: {}",
                    payment.getReceiptNumber(), e.getMessage());
        }

        return saved;
    }

    /**
     * Mark payment as failed. Only PENDING payments can be failed.
     */
    @Transactional
    public Payment failPayment(Long paymentId, String reason) {
        Long tenantId = TenantContext.requireTenantId();
        Payment payment = paymentRepository.findByIdAndTenantId(paymentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        if (payment.getStatus() != Payment.Status.PENDING) {
            throw new InvalidStateTransitionException("Payment", payment.getStatus().name(), "FAILED");
        }

        payment.setStatus(Payment.Status.FAILED);
        payment.setRemarks(InputSanitizer.sanitizeNullable(reason));

        log.warn("PAYMENT_FAILED: receipt={} reason={}", payment.getReceiptNumber(), payment.getRemarks());
        return paymentRepository.save(payment);
    }

    /**
     * Admin records a manual/cash payment and auto-confirms it.
     */
    @Transactional
    public Payment recordManualPayment(Long userId, PaymentRequest request, Long adminId) {
        Payment payment = initiatePayment(userId, request);
        payment.setPaymentMethod(Payment.PaymentMethod.CASH);
        String remarks = request.getRemarks() != null ? request.getRemarks() : "";
        payment.setRemarks(InputSanitizer.sanitizeNullable("Manual payment recorded by admin. " + remarks));
        paymentRepository.save(payment);

        log.info("PAYMENT_MANUAL: receipt={} userId={} adminId={}", payment.getReceiptNumber(), userId, adminId);
        return confirmPayment(payment.getId(), "MANUAL-" + System.currentTimeMillis());
    }

    public List<Payment> getStudentPayments(Long userId) {
        Long tenantId = TenantContext.requireTenantId();
        return paymentRepository.findByUserIdAndTenantId(userId, tenantId);
    }

    public List<Payment> getAllPayments() {
        Long tenantId = TenantContext.requireTenantId();
        return paymentRepository.findAllByTenantId(tenantId);
    }

    public Page<Payment> getAllPayments(Pageable pageable) {
        Long tenantId = TenantContext.requireTenantId();
        return paymentRepository.findAllByTenantId(tenantId, pageable);
    }

    public Payment getPaymentById(Long paymentId) {
        Long tenantId = TenantContext.requireTenantId();
        return paymentRepository.findByIdAndTenantId(paymentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));
    }

    public Map<String, Object> getRevenueStats() {
        Long tenantId = TenantContext.requireTenantId();
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalRevenue", paymentRepository.getTotalRevenueByTenant(tenantId));
        stats.put("successCount", paymentRepository.countByStatusAndTenantId(Payment.Status.SUCCESS, tenantId));
        stats.put("pendingCount", paymentRepository.countByStatusAndTenantId(Payment.Status.PENDING, tenantId));
        stats.put("failedCount", paymentRepository.countByStatusAndTenantId(Payment.Status.FAILED, tenantId));
        stats.put("methodBreakdown", paymentRepository.getPaymentMethodStatsByTenant(tenantId));
        return stats;
    }

    private String generateReceiptNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%06d", new java.security.SecureRandom().nextInt(1000000));
        return "BNA-" + timestamp + "-" + random;
    }
}
