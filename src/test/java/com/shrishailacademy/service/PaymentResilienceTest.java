package com.shrishailacademy.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.shrishailacademy.dto.PaymentRequest;
import com.shrishailacademy.exception.AccessDeniedException;
import com.shrishailacademy.exception.InvalidStateTransitionException;
import com.shrishailacademy.model.*;
import com.shrishailacademy.repository.*;
import com.shrishailacademy.tenant.TenantContext;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.util.*;

class PaymentResilienceTest {
    final PaymentRepository payments = mock(PaymentRepository.class);
    final UserRepository users = mock(UserRepository.class);
    final CourseRepository courses = mock(CourseRepository.class);
    final EnrollmentRepository enrollments = mock(EnrollmentRepository.class);
    final NotificationService notifications = mock(NotificationService.class);
    final EmailService email = mock(EmailService.class);
    final TenantService tenants = mock(TenantService.class);
    PaymentService service;
    User student;
    Course course;
    Tenant tenant;

    @BeforeEach
    void setup() {
        TenantContext.set(7L, "a");
        tenant = new Tenant();
        tenant.setId(7L);
        student = new User();
        student.setId(11L);
        student.setEmail("student@example.com");
        course = new Course();
        course.setId(21L);
        course.setTitle("Maths");
        course.setFee(new BigDecimal("1000"));
        ObjectProvider<EmailService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(email);
        service =
                new PaymentService(
                        payments, users, courses, enrollments, notifications, tenants, provider);
        when(tenants.requireCurrentTenant()).thenReturn(tenant);
        when(payments.save(any())).thenAnswer(i -> i.getArgument(0));
        when(enrollments.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    Payment pending() {
        Payment p = new Payment();
        p.setId(1L);
        p.setUser(student);
        p.setCourse(course);
        p.setAmount(course.getFee());
        p.setStatus(Payment.Status.PENDING);
        p.setReceiptNumber("test-receipt");
        return p;
    }

    @Test
    void sameStudentRetryIsIdempotentButAnotherStudentCannotReuseItsKey() {
        Payment p = pending();
        when(payments.findByIdempotencyKeyAndTenantId("retry-key", 7L)).thenReturn(Optional.of(p));
        var request = new PaymentRequest(21L, course.getFee(), "UPI", null, null);
        assertSame(p, service.initiatePayment(11L, request, "retry-key"));
        verify(payments, never()).save(any());
        verifyNoInteractions(users, courses);
        assertThrows(
                AccessDeniedException.class,
                () -> service.initiatePayment(99L, request, "retry-key"));
    }

    @Test
    void confirmationSurvivesNotificationFailuresAndActivatesEachEnrollmentPath() throws Exception {
        doThrow(new IllegalStateException("notification unavailable"))
                .when(notifications)
                .sendPaymentConfirmation(any());
        doThrow(new jakarta.mail.MessagingException("SMTP unavailable"))
                .when(email)
                .sendPaymentReceipt(any(), any());
        for (int route = 0; route < 3; route++) {
            Payment p = pending();
            Enrollment e = new Enrollment();
            e.setUser(student);
            e.setCourse(course);
            e.setStatus(Enrollment.Status.PENDING);
            when(payments.findByIdAndTenantId(1L, 7L)).thenReturn(Optional.of(p));
            when(enrollments.findByUserIdAndCourseIdAndTenantId(11L, 21L, 7L))
                    .thenReturn(route == 1 ? Optional.of(e) : Optional.empty());
            if (route == 2) p.setEnrollment(e);
            Payment confirmed = service.confirmPayment(1L, "gateway-test");
            assertEquals(Payment.Status.SUCCESS, confirmed.getStatus());
            assertNotNull(confirmed.getPaidAt());
            assertEquals(Enrollment.Status.ACTIVE, confirmed.getEnrollment().getStatus());
            assertThrows(
                    InvalidStateTransitionException.class,
                    () -> service.confirmPayment(1L, "gateway-test"));
        }
        verify(email, times(3)).sendPaymentReceipt(any(), eq(student));
    }

    @Test
    void gatewayOrderLookupDoesNotExposeAnotherStudentsPayment() {
        Payment p = pending();
        when(payments.findByGatewayOrderIdAndTenantId("order-test", 7L)).thenReturn(Optional.of(p));
        assertSame(p, service.getPaymentByGatewayOrderIdForStudent("order-test", 11L));
        assertThrows(
                AccessDeniedException.class,
                () -> service.getPaymentByGatewayOrderIdForStudent("order-test", 99L));
        when(payments.findByIdAndTenantId(1L, 7L)).thenReturn(Optional.of(p));
        assertEquals(
                "gateway-order",
                service.attachGatewayOrderId(1L, "gateway-order").getGatewayOrderId());
    }
}
