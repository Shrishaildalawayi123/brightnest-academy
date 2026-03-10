package com.shrishailacademy.service;

import com.shrishailacademy.model.*;
import com.shrishailacademy.repository.*;
import com.shrishailacademy.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Analytics Service - Aggregates dashboard metrics.
 * Centralizes all analytics queries behind a single service.
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PaymentRepository paymentRepository;
    private final AttendanceRepository attendanceRepository;
    private final ContactMessageRepository contactRepo;
    private final BlogPostRepository blogPostRepository;
    private final DemoBookingRepository demoBookingRepository;
    private final TeacherApplicationRepository teacherApplicationRepository;
    private final SiteVisitRepository siteVisitRepository;

    public AnalyticsService(UserRepository userRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            PaymentRepository paymentRepository,
            AttendanceRepository attendanceRepository,
            ContactMessageRepository contactRepo,
            BlogPostRepository blogPostRepository,
            DemoBookingRepository demoBookingRepository,
            TeacherApplicationRepository teacherApplicationRepository,
            SiteVisitRepository siteVisitRepository) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.paymentRepository = paymentRepository;
        this.attendanceRepository = attendanceRepository;
        this.contactRepo = contactRepo;
        this.blogPostRepository = blogPostRepository;
        this.demoBookingRepository = demoBookingRepository;
        this.teacherApplicationRepository = teacherApplicationRepository;
        this.siteVisitRepository = siteVisitRepository;
    }

    public Map<String, Object> getDashboardStats() {
        Long tenantId = TenantContext.requireTenantId();
        Map<String, Object> stats = new LinkedHashMap<>();

        // User stats
        stats.put("totalUsers", userRepository.countByTenantId(tenantId));
        stats.put("totalStudents", userRepository.countByRoleAndTenantId(User.Role.STUDENT, tenantId));

        // Course stats
        stats.put("totalCourses", courseRepository.countByTenantId(tenantId));

        // Enrollment stats
        stats.put("totalEnrollments", enrollmentRepository.countByTenantId(tenantId));
        stats.put("activeEnrollments",
                enrollmentRepository.countByStatusAndTenantId(Enrollment.Status.ACTIVE, tenantId));

        // Payment stats
        stats.put("totalPayments", paymentRepository.countByTenantId(tenantId));
        stats.put("successPayments", paymentRepository.countByStatusAndTenantId(Payment.Status.SUCCESS, tenantId));
        stats.put("pendingPayments", paymentRepository.countByStatusAndTenantId(Payment.Status.PENDING, tenantId));

        // Attendance stats
        stats.put("totalAttendanceRecords", attendanceRepository.countByTenantId(tenantId));

        // Contact message stats
        stats.put("totalContactMessages", contactRepo.countByTenantId(tenantId));
        stats.put("unreadContactMessages", contactRepo.countByTenantIdAndStatus(tenantId, ContactMessage.Status.NEW));

        // Blog stats
        stats.put("totalBlogPosts", blogPostRepository.countByTenantId(tenantId));
        stats.put("publishedBlogPosts", blogPostRepository.countByTenantIdAndPublished(tenantId, true));

        // Demo booking stats
        stats.put("totalDemoBookings", demoBookingRepository.countByTenantId(tenantId));
        stats.put("pendingDemoBookings",
                demoBookingRepository.countByTenantIdAndStatus(tenantId, DemoBooking.Status.PENDING));

        // Teacher application stats
        stats.put("totalTeacherApplications", teacherApplicationRepository.countByTenantId(tenantId));
        stats.put("newTeacherApplications",
                teacherApplicationRepository.countByTenantIdAndStatus(tenantId, TeacherApplication.Status.NEW));

        // Visitor analytics
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        stats.put("pageViews24h", siteVisitRepository.countByVisitedAtAfter(now.minusHours(24)));
        stats.put("uniqueVisitors24h", siteVisitRepository.countUniqueSessionsAfter(now.minusHours(24)));
        stats.put("pageViews7d", siteVisitRepository.countByVisitedAtAfter(now.minusDays(7)));
        stats.put("uniqueVisitors7d", siteVisitRepository.countUniqueSessionsAfter(now.minusDays(7)));

        return stats;
    }
}
