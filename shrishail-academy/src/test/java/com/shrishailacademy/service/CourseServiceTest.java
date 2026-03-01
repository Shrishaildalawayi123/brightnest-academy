package com.shrishailacademy.service;

import com.shrishailacademy.model.Course;
import com.shrishailacademy.repository.CourseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseService courseService;

    @Test
    void createCourseShouldThrowWhenTitleAlreadyExists() {
        Course course = new Course();
        course.setTitle("Mathematics");

        when(courseRepository.existsByTitle("Mathematics")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> courseService.createCourse(course));

        assertTrue(ex.getMessage().contains("Course already exists"));
        assertTrue(ex.getMessage().contains("Mathematics"));
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void createCourseShouldSaveWhenTitleIsUnique() {
        Course course = new Course();
        course.setTitle("Mathematics");
        course.setFee(new BigDecimal("3000.00"));

        when(courseRepository.existsByTitle("Mathematics")).thenReturn(false);
        when(courseRepository.save(course)).thenReturn(course);

        Course saved = courseService.createCourse(course);

        assertSame(course, saved);
        verify(courseRepository).save(course);
    }

    @Test
    void updateCourseShouldPatchOnlyProvidedFields() {
        Course existing = new Course();
        existing.setId(1L);
        existing.setTitle("Old Title");
        existing.setDescription("Old Description");
        existing.setDuration("6 months");
        existing.setFee(new BigDecimal("1000.00"));
        existing.setColor("#123456");

        Course patch = new Course();
        patch.setTitle("New Title");
        patch.setFee(new BigDecimal("1500.00"));
        patch.setDescription(null);
        patch.setDuration(null);

        when(courseRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Course updated = courseService.updateCourse(1L, patch);

        assertEquals("New Title", updated.getTitle());
        assertEquals(new BigDecimal("1500.00"), updated.getFee());
        assertEquals("Old Description", updated.getDescription());
        assertEquals("6 months", updated.getDuration());
        assertEquals("#123456", updated.getColor());
        verify(courseRepository).save(existing);
    }

    @Test
    void getCourseByIdShouldThrowWhenCourseMissing() {
        when(courseRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> courseService.getCourseById(99L));

        assertTrue(ex.getMessage().contains("Course not found"));
        assertTrue(ex.getMessage().contains("99"));
    }

    @Test
    void deleteCourseShouldDeleteExistingCourse() {
        Course course = new Course();
        course.setId(7L);

        when(courseRepository.findById(7L)).thenReturn(Optional.of(course));

        courseService.deleteCourse(7L);

        verify(courseRepository).delete(course);
    }
}
