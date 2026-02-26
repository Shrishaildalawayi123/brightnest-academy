package com.shrishailacademy.controller;

import com.shrishailacademy.model.ContactMessage;
import com.shrishailacademy.model.DemoBooking;
import com.shrishailacademy.model.TeacherApplication;
import com.shrishailacademy.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin Dashboard Analytics API
 * Provides real-time stats for the admin dashboard
 */
@RestController
@RequestMapping("/api/admin/analytics")
@PreAuthorize("hasRole('ADMIN')")
public class AnalyticsController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private ContactMessageRepository contactRepo;

    @Autowired
    private BlogPostRepository blogPostRepository;

    @Autowired
    private DemoBookingRepository demoBookingRepository;

    @Autowired
    private TeacherApplicationRepository teacherApplicationRepository;

    /**
     * GET /api/admin/analytics/dashboard
     * Returns all key metrics for the admin dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // User stats
        long totalUsers = userRepository.count();
        long totalStudents = userRepository.countByRole(com.shrishailacademy.model.User.Role.STUDENT);
        stats.put("totalUsers", totalUsers);
        stats.put("totalStudents", totalStudents);

        // Course stats
        stats.put("totalCourses", courseRepository.count());

        // Enrollment stats
        long totalEnrollments = enrollmentRepository.count();
        long activeEnrollments = enrollmentRepository.countByStatus(
                com.shrishailacademy.model.Enrollment.Status.ACTIVE);
        stats.put("totalEnrollments", totalEnrollments);
        stats.put("activeEnrollments", activeEnrollments);

        // Payment stats
        long totalPayments = paymentRepository.count();
        long successPayments = paymentRepository.countByStatus(
                com.shrishailacademy.model.Payment.Status.SUCCESS);
        long pendingPayments = paymentRepository.countByStatus(
                com.shrishailacademy.model.Payment.Status.PENDING);
        stats.put("totalPayments", totalPayments);
        stats.put("successPayments", successPayments);
        stats.put("pendingPayments", pendingPayments);

        // Attendance stats
        stats.put("totalAttendanceRecords", attendanceRepository.count());

        // Contact message stats
        long unreadMessages = contactRepo.countByStatus(ContactMessage.Status.NEW);
        stats.put("totalContactMessages", contactRepo.count());
        stats.put("unreadContactMessages", unreadMessages);

        // Blog stats
        stats.put("totalBlogPosts", blogPostRepository.count());
        stats.put("publishedBlogPosts", blogPostRepository.countByPublished(true));

        // Demo booking stats
        stats.put("totalDemoBookings", demoBookingRepository.count());
        stats.put("pendingDemoBookings", demoBookingRepository.countByStatus(DemoBooking.Status.PENDING));

        // Teacher application stats
        stats.put("totalTeacherApplications", teacherApplicationRepository.count());
        stats.put("newTeacherApplications", teacherApplicationRepository.countByStatus(TeacherApplication.Status.NEW));

        return ResponseEntity.ok(stats);
    }
}
