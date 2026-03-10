package com.shrishailacademy.service;

import com.shrishailacademy.dto.PaymentRequest;
import com.shrishailacademy.model.Course;
import com.shrishailacademy.model.Enrollment;
import com.shrishailacademy.model.Payment;
import com.shrishailacademy.model.Tenant;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.CourseRepository;
import com.shrishailacademy.repository.EnrollmentRepository;
import com.shrishailacademy.repository.PaymentRepository;
import com.shrishailacademy.repository.UserRepository;
import com.shrishailacademy.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final Long TENANT_ID = 1L;
    private static final String TENANT_KEY = "default";

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

    @Mock
    private TenantService tenantService;

    @InjectMocks
    private PaymentService paymentService;

    @BeforeEach
    void setTenantContext() {
        TenantContext.set(TENANT_ID, TENANT_KEY);
        lenient().when(tenantService.requireCurrentTenant())
                .thenReturn(new Tenant(TENANT_ID, TENANT_KEY, "Default Tenant"));
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void initiatePaymentShouldThrowWhenAmountDoesNotMatchCourseFee() {
        User user = user(1L);
        Course course = course(7L, "Mathematics", new BigDecimal("3000.00"));
        PaymentRequest request = new PaymentRequest(7L, new BigDecimal("2500.00"), "UPI", "TXN-1", null);

        when(userRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(user));
        when(courseRepository.findByIdAndTenantId(7L, TENANT_ID)).thenReturn(Optional.of(course));
        when(paymentRepository.existsByUserIdAndCourseIdAndTenantIdAndStatus(1L, 7L, TENANT_ID, Payment.Status.SUCCESS))
                .thenReturn(false);
        when(paymentRepository.findByTransactionIdAndTenantId("TXN-1", TENANT_ID)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> paymentService.initiatePayment(1L, request));

        assertTrue(ex.getMessage().contains("Payment amount does not match course fee"));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void initiatePaymentShouldDefaultToUpiWhenPaymentMethodIsInvalid() {
        User user = user(1L);
        Course course = course(7L, "Mathematics", new BigDecimal("3000.00"));
        PaymentRequest request = new PaymentRequest(7L, new BigDecimal("3000.00"), "invalid_method", "TXN-2", "test");

        when(userRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(user));
        when(courseRepository.findByIdAndTenantId(7L, TENANT_ID)).thenReturn(Optional.of(course));
        when(paymentRepository.existsByUserIdAndCourseIdAndTenantIdAndStatus(1L, 7L, TENANT_ID, Payment.Status.SUCCESS))
                .thenReturn(false);
        when(paymentRepository.findByTransactionIdAndTenantId("TXN-2", TENANT_ID)).thenReturn(Optional.empty());
        when(enrollmentRepository.findByUserIdAndCourseIdAndTenantId(1L, 7L, TENANT_ID)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(101L);
            return payment;
        });

        Payment payment = paymentService.initiatePayment(1L, request);

        assertEquals(101L, payment.getId());
        assertEquals(Payment.PaymentMethod.UPI, payment.getPaymentMethod());
        assertEquals(Payment.Status.PENDING, payment.getStatus());
        assertEquals(new BigDecimal("3000.00"), payment.getAmount());
        assertNull(payment.getEnrollment());
        assertNotNull(payment.getReceiptNumber());
        assertTrue(payment.getReceiptNumber().startsWith("BNA-"));
    }

    @Test
    void confirmPaymentShouldCreateEnrollmentWhenMissing() {
        User user = user(1L);
        Course course = course(2L, "Science", new BigDecimal("3500.00"));

        Payment pending = new Payment();
        pending.setId(5L);
        pending.setUser(user);
        pending.setCourse(course);
        pending.setAmount(new BigDecimal("3500.00"));
        pending.setStatus(Payment.Status.PENDING);
        pending.setEnrollment(null);
        pending.setReceiptNumber("BNA-20260226143000-000001");

        when(paymentRepository.findByIdAndTenantId(5L, TENANT_ID)).thenReturn(Optional.of(pending));
        when(enrollmentRepository.findByUserIdAndCourseIdAndTenantId(1L, 2L, TENANT_ID)).thenReturn(Optional.empty());
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

        when(paymentRepository.findByIdAndTenantId(9L, TENANT_ID)).thenReturn(Optional.of(payment));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> paymentService.confirmPayment(9L, "gw-1"));

        assertTrue(ex.getMessage().contains("Cannot transition"));
        assertTrue(ex.getMessage().contains("Payment"));
        assertTrue(ex.getMessage().contains("FAILED"));
        assertTrue(ex.getMessage().contains("SUCCESS"));
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void failPaymentShouldUpdateStatusAndReason() {
        Payment pending = new Payment();
        pending.setId(8L);
        pending.setStatus(Payment.Status.PENDING);
        pending.setReceiptNumber("BNA-20260226143100-000002");

        when(paymentRepository.findByIdAndTenantId(8L, TENANT_ID)).thenReturn(Optional.of(pending));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Payment failed = paymentService.failPayment(8L, "Gateway declined");

        assertEquals(Payment.Status.FAILED, failed.getStatus());
        assertEquals("Gateway declined", failed.getRemarks());
        verify(paymentRepository).save(pending);
    }

    @Test
    void getRevenueStatsShouldReturnAggregatedValues() {
        List<Object[]> methodBreakdown = Collections
                .singletonList(new Object[] { Payment.PaymentMethod.UPI, 2L, 6000.0 });

        when(paymentRepository.getTotalRevenueByTenant(TENANT_ID)).thenReturn(new BigDecimal("10000.00"));
        when(paymentRepository.countByStatusAndTenantId(Payment.Status.SUCCESS, TENANT_ID)).thenReturn(3L);
        when(paymentRepository.countByStatusAndTenantId(Payment.Status.PENDING, TENANT_ID)).thenReturn(1L);
        when(paymentRepository.countByStatusAndTenantId(Payment.Status.FAILED, TENANT_ID)).thenReturn(2L);
        when(paymentRepository.getPaymentMethodStatsByTenant(TENANT_ID)).thenReturn(methodBreakdown);

        Map<String, Object> stats = paymentService.getRevenueStats();

        assertEquals(new BigDecimal("10000.00"), stats.get("totalRevenue"));
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

    private Course course(Long id, String title, BigDecimal fee) {
        Course course = new Course();
        course.setId(id);
        course.setTitle(title);
        course.setFee(fee);
        return course;
    }
}
