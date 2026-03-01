package com.shrishailacademy.service;

import com.shrishailacademy.exception.DuplicateResourceException;
import com.shrishailacademy.exception.ResourceNotFoundException;
import com.shrishailacademy.model.Course;
import com.shrishailacademy.repository.CourseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseService {

    private static final Logger log = LoggerFactory.getLogger(CourseService.class);

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Cacheable(value = "courses")
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public Page<Course> getAllCourses(Pageable pageable) {
        return courseRepository.findAll(pageable);
    }

    @Cacheable(value = "courses", key = "#id")
    public Course getCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", id));
    }

    @CacheEvict(value = "courses", allEntries = true)
    public Course createCourse(Course course) {
        if (courseRepository.existsByTitle(course.getTitle())) {
            throw new DuplicateResourceException("Course", "title", course.getTitle());
        }
        Course saved = courseRepository.save(course);
        log.info("Course created: id={} title='{}'", saved.getId(), saved.getTitle());
        return saved;
    }

    @CacheEvict(value = "courses", allEntries = true)
    public Course updateCourse(Long id, Course courseDetails) {
        Course course = getCourseById(id);

        if (courseDetails.getTitle() != null) {
            course.setTitle(courseDetails.getTitle());
        }
        if (courseDetails.getDescription() != null) {
            course.setDescription(courseDetails.getDescription());
        }
        if (courseDetails.getDuration() != null) {
            course.setDuration(courseDetails.getDuration());
        }
        if (courseDetails.getIcon() != null) {
            course.setIcon(courseDetails.getIcon());
        }
        if (courseDetails.getColor() != null) {
            course.setColor(courseDetails.getColor());
        }
        if (courseDetails.getFee() != null) {
            course.setFee(courseDetails.getFee());
        }

        Course updated = courseRepository.save(course);
        log.info("Course updated: id={} title='{}'", updated.getId(), updated.getTitle());
        return updated;
    }

    @CacheEvict(value = "courses", allEntries = true)
    public void deleteCourse(Long id) {
        Course course = getCourseById(id);
        courseRepository.delete(course);
        log.info("Course deleted: id={} title='{}'", id, course.getTitle());
    }
}
