package com.shrishailacademy.service;

import com.shrishailacademy.dto.PaymentRequest;
import com.shrishailacademy.model.*;
import com.shrishailacademy.repository.CourseRepository;
import com.shrishailacademy.repository.EnrollmentRepository;
import com.shrishailacademy.repository.PaymentRepository;
import com.shrishailacademy.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private NotificationService notificationService;

    /**
     * Initiate a payment for a course enrollment
     */
    @Transactional
    public Payment initiatePayment(Long userId, PaymentRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        // Check for existing successful payment
        if (paymentRepository.existsByUserIdAndCourseIdAndStatus(userId, course.getId(), Payment.Status.SUCCESS)) {
            throw new RuntimeException("Payment already completed for this course");
        }

        // Validate amount matches course fee — reject if fee not configured
        if (course.getFee() == null) {
            throw new RuntimeException("Course fee not configured. Please contact admin.");
        }
        // Use epsilon comparison to avoid floating-point precision issues
        if (Math.abs(course.getFee() - request.getAmount()) > 0.01) {
            throw new RuntimeException("Payment amount does not match course fee: ₹" + course.getFee());
        }

        // Parse payment method
        Payment.PaymentMethod method;
        try {
            method = Payment.PaymentMethod.valueOf(
                    request.getPaymentMethod() != null ? request.getPaymentMethod().toUpperCase() : "UPI");
        } catch (IllegalArgumentException e) {
            method = Payment.PaymentMethod.UPI;
        }

        // Find enrollment if exists
        Enrollment enrollment = enrollmentRepository
                .findByUserIdAndCourseId(userId, course.getId())
                .orElse(null);

        // Generate receipt number
        String receiptNumber = generateReceiptNumber();

        Payment payment = new Payment();
        payment.setUser(user);
        payment.setCourse(course);
        payment.setEnrollment(enrollment);
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(method);
        payment.setTransactionId(request.getTransactionId());
        payment.setReceiptNumber(receiptNumber);
        payment.setRemarks(request.getRemarks());
        payment.setStatus(Payment.Status.PENDING);

        Payment saved = paymentRepository.save(payment);
        log.info("Payment initiated: {} for user {} course {} amount ₹{}",
                receiptNumber, user.getEmail(), course.getTitle(), request.getAmount());

        return saved;
    }

    /**
     * Confirm a payment (simulate gateway callback or admin manual confirmation)
     */
    @Transactional
    public Payment confirmPayment(Long paymentId, String gatewayPaymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        if (payment.getStatus() == Payment.Status.SUCCESS) {
            throw new RuntimeException("Payment already confirmed");
        }
        if (payment.getStatus() != Payment.Status.PENDING) {
            throw new RuntimeException(
                    "Only PENDING payments can be confirmed. Current status: " + payment.getStatus());
        }

        payment.setStatus(Payment.Status.SUCCESS);
        payment.setGatewayPaymentId(gatewayPaymentId);
        payment.setPaidAt(LocalDateTime.now());

        // Auto-enroll if not already enrolled
        if (payment.getEnrollment() == null) {
            Optional<com.shrishailacademy.model.Enrollment> existingEnrollment = enrollmentRepository
                    .findByUserIdAndCourseId(
                            payment.getUser().getId(), payment.getCourse().getId());

            if (existingEnrollment.isPresent()) {
                Enrollment enrollment = existingEnrollment.get();
                enrollment.setStatus(Enrollment.Status.ACTIVE);
                enrollmentRepository.save(enrollment);
                payment.setEnrollment(enrollment);
            } else {
                Enrollment newEnrollment = new Enrollment();
                newEnrollment.setUser(payment.getUser());
                newEnrollment.setCourse(payment.getCourse());
                newEnrollment.setStatus(Enrollment.Status.ACTIVE);
                Enrollment saved = enrollmentRepository.save(newEnrollment);
                payment.setEnrollment(saved);
            }
        } else {
            // Ensure enrollment is active
            Enrollment enrollment = payment.getEnrollment();
            enrollment.setStatus(Enrollment.Status.ACTIVE);
            enrollmentRepository.save(enrollment);
        }

        Payment saved = paymentRepository.save(payment);

        log.info("Payment confirmed: {} receipt {} amount ₹{}",
                payment.getUser().getEmail(), payment.getReceiptNumber(), payment.getAmount());

        // Send WhatsApp notification
        notificationService.sendPaymentConfirmation(saved);

        return saved;
    }

    /**
     * Mark payment as failed
     */
    public Payment failPayment(Long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        payment.setStatus(Payment.Status.FAILED);
        payment.setRemarks(reason);

        log.warn("Payment failed: {} reason: {}", payment.getReceiptNumber(), reason);
        return paymentRepository.save(payment);
    }

    /**
     * Admin records a manual/cash payment
     */
    @Transactional
    public Payment recordManualPayment(Long userId, PaymentRequest request, Long adminId) {
        Payment payment = initiatePayment(userId, request);
        payment.setPaymentMethod(Payment.PaymentMethod.CASH);
        payment.setRemarks("Manual payment recorded by admin. " +
                (request.getRemarks() != null ? request.getRemarks() : ""));
        paymentRepository.save(payment);

        return confirmPayment(payment.getId(), "MANUAL-" + System.currentTimeMillis());
    }

    /**
     * Get payment history for a student
     */
    public List<Payment> getStudentPayments(Long userId) {
        return paymentRepository.findByUserId(userId);
    }

    /**
     * Get all payments (admin)
     */
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    /**
     * Get payment by ID
     */
    public Payment getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }

    /**
     * Get revenue statistics
     */
    public Map<String, Object> getRevenueStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRevenue", paymentRepository.getTotalRevenue());
        stats.put("successCount", paymentRepository.countByStatus(Payment.Status.SUCCESS));
        stats.put("pendingCount", paymentRepository.countByStatus(Payment.Status.PENDING));
        stats.put("failedCount", paymentRepository.countByStatus(Payment.Status.FAILED));
        stats.put("methodBreakdown", paymentRepository.getPaymentMethodStats());
        return stats;
    }

    /**
     * Generate unique receipt number
     */
    private String generateReceiptNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%06d", new java.security.SecureRandom().nextInt(1000000));
        return "BNA-" + timestamp + "-" + random;
    }
}
