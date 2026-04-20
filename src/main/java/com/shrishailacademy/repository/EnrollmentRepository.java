package com.shrishailacademy.repository;

import com.shrishailacademy.dto.response.StudentProgressDto;
import com.shrishailacademy.model.Enrollment;
import com.shrishailacademy.model.User;
import com.shrishailacademy.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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

        List<Enrollment> findByUserIdAndTenantId(Long userId, Long tenantId);

        @Query("SELECT e FROM Enrollment e JOIN FETCH e.user JOIN FETCH e.course WHERE e.user.id = :userId AND e.tenant.id = :tenantId")
        List<Enrollment> findByUserIdAndTenantIdWithDetails(@Param("userId") Long userId,
                        @Param("tenantId") Long tenantId);

        /**
         * Find all enrollments for a specific course
         */
        List<Enrollment> findByCourse(Course course);

        /**
         * Find all enrollments for a specific course by course ID
         */
        List<Enrollment> findByCourseId(Long courseId);

        List<Enrollment> findByCourseIdAndTenantId(Long courseId, Long tenantId);

        List<Enrollment> findAllByTenantId(Long tenantId);

        Page<Enrollment> findAllByTenantId(Long tenantId, Pageable pageable);

        @Query(value = "SELECT e FROM Enrollment e JOIN FETCH e.user JOIN FETCH e.course WHERE e.tenant.id = :tenantId", countQuery = "SELECT COUNT(e) FROM Enrollment e WHERE e.tenant.id = :tenantId")
        Page<Enrollment> findAllByTenantIdWithDetails(@Param("tenantId") Long tenantId, Pageable pageable);

        /**
         * Check if user is already enrolled in a course
         */
        boolean existsByUserIdAndCourseId(Long userId, Long courseId);

        boolean existsByUserIdAndCourseIdAndTenantId(Long userId, Long courseId, Long tenantId);

        /**
         * Check if user has an active (non-cancelled) enrollment in a course
         */
        boolean existsByUserIdAndCourseIdAndStatusNot(Long userId, Long courseId, Enrollment.Status status);

        boolean existsByUserIdAndCourseIdAndTenantIdAndStatusNot(Long userId, Long courseId, Long tenantId,
                        Enrollment.Status status);

        /**
         * Batch lookup of active (non-cancelled) enrollments for a course.
         */
        @Query("SELECT e.user.id FROM Enrollment e WHERE e.tenant.id = :tenantId AND e.course.id = :courseId AND e.status <> :excludedStatus AND e.user.id IN :userIds")
        Set<Long> findActiveUserIdsByCourseIdAndUserIdIn(
                        @Param("tenantId") Long tenantId,
                        @Param("courseId") Long courseId,
                        @Param("userIds") Set<Long> userIds,
                        @Param("excludedStatus") Enrollment.Status excludedStatus);

        /**
         * Find specific enrollment by user and course
         */
        Optional<Enrollment> findByUserIdAndCourseId(Long userId, Long courseId);

        Optional<Enrollment> findByUserIdAndCourseIdAndTenantId(Long userId, Long courseId, Long tenantId);

        Optional<Enrollment> findByIdAndTenantId(Long id, Long tenantId);

        /**
         * Find all active enrollments for a user
         */
        List<Enrollment> findByUserIdAndStatus(Long userId, Enrollment.Status status);

        /**
         * Count enrollments by status
         */
        long countByStatus(Enrollment.Status status);

        long countByStatusAndTenantId(Enrollment.Status status, Long tenantId);

        long countByTenantId(Long tenantId);

        /**
         * Count enrollments for a course
         */
        long countByCourseId(Long courseId);

        long countByCourseIdAndTenantId(Long courseId, Long tenantId);

        /**
         * Get enrollment count per course
         */
        @Query("SELECT c.id, c.title, COUNT(e.id) FROM Course c LEFT JOIN c.enrollments e GROUP BY c.id, c.title")
        List<Object[]> getEnrollmentCountPerCourse();

                                @Query("""
                                                                                                SELECT new com.shrishailacademy.dto.response.StudentProgressDto(
                                                                                                                                c.id,
                                                                                                                                c.title,
                                                                                                                                (SELECT COUNT(cs.id)
                                                                                                                                 FROM ClassSession cs
                                                                                                                                 WHERE cs.tenantId = :tenantId
                                                                                                                                         AND cs.schedule.course.id = c.id),
                                                                                                                                (SELECT COUNT(sa.id)
                                                                                                                                 FROM SessionAttendance sa
                                                                                                                                 WHERE sa.tenantId = :tenantId
                                                                                                                                         AND sa.student.id = :userId
                                                                                                                                         AND sa.session.schedule.course.id = c.id
                                                                                                                                         AND sa.status <> com.shrishailacademy.model.SessionAttendance.AttendanceStatus.ABSENT),
                                                                                                                                (SELECT CASE
                                                                                                                                                                 WHEN COUNT(cs2.id) = 0 THEN 0.0
                                                                                                                                                                 ELSE (100.0 * (SELECT COUNT(sa2.id)
                                                                                                                                                                                                                                 FROM SessionAttendance sa2
                                                                                                                                                                                                                                 WHERE sa2.tenantId = :tenantId
                                                                                                                                                                                                                                         AND sa2.student.id = :userId
                                                                                                                                                                                                                                         AND sa2.session.schedule.course.id = c.id
                                                                                                                                                                                                                                         AND sa2.status <> com.shrishailacademy.model.SessionAttendance.AttendanceStatus.ABSENT)
                                                                                                                                                                                         / COUNT(cs2.id))
                                                                                                                                                                 END
                                                                                                                                 FROM ClassSession cs2
                                                                                                                                 WHERE cs2.tenantId = :tenantId
                                                                                                                                         AND cs2.schedule.course.id = c.id),
                                                                                                                                (SELECT COUNT(a.id)
                                                                                                                                 FROM Assignment a
                                                                                                                                 WHERE a.tenantId = :tenantId
                                                                                                                                         AND a.course.id = c.id),
                                                                                                                                (SELECT COUNT(asub.id)
                                                                                                                                 FROM AssignmentSubmission asub
                                                                                                                                 WHERE asub.tenantId = :tenantId
                                                                                                                                         AND asub.student.id = :userId
                                                                                                                                         AND asub.assignment.course.id = c.id),
                                                                                                                                (SELECT AVG(CAST(asub2.score AS double))
                                                                                                                                 FROM AssignmentSubmission asub2
                                                                                                                                 WHERE asub2.tenantId = :tenantId
                                                                                                                                         AND asub2.student.id = :userId
                                                                                                                                         AND asub2.assignment.course.id = c.id
                                                                                                                                         AND asub2.score IS NOT NULL)
                                                                                                )
                                                                                                FROM Enrollment e
                                                                                                JOIN e.course c
                                                                                                WHERE e.tenant.id = :tenantId
                                                                                                        AND e.user.id = :userId
                                                                                                        AND e.status = com.shrishailacademy.model.Enrollment.Status.ACTIVE
                                                                                                ORDER BY c.title ASC
                                                                                                """)
                                List<StudentProgressDto> findStudentProgressByUserIdAndTenantId(@Param("userId") Long userId,
                                                                                                @Param("tenantId") Long tenantId);
}
