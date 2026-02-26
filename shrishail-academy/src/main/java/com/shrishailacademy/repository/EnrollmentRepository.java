package com.shrishailacademy.repository;

import com.shrishailacademy.model.Enrollment;
import com.shrishailacademy.model.User;
import com.shrishailacademy.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Enrollment Repository - Data Access Layer for Enrollment entity
 */
@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    /**
     * Find all enrollments for a specific user
     */
    List<Enrollment> findByUser(User user);

    /**
     * Find all enrollments for a specific user by user ID
     */
    List<Enrollment> findByUserId(Long userId);

    /**
     * Find all enrollments for a specific course
     */
    List<Enrollment> findByCourse(Course course);

    /**
     * Find all enrollments for a specific course by course ID
     */
    List<Enrollment> findByCourseId(Long courseId);

    /**
     * Check if user is already enrolled in a course
     */
    boolean existsByUserIdAndCourseId(Long userId, Long courseId);

    /**
     * Check if user has an active (non-cancelled) enrollment in a course
     */
    boolean existsByUserIdAndCourseIdAndStatusNot(Long userId, Long courseId, Enrollment.Status status);

    /**
     * Find specific enrollment by user and course
     */
    Optional<Enrollment> findByUserIdAndCourseId(Long userId, Long courseId);

    /**
     * Find all active enrollments for a user
     */
    List<Enrollment> findByUserIdAndStatus(Long userId, Enrollment.Status status);

    /**
     * Count enrollments by status
     */
    long countByStatus(Enrollment.Status status);

    /**
     * Count enrollments for a course
     */
    long countByCourseId(Long courseId);

    /**
     * Get enrollment count per course
     */
    @Query("SELECT c.id, c.title, COUNT(e.id) FROM Course c LEFT JOIN c.enrollments e GROUP BY c.id, c.title")
    List<Object[]> getEnrollmentCountPerCourse();
}
