package com.shrishailacademy.service;

import com.shrishailacademy.dto.CourseCreateRequest;
import com.shrishailacademy.dto.CourseUpdateRequest;
import com.shrishailacademy.exception.DuplicateResourceException;
import com.shrishailacademy.model.Course;
import com.shrishailacademy.model.Tenant;
import com.shrishailacademy.repository.CourseRepository;
import com.shrishailacademy.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    private static final Long TENANT_ID = 1L;
    private static final String TENANT_KEY = "default";

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private TenantService tenantService;

    @InjectMocks
    private CourseService courseService;

    @BeforeEach
    void setTenantContext() {
        TenantContext.set(TENANT_ID, TENANT_KEY);
        lenient().when(tenantService.requireCurrentTenant())
                .thenReturn(new Tenant(TENANT_ID, TENANT_KEY, "Default Tenant"));
    }

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void createCourseShouldThrowWhenTitleAlreadyExists() {
        CourseCreateRequest request = new CourseCreateRequest();
        request.setTitle("Mathematics");
        request.setFee(new BigDecimal("3000.00"));

        when(courseRepository.existsByTitleAndTenantId("Mathematics", TENANT_ID)).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> courseService.createCourse(request));

        assertTrue(ex.getMessage().contains("Course already exists"));
        assertTrue(ex.getMessage().contains("Mathematics"));
        verify(courseRepository, never()).save(any(Course.class));
    }

    @Test
    void createCourseShouldSaveWhenTitleIsUnique() {
        CourseCreateRequest request = new CourseCreateRequest();
        request.setTitle("Mathematics");
        request.setFee(new BigDecimal("3000.00"));

        Course savedEntity = new Course();
        savedEntity.setId(10L);
        savedEntity.setTitle("Mathematics");
        savedEntity.setFee(new BigDecimal("3000.00"));

        when(courseRepository.existsByTitleAndTenantId("Mathematics", TENANT_ID)).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenReturn(savedEntity);

        Course saved = courseService.createCourse(request);

        assertSame(savedEntity, saved);
        verify(courseRepository, times(1)).save(any(Course.class));
    }

    @Test
    void createCourseShouldSanitizeStoredTextFields() {
        CourseCreateRequest request = new CourseCreateRequest();
        request.setTitle("<script>alert(1)</script> Mathematics");
        request.setDescription("<img src=x onerror=alert(1)>desc");
        request.setFee(new BigDecimal("3000.00"));

        when(courseRepository.existsByTitleAndTenantId(anyString(), eq(TENANT_ID))).thenReturn(false);
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> {
            Course c = invocation.getArgument(0);
            c.setId(11L);
            return c;
        });

        Course saved = courseService.createCourse(request);

        assertFalse(saved.getTitle().contains("<script>"));
        assertTrue(saved.getTitle().contains("&lt;script&gt;"));
        assertFalse(saved.getDescription().contains("<img"));
        assertTrue(saved.getDescription().contains("&lt;img"));
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

        CourseUpdateRequest patch = new CourseUpdateRequest();
        patch.setTitle("New Title");
        patch.setFee(new BigDecimal("1500.00"));

        when(courseRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(existing));
        when(courseRepository.existsByTitleAndTenantIdAndIdNot("New Title", TENANT_ID, 1L)).thenReturn(false);
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
        when(courseRepository.findByIdAndTenantId(99L, TENANT_ID)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> courseService.getCourseById(99L));

        assertTrue(ex.getMessage().contains("Course not found"));
        assertTrue(ex.getMessage().contains("99"));
    }

    @Test
    void deleteCourseShouldDeleteExistingCourse() {
        Course course = new Course();
        course.setId(7L);

        when(courseRepository.findByIdAndTenantId(7L, TENANT_ID)).thenReturn(Optional.of(course));

        courseService.deleteCourse(7L);

        verify(courseRepository).delete(course);
    }
}
