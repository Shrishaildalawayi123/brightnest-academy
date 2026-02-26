package com.shrishailacademy.repository;

import com.shrishailacademy.model.Course;
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

    /**
     * Find courses by title containing (search)
     */
    List<Course> findByTitleContainingIgnoreCase(String title);

    /**
     * Check if course title already exists
     */
    boolean existsByTitle(String title);
}
