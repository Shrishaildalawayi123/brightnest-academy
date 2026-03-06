package com.shrishailacademy.service;

import com.shrishailacademy.exception.AccessDeniedException;
import com.shrishailacademy.exception.BusinessException;
import com.shrishailacademy.exception.DuplicateResourceException;
import com.shrishailacademy.exception.ResourceNotFoundException;
import com.shrishailacademy.model.Course;
import com.shrishailacademy.model.Enrollment;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.CourseRepository;
import com.shrishailacademy.repository.EnrollmentRepository;
import com.shrishailacademy.repository.UserRepository;
import com.shrishailacademy.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EnrollmentService {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);

    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final NotificationService notificationService;
    private final TenantService tenantService;

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
            UserRepository userRepository,
            CourseRepository courseRepository,
            NotificationService notificationService,
            TenantService tenantService) {
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.notificationService = notificationService;
        this.tenantService = tenantService;
    }

    @Transactional
    public Enrollment enrollStudent(Long userId, Long courseId) {
        Long tenantId = TenantContext.requireTenantId();
        if (enrollmentRepository.existsByUserIdAndCourseIdAndTenantIdAndStatusNot(userId, courseId, tenantId,
                Enrollment.Status.CANCELLED)) {
            throw new DuplicateResourceException("Enrollment", "userId+courseId", userId + "+" + courseId);
        }

        User user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        Course course = courseRepository.findByIdAndTenantId(courseId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        Enrollment enrollment = new Enrollment();
        enrollment.setTenant(tenantService.requireCurrentTenant());
        enrollment.setUser(user);
        enrollment.setCourse(course);
        enrollment.setStatus(Enrollment.Status.ACTIVE);

        Enrollment saved = enrollmentRepository.save(enrollment);
        log.info("ENROLLMENT_CREATED: user={} course='{}'", user.getEmail(), course.getTitle());

        try {
            notificationService.sendEnrollmentConfirmation(saved);
        } catch (Exception e) {
            log.error("Failed to send enrollment notification for user={}: {}", user.getEmail(), e.getMessage());
        }

        return saved;
    }

    public List<Enrollment> getStudentEnrollments(Long userId) {
        Long tenantId = TenantContext.requireTenantId();
        return enrollmentRepository.findByUserIdAndTenantIdWithDetails(userId, tenantId);
    }

    public List<Enrollment> getAllEnrollments() {
        Long tenantId = TenantContext.requireTenantId();
        return enrollmentRepository.findAllByTenantId(tenantId);
    }

    public Page<Enrollment> getAllEnrollments(Pageable pageable) {
        Long tenantId = TenantContext.requireTenantId();
        return enrollmentRepository.findAllByTenantIdWithDetails(tenantId, pageable);
    }

    @Transactional
    public void cancelEnrollment(Long enrollmentId, Long userId, String role) {
        Long tenantId = TenantContext.requireTenantId();
        Enrollment enrollment = enrollmentRepository.findByIdAndTenantId(enrollmentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", "id", enrollmentId));

        if (enrollment.getStatus() == Enrollment.Status.CANCELLED) {
            throw new BusinessException("Enrollment is already cancelled");
        }
        // Students can only cancel their own enrollments
        if (!User.Role.ADMIN.name().equals(role) && !enrollment.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You can only cancel your own enrollment");
        }

        enrollment.setStatus(Enrollment.Status.CANCELLED);
        enrollmentRepository.save(enrollment);
        log.info("ENROLLMENT_CANCELLED: id={} user={}", enrollmentId, enrollment.getUser().getEmail());
    }
}
