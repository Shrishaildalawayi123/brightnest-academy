package com.shrishailacademy.service;

import com.shrishailacademy.dto.UserCreateRequest;
import com.shrishailacademy.dto.UserUpdateRequest;
import com.shrishailacademy.exception.DuplicateResourceException;
import com.shrishailacademy.exception.ResourceNotFoundException;
import com.shrishailacademy.model.Tenant;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.UserRepository;
import com.shrishailacademy.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Long TENANT_ID = 1L;
    private static final String TENANT_KEY = "default";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TenantService tenantService;

    @InjectMocks
    private UserService userService;

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
    void getUserByIdShouldReturnUserWhenExists() {
        User user = new User();
        user.setId(1L);
        user.setEmail("student@example.com");

        when(userRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(user));

        User result = userService.getUserById(1L);

        assertSame(user, result);
    }

    @Test
    void getUserByIdShouldThrowWhenMissing() {
        when(userRepository.findByIdAndTenantId(99L, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(99L));
    }

    @Test
    void getUserByEmailShouldReturnUserWhenExists() {
        User user = new User();
        user.setId(2L);
        user.setEmail("admin@example.com");

        when(userRepository.findByEmailAndTenantId("admin@example.com", TENANT_ID)).thenReturn(Optional.of(user));

        User result = userService.getUserByEmail("admin@example.com");

        assertSame(user, result);
    }

    @Test
    void getUserByEmailShouldThrowWhenMissing() {
        when(userRepository.findByEmailAndTenantId("missing@example.com", TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserByEmail("missing@example.com"));
    }

    @Test
    void getAllStudentsShouldReturnStudentsList() {
        User s1 = new User();
        s1.setId(1L);
        User s2 = new User();
        s2.setId(2L);

        when(userRepository.findByRoleAndTenantId(User.Role.STUDENT, TENANT_ID)).thenReturn(List.of(s1, s2));

        List<User> result = userService.getAllStudents();

        assertEquals(2, result.size());
        assertSame(s1, result.get(0));
        assertSame(s2, result.get(1));
    }

    @Test
    void getAllUsersShouldReturnAllUsers() {
        User u1 = new User();
        u1.setId(1L);
        User u2 = new User();
        u2.setId(2L);

        when(userRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(u1, u2));

        List<User> result = userService.getAllUsers();

        assertEquals(2, result.size());
        assertSame(u1, result.get(0));
        assertSame(u2, result.get(1));
    }

    @Test
    void createUserShouldPersistWhenEmailIsUnique() {
        UserCreateRequest request = new UserCreateRequest(
                "New Admin",
                "newadmin@example.com",
                "Admin@123!",
                "9999999999",
                User.Role.ADMIN);

        when(userRepository.existsByEmailAndTenantId("newadmin@example.com", TENANT_ID)).thenReturn(false);
        when(passwordEncoder.encode("Admin@123!")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.createUser(request);

        assertEquals("newadmin@example.com", saved.getEmail());
        assertEquals("encoded", saved.getPassword());
        assertEquals(User.Role.ADMIN, saved.getRole());
    }

    @Test
    void createUserShouldThrowWhenEmailAlreadyExists() {
        UserCreateRequest request = new UserCreateRequest(
                "Existing",
                "existing@example.com",
                "Admin@123!",
                null,
                User.Role.STUDENT);

        when(userRepository.existsByEmailAndTenantId("existing@example.com", TENANT_ID)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> userService.createUser(request));
    }

    @Test
    void updateUserShouldApplyChanges() {
        User existing = new User();
        existing.setId(5L);
        existing.setEmail("old@example.com");
        existing.setPassword("oldhash");
        existing.setRole(User.Role.STUDENT);

        UserUpdateRequest request = new UserUpdateRequest(
                "Updated Name",
                "updated@example.com",
                "Strong@123",
                "8888888888",
                User.Role.ADMIN);

        when(userRepository.findByIdAndTenantId(5L, TENANT_ID)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmailAndTenantIdAndIdNot("updated@example.com", TENANT_ID, 5L)).thenReturn(false);
        when(passwordEncoder.encode("Strong@123")).thenReturn("newhash");
        when(userRepository.save(existing)).thenReturn(existing);

        User updated = userService.updateUser(5L, request);

        assertEquals("Updated Name", updated.getName());
        assertEquals("updated@example.com", updated.getEmail());
        assertEquals("newhash", updated.getPassword());
        assertEquals(User.Role.ADMIN, updated.getRole());
    }

    @Test
    void deleteUserShouldRemoveExistingUser() {
        User existing = new User();
        existing.setId(9L);
        existing.setEmail("delete@example.com");

        when(userRepository.findByIdAndTenantId(9L, TENANT_ID)).thenReturn(Optional.of(existing));

        userService.deleteUser(9L);

        verify(userRepository).delete(existing);
    }
}
