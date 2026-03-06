package com.shrishailacademy.service;

import com.shrishailacademy.model.Course;
import com.shrishailacademy.model.Enrollment;
import com.shrishailacademy.model.Tenant;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.CourseRepository;
import com.shrishailacademy.repository.EnrollmentRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    private static final Long TENANT_ID = 1L;
    private static final String TENANT_KEY = "default";

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private TenantService tenantService;

    @InjectMocks
    private EnrollmentService enrollmentService;

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
    void enrollStudentShouldThrowWhenAlreadyEnrolled() {
        when(enrollmentRepository.existsByUserIdAndCourseIdAndTenantIdAndStatusNot(1L, 10L, TENANT_ID,
                Enrollment.Status.CANCELLED))
                .thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> enrollmentService.enrollStudent(1L, 10L));

        assertTrue(ex.getMessage().contains("Enrollment already exists"));
        assertTrue(ex.getMessage().contains("1+10"));
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
        verify(notificationService, never()).sendEnrollmentConfirmation(any(Enrollment.class));
    }

    @Test
    void enrollStudentShouldSaveAndNotifyWhenValid() {
        User student = user(1L, User.Role.STUDENT);
        Course course = course(10L, "Science");

        when(enrollmentRepository.existsByUserIdAndCourseIdAndTenantIdAndStatusNot(1L, 10L, TENANT_ID,
                Enrollment.Status.CANCELLED))
                .thenReturn(false);
        when(userRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(student));
        when(courseRepository.findByIdAndTenantId(10L, TENANT_ID)).thenReturn(Optional.of(course));
        when(enrollmentRepository.save(any(Enrollment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Enrollment saved = enrollmentService.enrollStudent(1L, 10L);

        assertSame(student, saved.getUser());
        assertSame(course, saved.getCourse());
        assertEquals(Enrollment.Status.ACTIVE, saved.getStatus());
        verify(notificationService).sendEnrollmentConfirmation(saved);
    }

    @Test
    void cancelEnrollmentShouldThrowWhenAlreadyCancelled() {
        Enrollment enrollment = new Enrollment();
        enrollment.setId(100L);
        enrollment.setStatus(Enrollment.Status.CANCELLED);
        enrollment.setUser(user(1L, User.Role.STUDENT));

        when(enrollmentRepository.findByIdAndTenantId(100L, TENANT_ID)).thenReturn(Optional.of(enrollment));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> enrollmentService.cancelEnrollment(100L, 1L, User.Role.STUDENT.name()));

        assertEquals("Enrollment is already cancelled", ex.getMessage());
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void cancelEnrollmentShouldThrowWhenStudentCancelsOtherUserEnrollment() {
        Enrollment enrollment = new Enrollment();
        enrollment.setId(200L);
        enrollment.setStatus(Enrollment.Status.ACTIVE);
        enrollment.setUser(user(2L, User.Role.STUDENT));

        when(enrollmentRepository.findByIdAndTenantId(200L, TENANT_ID)).thenReturn(Optional.of(enrollment));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> enrollmentService.cancelEnrollment(200L, 1L, User.Role.STUDENT.name()));

        assertEquals("You can only cancel your own enrollment", ex.getMessage());
        verify(enrollmentRepository, never()).save(any(Enrollment.class));
    }

    @Test
    void cancelEnrollmentShouldAllowAdminToCancelAnyEnrollment() {
        Enrollment enrollment = new Enrollment();
        enrollment.setId(300L);
        enrollment.setStatus(Enrollment.Status.ACTIVE);
        enrollment.setUser(user(2L, User.Role.STUDENT));

        when(enrollmentRepository.findByIdAndTenantId(300L, TENANT_ID)).thenReturn(Optional.of(enrollment));

        enrollmentService.cancelEnrollment(300L, 1L, User.Role.ADMIN.name());

        assertEquals(Enrollment.Status.CANCELLED, enrollment.getStatus());
        verify(enrollmentRepository).save(enrollment);
    }

    private User user(Long id, User.Role role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setName("User " + id);
        user.setEmail("user" + id + "@example.com");
        user.setPassword("encoded-password");
        return user;
    }

    private Course course(Long id, String title) {
        Course course = new Course();
        course.setId(id);
        course.setTitle(title);
        course.setFee(new BigDecimal("2500.00"));
        return course;
    }
}
