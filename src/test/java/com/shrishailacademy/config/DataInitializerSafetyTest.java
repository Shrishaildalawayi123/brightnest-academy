package com.shrishailacademy.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.shrishailacademy.model.Tenant;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.CourseRepository;
import com.shrishailacademy.repository.UserRepository;
import com.shrishailacademy.service.TenantService;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class DataInitializerSafetyTest {
    @Test
    void existingAdminPasswordIsNeverRewrittenOnRestart() {
        UserRepository users = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        TenantService tenants = mock(TenantService.class);
        Tenant tenant = new Tenant();
        tenant.setId(1L);
        when(tenants.ensureDefaultTenantExists()).thenReturn(tenant);
        when(users.existsByEmailAndTenantId("admin@example.com", 1L)).thenReturn(true);
        var initializer =
                new DataInitializer(users, mock(CourseRepository.class), encoder, tenants);
        ReflectionTestUtils.setField(initializer, "adminEmail", "admin@example.com");
        ReflectionTestUtils.setField(initializer, "adminPassword", "test-bootstrap-value");
        initializer.run();
        verify(users, never()).save(any(User.class));
        verifyNoInteractions(encoder);
    }

    @Test
    void emptyBootstrapDoesNotCreateAnAdministrator() {
        UserRepository users = mock(UserRepository.class);
        TenantService tenants = mock(TenantService.class);
        Tenant tenant = new Tenant();
        tenant.setId(1L);
        when(tenants.ensureDefaultTenantExists()).thenReturn(tenant);
        var initializer =
                new DataInitializer(
                        users, mock(CourseRepository.class), mock(PasswordEncoder.class), tenants);
        initializer.run();
        verifyNoInteractions(users);
    }

    @Test
    void explicitCatalogBootstrapCreatesAssignedCoursesAndNeverRewritesExistingTeacherPassword() {
        UserRepository users = mock(UserRepository.class);
        CourseRepository courses = mock(CourseRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        TenantService tenants = mock(TenantService.class);
        Tenant tenant = new Tenant();
        tenant.setId(1L);
        when(tenants.ensureDefaultTenantExists()).thenReturn(tenant);
        when(encoder.encode(any(String.class))).thenReturn("test-hash");
        when(users.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        User existing = new User();
        existing.setRole(User.Role.TEACHER);
        existing.setName("Bharati R Satappagol");
        existing.setPassword("existing-hash");
        when(users.findByEmailAndTenantId("bharati@brightnest-academy.com", 1L))
                .thenReturn(java.util.Optional.of(existing));
        var initializer = new DataInitializer(users, courses, encoder, tenants);
        ReflectionTestUtils.setField(initializer, "seedCatalog", true);
        initializer.run();
        var saved = org.mockito.ArgumentCaptor.forClass(com.shrishailacademy.model.Course.class);
        verify(courses, times(7)).save(saved.capture());
        org.junit.jupiter.api.Assertions.assertTrue(
                saved.getAllValues().stream()
                        .allMatch(
                                c ->
                                        c.getTeacher() != null
                                                && c.getFee().signum() > 0
                                                && c.getTenant() == tenant));
        org.junit.jupiter.api.Assertions.assertEquals(
                7,
                saved.getAllValues().stream()
                        .map(com.shrishailacademy.model.Course::getTitle)
                        .distinct()
                        .count());
        org.junit.jupiter.api.Assertions.assertEquals("existing-hash", existing.getPassword());
    }
}
