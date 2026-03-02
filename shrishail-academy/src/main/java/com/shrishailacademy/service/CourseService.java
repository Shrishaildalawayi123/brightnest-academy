package com.shrishailacademy.service;

import com.shrishailacademy.dto.CourseCreateRequest;
import com.shrishailacademy.dto.CourseUpdateRequest;
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
    public Course createCourse(CourseCreateRequest request) {
        if (courseRepository.existsByTitle(request.getTitle())) {
            throw new DuplicateResourceException("Course", "title", request.getTitle());
        }

        Course course = new Course();
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setDuration(request.getDuration());
        course.setIcon(request.getIcon());
        course.setColor(request.getColor());
        course.setFee(request.getFee());

        Course saved = courseRepository.save(course);
        log.info("Course created: id={} title='{}'", saved.getId(), saved.getTitle());
        return saved;
    }

    @CacheEvict(value = "courses", allEntries = true)
    public Course updateCourse(Long id, CourseUpdateRequest request) {
        Course course = getCourseById(id);

        if (request.getTitle() != null) {
            String newTitle = request.getTitle();
            if (!newTitle.equals(course.getTitle()) && courseRepository.existsByTitleAndIdNot(newTitle, id)) {
                throw new DuplicateResourceException("Course", "title", newTitle);
            }
            course.setTitle(newTitle);
        }
        if (request.getDescription() != null) {
            course.setDescription(request.getDescription());
        }
        if (request.getDuration() != null) {
            course.setDuration(request.getDuration());
        }
        if (request.getIcon() != null) {
            course.setIcon(request.getIcon());
        }
        if (request.getColor() != null) {
            course.setColor(request.getColor());
        }
        if (request.getFee() != null) {
            course.setFee(request.getFee());
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
