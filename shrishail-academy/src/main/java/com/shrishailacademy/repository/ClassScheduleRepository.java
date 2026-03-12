package com.shrishailacademy.repository;

import com.shrishailacademy.model.ClassSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClassScheduleRepository  extends JpaRepository<ClassSchedule, Long> {
    
    /**
     * Find all active schedules for a tenant
     */
    List<ClassSchedule> findByTenantIdAndIsActiveTrueOrderByDayOfWeekAsc(Long tenantId);
    
    /**
     * Find all schedules for a specific course
     */
    List<ClassSchedule> findByCourseIdOrderByDayOfWeekAsc(Long courseId);
    
    /**
     * Find all schedules for a specific teacher
     */
    List<ClassSchedule> findByTeacherIdOrderByDayOfWeekAsc(Long teacherId);
    
    /**
     * Find schedule by ID and tenant (for security)
     */
    Optional<ClassSchedule> findByIdAndTenantId(Long id, Long tenantId);
    
    /**
     * Find schedules for a specific day of week
     */
    List<ClassSchedule> findByTenantIdAndDayOfWeekAndIsActiveTrue(Long tenantId, DayOfWeek dayOfWeek);
    
    /**
     * Find conflicting schedules (same teacher, same day, overlapping time)
     */
    @Query("SELECT cs FROM ClassSchedule cs WHERE " +
           "cs.teacher.id = :teacherId AND " +
           "cs.dayOfWeek = :dayOfWeek AND " +
           "cs.isActive = true AND " +
           "((cs.startTime < :endTime AND cs.endTime > :startTime) OR " +
           "(cs.startTime >= :startTime AND cs.startTime < :endTime))")
    List<ClassSchedule> findConflictingSchedules(
        @Param("teacherId") Long teacherId,
        @Param("dayOfWeek") DayOfWeek dayOfWeek,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime
    );
    
    /**
     * Count active schedules for a course
     */
    long countByCourseIdAndIsActiveTrue(Long courseId);
    
    /**
     * Count active schedules for a teacher
     */
    long countByTeacherIdAndIsActiveTrue(Long teacherId);
}
