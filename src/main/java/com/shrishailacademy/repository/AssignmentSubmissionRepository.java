package com.shrishailacademy.repository;

import com.shrishailacademy.model.AssignmentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Long> {
    
    /**
     * Find submission by ID and tenant (for security)
     */
    Optional<AssignmentSubmission> findByIdAndTenantId(Long id, Long tenantId);
    
    /**
     * Find all submissions by assignment and tenant
     */
    List<AssignmentSubmission> findByAssignment_IdAndTenantId(Long assignmentId, Long tenantId);
    
    /**
     * Find all submissions by student and tenant
     */
    List<AssignmentSubmission> findByStudent_IdAndTenantId(Long studentId, Long tenantId);
    
    /**
     * Find ungraded submissions by tenant
     */
    List<AssignmentSubmission> findByTenantIdAndGradedAtIsNull(Long tenantId);

    List<AssignmentSubmission> findByTenantIdAndGradedAtIsNullOrderBySubmittedAtAsc(Long tenantId);

    List<AssignmentSubmission> findByTenantIdAndAssignment_Teacher_IdAndGradedAtIsNullOrderBySubmittedAtAsc(
            Long tenantId,
            Long teacherId);
    
    /**
     * Find submission by assignment and student
     */
    @Query("SELECT s FROM AssignmentSubmission s WHERE s.assignment.id = :assignmentId AND s.student.id = :studentId")
    Optional<AssignmentSubmission> findByAssignmentIdAndStudentId(@Param("assignmentId") Long assignmentId, @Param("studentId") Long studentId);

    @Query("SELECT s FROM AssignmentSubmission s WHERE s.assignment.id = :assignmentId AND s.student.id = :studentId AND s.tenantId = :tenantId")
    Optional<AssignmentSubmission> findByAssignmentIdAndStudentIdAndTenantId(
           @Param("assignmentId") Long assignmentId,
           @Param("studentId") Long studentId,
           @Param("tenantId") Long tenantId);
    
    /**
     * Find all submissions for an assignment
     */
    List<AssignmentSubmission> findByAssignmentIdOrderBySubmittedAtDesc(Long assignmentId);
    
    /**
     * Find all submissions by a student
     */
    List<AssignmentSubmission> findByStudentIdOrderBySubmittedAtDesc(Long studentId);
    
    /**
     * Find submission by student and assignment
     */
    Optional<AssignmentSubmission> findByStudentIdAndAssignmentId(Long studentId, Long assignmentId);
    
    /**
     * Find ungraded submissions for an assignment
     */
    @Query("SELECT s FROM AssignmentSubmission s WHERE " +
           "s.assignment.id = :assignmentId AND " +
           "s.gradedAt IS NULL " +
           "ORDER BY s.submittedAt ASC")
    List<AssignmentSubmission> findUngradedSubmissions(@Param("assignmentId") Long assignmentId);

    @Query("SELECT s FROM AssignmentSubmission s WHERE " +
           "s.assignment.id = :assignmentId AND " +
           "s.tenantId = :tenantId AND " +
           "s.gradedAt IS NULL " +
           "ORDER BY s.submittedAt ASC")
    List<AssignmentSubmission> findUngradedSubmissionsByTenant(
            @Param("assignmentId") Long assignmentId,
            @Param("tenantId") Long tenantId);
    
    /**
     * Find late submissions
     */
    List<AssignmentSubmission> findByAssignmentIdAndIsLateTrue(Long assignmentId);
    
    /**
     * Count submissions for an assignment
     */
    long countByAssignmentId(Long assignmentId);
    
    /**
     * Count graded submissions for an assignment
     */
    long countByAssignmentIdAndGradedAtIsNotNull(Long assignmentId);
    
    /**
     * Check if student has submitted assignment
     */
    boolean existsByStudentIdAndAssignmentId(Long studentId, Long assignmentId);
    
    /**
     * Get average score for an assignment
     */
    @Query("SELECT AVG(s.score) FROM AssignmentSubmission s WHERE " +
           "s.assignment.id = :assignmentId AND " +
           "s.score IS NOT NULL")
    Double getAverageScore(@Param("assignmentId") Long assignmentId);
}
