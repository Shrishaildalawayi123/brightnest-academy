package com.shrishailacademy.config;

import com.shrishailacademy.model.Tenant;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.CourseRepository;
import com.shrishailacademy.repository.UserRepository;
import com.shrishailacademy.service.TenantService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
        var initializer = new DataInitializer(users, mock(CourseRepository.class), encoder, tenants);
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
        Tenant tenant = new Tenant(); tenant.setId(1L);
        when(tenants.ensureDefaultTenantExists()).thenReturn(tenant);
        var initializer = new DataInitializer(users, mock(CourseRepository.class), mock(PasswordEncoder.class), tenants);
        initializer.run();
        verifyNoInteractions(users);
    }
}
