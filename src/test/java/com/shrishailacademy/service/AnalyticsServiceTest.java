package com.shrishailacademy.service;

import com.shrishailacademy.model.ContactMessage;
import com.shrishailacademy.model.DemoBooking;
import com.shrishailacademy.model.Enrollment;
import com.shrishailacademy.model.Payment;
import com.shrishailacademy.model.TeacherApplication;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.AttendanceRepository;
import com.shrishailacademy.repository.BlogPostRepository;
import com.shrishailacademy.repository.ContactMessageRepository;
import com.shrishailacademy.repository.CourseRepository;
import com.shrishailacademy.repository.DemoBookingRepository;
import com.shrishailacademy.repository.EnrollmentRepository;
import com.shrishailacademy.repository.PaymentRepository;
import com.shrishailacademy.repository.SiteVisitRepository;
import com.shrishailacademy.repository.TeacherApplicationRepository;
import com.shrishailacademy.repository.UserRepository;
import com.shrishailacademy.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    private static final Long TENANT_ID = 101L;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private ContactMessageRepository contactRepo;

    @Mock
    private BlogPostRepository blogPostRepository;

    @Mock
    private DemoBookingRepository demoBookingRepository;

    @Mock
    private TeacherApplicationRepository teacherApplicationRepository;

    @Mock
    private SiteVisitRepository siteVisitRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        TenantContext.set(TENANT_ID, "tenant-test");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void getDashboardStatsShouldUseTenantScopedCounts() {
        when(userRepository.countByTenantId(TENANT_ID)).thenReturn(25L);
        when(userRepository.countByRoleAndTenantId(User.Role.STUDENT, TENANT_ID)).thenReturn(20L);

        when(courseRepository.countByTenantId(TENANT_ID)).thenReturn(12L);

        when(enrollmentRepository.countByTenantId(TENANT_ID)).thenReturn(40L);
        when(enrollmentRepository.countByStatusAndTenantId(Enrollment.Status.ACTIVE, TENANT_ID)).thenReturn(31L);

        when(paymentRepository.countByTenantId(TENANT_ID)).thenReturn(18L);
        when(paymentRepository.countByStatusAndTenantId(Payment.Status.SUCCESS, TENANT_ID)).thenReturn(15L);
        when(paymentRepository.countByStatusAndTenantId(Payment.Status.PENDING, TENANT_ID)).thenReturn(2L);

        when(attendanceRepository.countByTenantId(TENANT_ID)).thenReturn(73L);

        when(contactRepo.countByTenantId(TENANT_ID)).thenReturn(9L);
        when(contactRepo.countByTenantIdAndStatus(TENANT_ID, ContactMessage.Status.NEW)).thenReturn(3L);

        when(blogPostRepository.countByTenantId(TENANT_ID)).thenReturn(11L);
        when(blogPostRepository.countByTenantIdAndPublished(TENANT_ID, true)).thenReturn(7L);

        when(demoBookingRepository.countByTenantId(TENANT_ID)).thenReturn(6L);
        when(demoBookingRepository.countByTenantIdAndStatus(TENANT_ID, DemoBooking.Status.PENDING)).thenReturn(1L);

        when(teacherApplicationRepository.countByTenantId(TENANT_ID)).thenReturn(4L);
        when(teacherApplicationRepository.countByTenantIdAndStatus(TENANT_ID, TeacherApplication.Status.NEW))
                .thenReturn(2L);

        when(siteVisitRepository.countByVisitedAtAfter(any(LocalDateTime.class))).thenReturn(200L, 900L);
        when(siteVisitRepository.countUniqueSessionsAfter(any(LocalDateTime.class))).thenReturn(80L, 310L);

        Map<String, Object> stats = analyticsService.getDashboardStats();

        assertEquals(25L, stats.get("totalUsers"));
        assertEquals(20L, stats.get("totalStudents"));
        assertEquals(12L, stats.get("totalCourses"));
        assertEquals(40L, stats.get("totalEnrollments"));
        assertEquals(31L, stats.get("activeEnrollments"));
        assertEquals(18L, stats.get("totalPayments"));
        assertEquals(15L, stats.get("successPayments"));
        assertEquals(2L, stats.get("pendingPayments"));
        assertEquals(73L, stats.get("totalAttendanceRecords"));
        assertEquals(9L, stats.get("totalContactMessages"));
        assertEquals(3L, stats.get("unreadContactMessages"));
        assertEquals(11L, stats.get("totalBlogPosts"));
        assertEquals(7L, stats.get("publishedBlogPosts"));
        assertEquals(6L, stats.get("totalDemoBookings"));
        assertEquals(1L, stats.get("pendingDemoBookings"));
        assertEquals(4L, stats.get("totalTeacherApplications"));
        assertEquals(2L, stats.get("newTeacherApplications"));
        assertEquals(200L, stats.get("pageViews24h"));
        assertEquals(80L, stats.get("uniqueVisitors24h"));
        assertEquals(900L, stats.get("pageViews7d"));
        assertEquals(310L, stats.get("uniqueVisitors7d"));

        verify(userRepository).countByTenantId(TENANT_ID);
        verify(userRepository).countByRoleAndTenantId(eq(User.Role.STUDENT), eq(TENANT_ID));
        verify(courseRepository).countByTenantId(TENANT_ID);
        verify(enrollmentRepository).countByTenantId(TENANT_ID);
        verify(enrollmentRepository).countByStatusAndTenantId(eq(Enrollment.Status.ACTIVE), eq(TENANT_ID));
        verify(paymentRepository).countByTenantId(TENANT_ID);
        verify(paymentRepository).countByStatusAndTenantId(eq(Payment.Status.SUCCESS), eq(TENANT_ID));
        verify(paymentRepository).countByStatusAndTenantId(eq(Payment.Status.PENDING), eq(TENANT_ID));
        verify(attendanceRepository).countByTenantId(TENANT_ID);
        verify(contactRepo).countByTenantId(TENANT_ID);
        verify(contactRepo).countByTenantIdAndStatus(eq(TENANT_ID), eq(ContactMessage.Status.NEW));
        verify(blogPostRepository).countByTenantId(TENANT_ID);
        verify(blogPostRepository).countByTenantIdAndPublished(TENANT_ID, true);
        verify(demoBookingRepository).countByTenantId(TENANT_ID);
        verify(demoBookingRepository).countByTenantIdAndStatus(eq(TENANT_ID), eq(DemoBooking.Status.PENDING));
        verify(teacherApplicationRepository).countByTenantId(TENANT_ID);
        verify(teacherApplicationRepository).countByTenantIdAndStatus(eq(TENANT_ID), eq(TeacherApplication.Status.NEW));
    }

    @Test
    void getDashboardStatsShouldFailWhenTenantContextMissing() {
        TenantContext.clear();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> analyticsService.getDashboardStats());

        assertEquals("Tenant context is missing", exception.getMessage());
    }
}
