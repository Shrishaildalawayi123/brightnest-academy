package com.shrishailacademy.service;

import com.shrishailacademy.model.Course;
import com.shrishailacademy.model.Enrollment;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.CourseRepository;
import com.shrishailacademy.repository.EnrollmentRepository;
import com.shrishailacademy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EnrollmentService {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private NotificationService notificationService;

    public Enrollment enrollStudent(Long userId, Long courseId) {
        if (enrollmentRepository.existsByUserIdAndCourseIdAndStatusNot(userId, courseId, Enrollment.Status.CANCELLED)) {
            throw new RuntimeException("Student already enrolled in this course");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Enrollment enrollment = new Enrollment();
        enrollment.setUser(user);
        enrollment.setCourse(course);
        enrollment.setStatus(Enrollment.Status.ACTIVE);

        Enrollment saved = enrollmentRepository.save(enrollment);

        // Send WhatsApp enrollment confirmation
        notificationService.sendEnrollmentConfirmation(saved);

        return saved;
    }

    public List<Enrollment> getStudentEnrollments(Long userId) {
        return enrollmentRepository.findByUserId(userId);
    }

    public List<Enrollment> getAllEnrollments() {
        return enrollmentRepository.findAll();
    }

    public void cancelEnrollment(Long enrollmentId, Long userId, String role) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));
        // Already cancelled check
        if (enrollment.getStatus() == Enrollment.Status.CANCELLED) {
            throw new RuntimeException("Enrollment is already cancelled");
        }
        // Students can only cancel their own enrollments
        if (!User.Role.ADMIN.name().equals(role) && !enrollment.getUser().getId().equals(userId)) {
            throw new RuntimeException("You can only cancel your own enrollment");
        }
        enrollment.setStatus(Enrollment.Status.CANCELLED);
        enrollmentRepository.save(enrollment);
    }
}
