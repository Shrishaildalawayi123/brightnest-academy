package com.shrishailacademy.service;

import com.shrishailacademy.model.Course;
import com.shrishailacademy.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseService {

    @Autowired
    private CourseRepository courseRepository;

    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    public Course getCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + id));
    }

    public Course createCourse(Course course) {
        if (courseRepository.existsByTitle(course.getTitle())) {
            throw new RuntimeException("Course with this title already exists");
        }
        return courseRepository.save(course);
    }

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

        return courseRepository.save(course);
    }

    public void deleteCourse(Long id) {
        Course course = getCourseById(id);
        courseRepository.delete(course);
    }
}
