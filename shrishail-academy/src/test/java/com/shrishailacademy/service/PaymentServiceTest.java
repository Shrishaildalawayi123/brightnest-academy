package com.shrishailacademy.service;

import com.shrishailacademy.dto.PaymentRequest;
import com.shrishailacademy.model.Course;
import com.shrishailacademy.model.Enrollment;
import com.shrishailacademy.model.Payment;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.CourseRepository;
import com.shrishailacademy.repository.EnrollmentRepository;
import com.shrishailacademy.repository.PaymentRepository;
import com.shrishailacademy.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void initiatePaymentShouldThrowWhenAmountDoesNotMatchCourseFee() {
        User user = user(1L);
        Course course = course(7L, "Mathematics", 3000.0);
        PaymentRequest request = new PaymentRequest(7L, 2500.0, "UPI", "TXN-1", null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(courseRepository.findById(7L)).thenReturn(Optional.of(course));
        when(paymentRepository.existsByUserIdAndCourseIdAndStatus(1L, 7L, Payment.Status.SUCCESS)).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> paymentService.initiatePayment(1L, request));

        assertTrue(ex.getMessage().contains("Payment amount does not match course fee"));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void initiatePaymentShouldDefaultToUpiWhenPaymentMethodIsInvalid() {
        User user = user(1L);
        Course course = course(7L, "Mathematics", 3000.0);
        PaymentRequest request = new PaymentRequest(7L, 3000.0, "invalid_method", "TXN-2", "test");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(courseRepository.findById(7L)).thenReturn(Optional.of(course));
        when(paymentRepository.existsByUserIdAndCourseIdAndStatus(1L, 7L, Payment.Status.SUCCESS)).thenReturn(false);
        when(enrollmentRepository.findByUserIdAndCourseId(1L, 7L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(101L);
            return payment;
        });

        Payment payment = paymentService.initiatePayment(1L, request);

        assertEquals(101L, payment.getId());
        assertEquals(Payment.PaymentMethod.UPI, payment.getPaymentMethod());
        assertEquals(Payment.Status.PENDING, payment.getStatus());
        assertEquals(3000.0, payment.getAmount());
        assertNull(payment.getEnrollment());
        assertNotNull(payment.getReceiptNumber());
        assertTrue(payment.getReceiptNumber().startsWith("BNA-"));
    }

    @Test
    void confirmPaymentShouldCreateEnrollmentWhenMissing() {
        User user = user(1L);
        Course course = course(2L, "Science", 3500.0);

        Payment pending = new Payment();
        pending.setId(5L);
        pending.setUser(user);
        pending.setCourse(course);
        pending.setAmount(3500.0);
        pending.setStatus(Payment.Status.PENDING);
        pending.setEnrollment(null);
        pending.setReceiptNumber("BNA-20260226143000-000001");

        when(paymentRepository.findById(5L)).thenReturn(Optional.of(pending));
        when(enrollmentRepository.findByUserIdAndCourseId(1L, 2L)).thenReturn(Optional.empty());
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> {
            Enrollment enrollment = invocation.getArgument(0);
            enrollment.setId(50L);
            return enrollment;
        });
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment confirmed = paymentService.confirmPayment(5L, "gw-pay-123");

        assertEquals(Payment.Status.SUCCESS, confirmed.getStatus());
        assertEquals("gw-pay-123", confirmed.getGatewayPaymentId());
        assertNotNull(confirmed.getPaidAt());
        assertNotNull(confirmed.getEnrollment());
        assertEquals(Enrollment.Status.ACTIVE, confirmed.getEnrollment().getStatus());
        verify(notificationService).sendPaymentConfirmation(confirmed);
    }

    @Test
    void confirmPaymentShouldThrowWhenStatusIsNotPending() {
        Payment payment = new Payment();
        payment.setId(9L);
        payment.setStatus(Payment.Status.FAILED);

        when(paymentRepository.findById(9L)).thenReturn(Optional.of(payment));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> paymentService.confirmPayment(9L, "gw-1"));

        assertEquals("Only PENDING payments can be confirmed. Current status: FAILED", ex.getMessage());
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void failPaymentShouldUpdateStatusAndReason() {
        Payment pending = new Payment();
        pending.setId(8L);
        pending.setStatus(Payment.Status.PENDING);
        pending.setReceiptNumber("BNA-20260226143100-000002");

        when(paymentRepository.findById(8L)).thenReturn(Optional.of(pending));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment failed = paymentService.failPayment(8L, "Gateway declined");

        assertEquals(Payment.Status.FAILED, failed.getStatus());
        assertEquals("Gateway declined", failed.getRemarks());
        verify(paymentRepository).save(pending);
    }

    @Test
    void getRevenueStatsShouldReturnAggregatedValues() {
        List<Object[]> methodBreakdown = Collections.singletonList(new Object[] { Payment.PaymentMethod.UPI, 2L, 6000.0 });

        when(paymentRepository.getTotalRevenue()).thenReturn(10000.0);
        when(paymentRepository.countByStatus(Payment.Status.SUCCESS)).thenReturn(3L);
        when(paymentRepository.countByStatus(Payment.Status.PENDING)).thenReturn(1L);
        when(paymentRepository.countByStatus(Payment.Status.FAILED)).thenReturn(2L);
        when(paymentRepository.getPaymentMethodStats()).thenReturn(methodBreakdown);

        Map<String, Object> stats = paymentService.getRevenueStats();

        assertEquals(10000.0, stats.get("totalRevenue"));
        assertEquals(3L, stats.get("successCount"));
        assertEquals(1L, stats.get("pendingCount"));
        assertEquals(2L, stats.get("failedCount"));
        assertEquals(methodBreakdown, stats.get("methodBreakdown"));
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setName("Student " + id);
        user.setEmail("student" + id + "@example.com");
        user.setPassword("encoded-password");
        user.setRole(User.Role.STUDENT);
        return user;
    }

    private Course course(Long id, String title, Double fee) {
        Course course = new Course();
        course.setId(id);
        course.setTitle(title);
        course.setFee(fee);
        return course;
    }
}
