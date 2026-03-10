package com.shrishailacademy.repository;

import com.shrishailacademy.model.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    
    /**
     * Find assignment by ID and tenant (for security)
     */
    Optional<Assignment> findByIdAndTenantId(Long id, Long tenantId);
    
    /**
     * Find all published assignments for a course
     */
    List<Assignment> findByCourseIdAndIsPublishedTrueOrderByDueDateDesc(Long courseId);
    
    /**
     * Find all assignments for a course (teacher view)
     */
    List<Assignment> findByCourseIdOrderByDueDateDesc(Long courseId);
    
    /**
     * Find assignments created by a teacher
     */
    List<Assignment> findByTeacherIdOrderByDueDateDesc(Long teacherId);
    
    /**
     * Find upcoming assignments (not yet due)
     */
    @Query("SELECT a FROM Assignment a WHERE " +
           "a.tenantId = :tenantId AND " +
           "a.isPublished = true AND " +
           "a.dueDate > :now " +
           "ORDER BY a.dueDate ASC")
    List<Assignment> findUpcomingAssignments(@Param("tenantId") Long tenantId, @Param("now") LocalDateTime now);
    
    /**
     * Find all assignments by tenant
     */
    List<Assignment> findByTenantId(Long tenantId);
    
    /**
     * Find assignments by course and tenant
     */
    List<Assignment> findByCourse_IdAndTenantId(Long courseId, Long tenantId);
    
    /**
     * Find overdue assignments
     */
    @Query("SELECT a FROM Assignment a WHERE " +
           "a.tenantId = :tenantId AND " +
           "a.isPublished = true AND " +
           "a.dueDate < :now " +
           "ORDER BY a.dueDate DESC")
    List<Assignment> findOverdueAssignments(@Param("tenantId") Long tenantId, @Param("now") LocalDateTime now);
    
    /**
     * Count published assignments for a course
     */
    long countByCourseIdAndIsPublishedTrue(Long courseId);
}
