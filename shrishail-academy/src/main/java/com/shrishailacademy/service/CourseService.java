package com.shrishailacademy.service;

import com.shrishailacademy.dto.CourseCreateRequest;
import com.shrishailacademy.dto.CourseUpdateRequest;
import com.shrishailacademy.exception.DuplicateResourceException;
import com.shrishailacademy.exception.ResourceNotFoundException;
import com.shrishailacademy.model.Course;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.CourseRepository;
import com.shrishailacademy.repository.UserRepository;
import com.shrishailacademy.tenant.TenantContext;
import com.shrishailacademy.util.InputSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class CourseService {

    private static final Logger log = LoggerFactory.getLogger(CourseService.class);

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final TenantService tenantService;

    public CourseService(CourseRepository courseRepository, UserRepository userRepository,
            TenantService tenantService) {
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.tenantService = tenantService;
    }

    @Cacheable(value = "courses", key = "'all:' + T(com.shrishailacademy.tenant.TenantContext).requireTenantId()")
    public List<Course> getAllCourses() {
        Long tenantId = TenantContext.requireTenantId();
        return courseRepository.findAllByTenantId(tenantId);
    }

    public Page<Course> getAllCourses(Pageable pageable) {
        Long tenantId = TenantContext.requireTenantId();
        return courseRepository.findAllByTenantId(tenantId, pageable);
    }

    @Cacheable(value = "courses", key = "T(com.shrishailacademy.tenant.TenantContext).requireTenantId() + ':' + #id")
    public Course getCourseById(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        return courseRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", id));
    }

    @Cacheable(value = "courses", key = "T(com.shrishailacademy.tenant.TenantContext).requireTenantId() + ':subject:' + #subjectKey")
    public Course getCourseBySubjectKey(String subjectKey) {
        Long tenantId = TenantContext.requireTenantId();
        String normalizedSubjectKey = normalizeSubjectKey(subjectKey, subjectKey);
        return courseRepository.findBySubjectKeyAndTenantId(normalizedSubjectKey, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "subjectKey", normalizedSubjectKey));
    }

    @CacheEvict(value = "courses", allEntries = true)
    public Course createCourse(CourseCreateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        String normalizedTitle = InputSanitizer.sanitizeAndTruncate(request.getTitle(), 100);
        String normalizedSubjectKey = normalizeSubjectKey(request.getSubjectKey(), request.getTitle());
        if (courseRepository.existsByTitleAndTenantId(normalizedTitle, tenantId)) {
            throw new DuplicateResourceException("Course", "title", normalizedTitle);
        }
        if (courseRepository.existsBySubjectKeyAndTenantId(normalizedSubjectKey, tenantId)) {
            throw new DuplicateResourceException("Course", "subjectKey", normalizedSubjectKey);
        }

        Course course = new Course();
        course.setTenant(tenantService.requireCurrentTenant());
        course.setTitle(normalizedTitle);
        course.setDescription(InputSanitizer.sanitizeNullable(request.getDescription()));
        course.setDuration(InputSanitizer.sanitizeAndTruncateNullable(request.getDuration(), 50));
        course.setIcon(InputSanitizer.sanitizeAndTruncateNullable(request.getIcon(), 50));
        course.setColor(InputSanitizer.sanitizeAndTruncateNullable(request.getColor(), 20));
        course.setSubjectKey(normalizedSubjectKey);
        course.setTeacher(resolveTeacher(request.getTeacherId()));
        course.setFee(request.getFee());

        Course saved = courseRepository.save(course);
        log.info("Course created: id={} title='{}' subjectKey={} teacherId={}", saved.getId(), saved.getTitle(),
                saved.getSubjectKey(), saved.getTeacher() != null ? saved.getTeacher().getId() : null);
        return saved;
    }

    @CacheEvict(value = "courses", allEntries = true)
    public Course updateCourse(Long id, CourseUpdateRequest request) {
        Long tenantId = TenantContext.requireTenantId();
        Course course = getCourseById(id);
        String effectiveTitle = course.getTitle();

        if (request.getTitle() != null) {
            String newTitle = InputSanitizer.sanitizeAndTruncate(request.getTitle(), 100);
            if (!newTitle.equals(course.getTitle())
                    && courseRepository.existsByTitleAndTenantIdAndIdNot(newTitle, tenantId, id)) {
                throw new DuplicateResourceException("Course", "title", newTitle);
            }
            course.setTitle(newTitle);
            effectiveTitle = newTitle;
        }
        if (request.getDescription() != null) {
            course.setDescription(InputSanitizer.sanitizeNullable(request.getDescription()));
        }
        if (request.getDuration() != null) {
            course.setDuration(InputSanitizer.sanitizeAndTruncateNullable(request.getDuration(), 50));
        }
        if (request.getIcon() != null) {
            course.setIcon(InputSanitizer.sanitizeAndTruncateNullable(request.getIcon(), 50));
        }
        if (request.getColor() != null) {
            course.setColor(InputSanitizer.sanitizeAndTruncateNullable(request.getColor(), 20));
        }
        if (request.getSubjectKey() != null || course.getSubjectKey() == null || course.getSubjectKey().isBlank()) {
            String normalizedSubjectKey = normalizeSubjectKey(request.getSubjectKey(), effectiveTitle);
            if (!normalizedSubjectKey.equals(course.getSubjectKey())
                    && courseRepository.existsBySubjectKeyAndTenantIdAndIdNot(normalizedSubjectKey, tenantId, id)) {
                throw new DuplicateResourceException("Course", "subjectKey", normalizedSubjectKey);
            }
            course.setSubjectKey(normalizedSubjectKey);
        }
        if (request.getTeacherId() != null) {
            course.setTeacher(resolveTeacher(request.getTeacherId()));
        }
        if (request.getFee() != null) {
            course.setFee(request.getFee());
        }

        Course updated = courseRepository.save(course);
        log.info("Course updated: id={} title='{}' subjectKey={} teacherId={}", updated.getId(), updated.getTitle(),
                updated.getSubjectKey(), updated.getTeacher() != null ? updated.getTeacher().getId() : null);
        return updated;
    }

    @CacheEvict(value = "courses", allEntries = true)
    public void deleteCourse(Long id) {
        Course course = getCourseById(id);
        courseRepository.delete(course);
        log.info("Course deleted: id={} title='{}'", id, course.getTitle());
    }

    private User resolveTeacher(Long teacherId) {
        if (teacherId == null) {
            return null;
        }

        Long tenantId = TenantContext.requireTenantId();
        User teacher = userRepository.findByIdAndTenantId(teacherId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", teacherId));
        if (teacher.getRole() != User.Role.TEACHER && teacher.getRole() != User.Role.ADMIN) {
            throw new IllegalArgumentException("Assigned teacher must have TEACHER or ADMIN role");
        }
        return teacher;
    }

    private String normalizeSubjectKey(String requestedSubjectKey, String fallbackTitle) {
        String source = requestedSubjectKey;
        if (source == null || source.isBlank()) {
            source = fallbackTitle;
        }

        String sanitized = InputSanitizer.sanitizeAndTruncate(source, 50)
                .toLowerCase(Locale.ROOT)
                .replace('&', ' ')
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");

        if (sanitized.isBlank()) {
            throw new IllegalArgumentException("Subject key is required");
        }
        if (sanitized.equals("mathematics") || sanitized.equals("math")) {
            return "maths";
        }
        return sanitized;
    }
}
