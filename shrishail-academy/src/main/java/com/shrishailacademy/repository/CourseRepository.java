package com.shrishailacademy.repository;

import com.shrishailacademy.model.Course;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Course Repository - Data Access Layer for Course entity
 */
@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    /**
     * Find course by title
     */
    Optional<Course> findByTitle(String title);

    @EntityGraph(attributePaths = "teacher")
    Optional<Course> findByTitleAndTenantId(String title, Long tenantId);

    @EntityGraph(attributePaths = "teacher")
    Optional<Course> findBySubjectKeyAndTenantId(String subjectKey, Long tenantId);

    /**
     * Find courses by title containing (search)
     */
    List<Course> findByTitleContainingIgnoreCase(String title);

    List<Course> findByTitleContainingIgnoreCaseAndTenantId(String title, Long tenantId);

    @EntityGraph(attributePaths = "teacher")
    List<Course> findAllByTenantId(Long tenantId);

    @EntityGraph(attributePaths = "teacher")
    Page<Course> findAllByTenantId(Long tenantId, Pageable pageable);

    @EntityGraph(attributePaths = "teacher")
    Optional<Course> findByIdAndTenantId(Long id, Long tenantId);

    /**
     * Check if course title already exists
     */
    boolean existsByTitle(String title);

    boolean existsByTitleAndTenantId(String title, Long tenantId);

    boolean existsBySubjectKeyAndTenantId(String subjectKey, Long tenantId);

    /**
     * Check if course title already exists for a different course.
     */
    boolean existsByTitleAndIdNot(String title, Long id);

    boolean existsByTitleAndTenantIdAndIdNot(String title, Long tenantId, Long id);

    boolean existsBySubjectKeyAndTenantIdAndIdNot(String subjectKey, Long tenantId, Long id);
}
