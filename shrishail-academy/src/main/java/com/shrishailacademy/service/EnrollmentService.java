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

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
            UserRepository userRepository,
            CourseRepository courseRepository,
            NotificationService notificationService) {
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public Enrollment enrollStudent(Long userId, Long courseId) {
        if (enrollmentRepository.existsByUserIdAndCourseIdAndStatusNot(userId, courseId, Enrollment.Status.CANCELLED)) {
            throw new DuplicateResourceException("Enrollment", "userId+courseId", userId + "+" + courseId);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));

        Enrollment enrollment = new Enrollment();
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
        return enrollmentRepository.findByUserId(userId);
    }

    public List<Enrollment> getAllEnrollments() {
        return enrollmentRepository.findAll();
    }

    public Page<Enrollment> getAllEnrollments(Pageable pageable) {
        return enrollmentRepository.findAll(pageable);
    }

    @Transactional
    public void cancelEnrollment(Long enrollmentId, Long userId, String role) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
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
