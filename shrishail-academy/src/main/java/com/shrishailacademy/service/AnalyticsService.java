package com.shrishailacademy.service;

import com.shrishailacademy.model.*;
import com.shrishailacademy.repository.*;
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
        Map<String, Object> stats = new LinkedHashMap<>();

        // User stats
        stats.put("totalUsers", userRepository.count());
        stats.put("totalStudents", userRepository.countByRole(User.Role.STUDENT));

        // Course stats
        stats.put("totalCourses", courseRepository.count());

        // Enrollment stats
        stats.put("totalEnrollments", enrollmentRepository.count());
        stats.put("activeEnrollments", enrollmentRepository.countByStatus(Enrollment.Status.ACTIVE));

        // Payment stats
        stats.put("totalPayments", paymentRepository.count());
        stats.put("successPayments", paymentRepository.countByStatus(Payment.Status.SUCCESS));
        stats.put("pendingPayments", paymentRepository.countByStatus(Payment.Status.PENDING));

        // Attendance stats
        stats.put("totalAttendanceRecords", attendanceRepository.count());

        // Contact message stats
        stats.put("totalContactMessages", contactRepo.count());
        stats.put("unreadContactMessages", contactRepo.countByStatus(ContactMessage.Status.NEW));

        // Blog stats
        stats.put("totalBlogPosts", blogPostRepository.count());
        stats.put("publishedBlogPosts", blogPostRepository.countByPublished(true));

        // Demo booking stats
        stats.put("totalDemoBookings", demoBookingRepository.count());
        stats.put("pendingDemoBookings", demoBookingRepository.countByStatus(DemoBooking.Status.PENDING));

        // Teacher application stats
        stats.put("totalTeacherApplications", teacherApplicationRepository.count());
        stats.put("newTeacherApplications",
                teacherApplicationRepository.countByStatus(TeacherApplication.Status.NEW));

        // Visitor analytics
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        stats.put("pageViews24h", siteVisitRepository.countByVisitedAtAfter(now.minusHours(24)));
        stats.put("uniqueVisitors24h", siteVisitRepository.countUniqueSessionsAfter(now.minusHours(24)));
        stats.put("pageViews7d", siteVisitRepository.countByVisitedAtAfter(now.minusDays(7)));
        stats.put("uniqueVisitors7d", siteVisitRepository.countUniqueSessionsAfter(now.minusDays(7)));

        return stats;
    }
}
