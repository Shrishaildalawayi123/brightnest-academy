package com.shrishailacademy.controller;

import com.shrishailacademy.dto.ApiResponse;
import com.shrishailacademy.dto.response.CourseResponse;
import com.shrishailacademy.model.Course;
import com.shrishailacademy.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public ResponseEntity<Page<CourseResponse>> getAllCourses(
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        Page<CourseResponse> courses = courseService.getAllCourses(pageable)
                .map(CourseResponse::fromEntity);
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getCourseById(@PathVariable Long id) {
        Course course = courseService.getCourseById(id);
        return ResponseEntity.ok(CourseResponse.fromEntity(course));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> createCourse(@Valid @RequestBody Course course) {
        Course created = courseService.createCourse(course);
        return ResponseEntity
                .ok(ApiResponse.success("Course created successfully", CourseResponse.fromEntity(created)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> updateCourse(@PathVariable Long id, @Valid @RequestBody Course course) {
        Course updated = courseService.updateCourse(id, course);
        return ResponseEntity
                .ok(ApiResponse.success("Course updated successfully", CourseResponse.fromEntity(updated)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deleteCourse(@PathVariable Long id) {
        courseService.deleteCourse(id);
        return ResponseEntity.ok(ApiResponse.success("Course deleted successfully"));
    }
}
