package com.shrishailacademy.service;

import com.shrishailacademy.exception.ResourceNotFoundException;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getUserByIdShouldReturnUserWhenExists() {
        User user = new User();
        user.setId(1L);
        user.setEmail("student@example.com");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User result = userService.getUserById(1L);

        assertSame(user, result);
    }

    @Test
    void getUserByIdShouldThrowWhenMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserById(99L));
    }

    @Test
    void getUserByEmailShouldReturnUserWhenExists() {
        User user = new User();
        user.setId(2L);
        user.setEmail("admin@example.com");

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));

        User result = userService.getUserByEmail("admin@example.com");

        assertSame(user, result);
    }

    @Test
    void getUserByEmailShouldThrowWhenMissing() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.getUserByEmail("missing@example.com"));
    }

    @Test
    void getAllStudentsShouldReturnStudentsList() {
        User s1 = new User();
        s1.setId(1L);
        User s2 = new User();
        s2.setId(2L);

        when(userRepository.findAllStudents()).thenReturn(List.of(s1, s2));

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

        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        List<User> result = userService.getAllUsers();

        assertEquals(2, result.size());
        assertSame(u1, result.get(0));
        assertSame(u2, result.get(1));
    }
}
