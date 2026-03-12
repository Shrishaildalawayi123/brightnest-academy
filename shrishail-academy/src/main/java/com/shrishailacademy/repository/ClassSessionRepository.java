package com.shrishailacademy.repository;

import com.shrishailacademy.model.ClassSession;
import com.shrishailacademy.model.ClassSession.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {
    
    /**
     * Find session by ID and tenant (for security)
     */
    Optional<ClassSession> findByIdAndTenantId(Long id, Long tenantId);
    
    /**
     * Find all sessions for a schedule
     */
    List<ClassSession> findByScheduleIdOrderBySessionDateAsc(Long scheduleId);
    
    /**
     * Find all sessions for a date range
     */
    List<ClassSession> findByTenantIdAndSessionDateBetweenOrderBySessionDateAsc(
        Long tenantId, LocalDate startDate, LocalDate endDate
    );
    
    /**
     * Find sessions for a specific date
     */
    List<ClassSession> findByTenantIdAndSessionDate(Long tenantId, LocalDate sessionDate);
    
    /**
     * Find sessions by status
     */
    List<ClassSession> findByTenantIdAndStatusOrderBySessionDateAsc(Long tenantId, SessionStatus status);
    
    /**
     * Find upcoming sessions (scheduled or in-progress)
     */
    @Query("SELECT cs FROM ClassSession cs WHERE " +
           "cs.tenantId = :tenantId AND " +
           "cs.sessionDate >= :fromDate AND " +
           "cs.status IN ('SCHEDULED', 'IN_PROGRESS') " +
           "ORDER BY cs.sessionDate ASC")
    List<ClassSession> findUpcomingSessions(@Param("tenantId") Long tenantId, @Param("fromDate") LocalDate fromDate);
    
    /**
     * Find sessions needing attendance marking
     */
    @Query("SELECT cs FROM ClassSession cs WHERE " +
           "cs.tenantId = :tenantId AND " +
           "cs.sessionDate < :today AND " +
           "cs.attendanceMarked = false AND " +
           "cs.status = 'COMPLETED'")
    List<ClassSession> findSessionsNeedingAttendance(@Param("tenantId") Long tenantId, @Param("today") LocalDate today);
    
    /**
     * Check if session exists for schedule and date
     */
    boolean existsByScheduleIdAndSessionDate(Long scheduleId, LocalDate sessionDate);
    
    /**
     * Count sessions for a schedule
     */
    long countByScheduleId(Long scheduleId);
}
