package com.shrishailacademy.repository;

import com.shrishailacademy.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByUserId(Long userId);

    List<Attendance> findByUserIdAndTenantId(Long userId, Long tenantId);

    List<Attendance> findByCourseId(Long courseId);

    List<Attendance> findByCourseIdAndTenantId(Long courseId, Long tenantId);

    List<Attendance> findByUserIdAndCourseId(Long userId, Long courseId);

    List<Attendance> findByUserIdAndCourseIdAndTenantId(Long userId, Long courseId, Long tenantId);

    List<Attendance> findByCourseIdAndAttendanceDate(Long courseId, LocalDate date);

    List<Attendance> findByCourseIdAndAttendanceDateAndTenantId(Long courseId, LocalDate date, Long tenantId);

    Optional<Attendance> findByUserIdAndCourseIdAndAttendanceDate(Long userId, Long courseId, LocalDate date);

    Optional<Attendance> findByUserIdAndCourseIdAndAttendanceDateAndTenantId(Long userId, Long courseId, LocalDate date,
            Long tenantId);

    List<Attendance> findByCourseIdAndAttendanceDateAndUserIdIn(Long courseId, LocalDate date,
            Collection<Long> userIds);

    List<Attendance> findByCourseIdAndAttendanceDateAndUserIdInAndTenantId(Long courseId, LocalDate date,
            Collection<Long> userIds, Long tenantId);

    boolean existsByUserIdAndCourseIdAndAttendanceDate(Long userId, Long courseId, LocalDate date);

    boolean existsByUserIdAndCourseIdAndAttendanceDateAndTenantId(Long userId, Long courseId, LocalDate date,
            Long tenantId);

    @Query("SELECT a.status, COUNT(a) FROM Attendance a WHERE a.tenant.id = :tenantId AND a.user.id = :userId AND a.course.id = :courseId GROUP BY a.status")
    List<Object[]> getAttendanceSummary(@Param("tenantId") Long tenantId, @Param("userId") Long userId,
            @Param("courseId") Long courseId);

    @Query("SELECT a.status, COUNT(a) FROM Attendance a WHERE a.tenant.id = :tenantId AND a.course.id = :courseId GROUP BY a.status")
    List<Object[]> getCourseAttendanceSummary(@Param("tenantId") Long tenantId, @Param("courseId") Long courseId);

    long countByUserIdAndCourseIdAndStatus(Long userId, Long courseId, Attendance.Status status);

    List<Attendance> findByUserIdAndAttendanceDateBetween(Long userId, LocalDate start, LocalDate end);

    List<Attendance> findByUserIdAndAttendanceDateBetweenAndTenantId(Long userId, LocalDate start, LocalDate end,
            Long tenantId);

    List<Attendance> findByCourseIdAndAttendanceDateBetween(Long courseId, LocalDate start, LocalDate end);

    List<Attendance> findByCourseIdAndAttendanceDateBetweenAndTenantId(Long courseId, LocalDate start, LocalDate end,
            Long tenantId);

    List<Attendance> findAllByTenantId(Long tenantId);
}
