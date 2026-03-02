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

    List<Attendance> findByCourseId(Long courseId);

    List<Attendance> findByUserIdAndCourseId(Long userId, Long courseId);

    List<Attendance> findByCourseIdAndAttendanceDate(Long courseId, LocalDate date);

    Optional<Attendance> findByUserIdAndCourseIdAndAttendanceDate(Long userId, Long courseId, LocalDate date);

    List<Attendance> findByCourseIdAndAttendanceDateAndUserIdIn(Long courseId, LocalDate date, Collection<Long> userIds);

    boolean existsByUserIdAndCourseIdAndAttendanceDate(Long userId, Long courseId, LocalDate date);

    @Query("SELECT a.status, COUNT(a) FROM Attendance a WHERE a.user.id = :userId AND a.course.id = :courseId GROUP BY a.status")
    List<Object[]> getAttendanceSummary(@Param("userId") Long userId, @Param("courseId") Long courseId);

    @Query("SELECT a.status, COUNT(a) FROM Attendance a WHERE a.course.id = :courseId GROUP BY a.status")
    List<Object[]> getCourseAttendanceSummary(@Param("courseId") Long courseId);

    long countByUserIdAndCourseIdAndStatus(Long userId, Long courseId, Attendance.Status status);

    List<Attendance> findByUserIdAndAttendanceDateBetween(Long userId, LocalDate start, LocalDate end);

    List<Attendance> findByCourseIdAndAttendanceDateBetween(Long courseId, LocalDate start, LocalDate end);
}
