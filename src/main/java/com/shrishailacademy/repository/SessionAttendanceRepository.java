package com.shrishailacademy.repository;

import com.shrishailacademy.model.SessionAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for SessionAttendance (class scheduling system)
 */
@Repository
public interface SessionAttendanceRepository extends JpaRepository<SessionAttendance, Long> {
    
    /**
     * Find attendance records by session
     */
    List<SessionAttendance> findBySession_IdAndTenantId(Long sessionId, Long tenantId);
    
    /**
     * Find attendance records by student
     */
    List<SessionAttendance> findByStudent_IdAndTenantId(Long studentId, Long tenantId);
    
    /**
     * Find specific attendance record
     */
        @Query("SELECT sa FROM SessionAttendance sa WHERE sa.session.id = :sessionId AND sa.student.id = :studentId AND sa.tenantId = :tenantId")
        Optional<SessionAttendance> findBySessionIdAndStudentIdAndTenantId(
            @Param("sessionId") Long sessionId,
            @Param("studentId") Long studentId,
            @Param("tenantId") Long tenantId);

        Optional<SessionAttendance> findByIdAndTenantId(Long id, Long tenantId);
    
    /**
     * Count present students for a session
     */
    @Query("SELECT COUNT(sa) FROM SessionAttendance sa WHERE sa.session.id = :sessionId AND sa.status = 'PRESENT'")
    Long countPresentBySessionId(@Param("sessionId") Long sessionId);
    
    /**
     * Get attendance summary for a course
     */
    @Query("SELECT sa FROM SessionAttendance sa " +
           "WHERE sa.session.schedule.course.id = :courseId " +
           "AND sa.tenantId = :tenantId " +
           "ORDER BY sa.session.sessionDate DESC")
    List<SessionAttendance> findByCourseIdAndTenantId(@Param("courseId") Long courseId, @Param("tenantId") Long tenantId);
}
