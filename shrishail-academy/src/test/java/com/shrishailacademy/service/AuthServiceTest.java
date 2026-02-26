package com.shrishailacademy.service;

import com.shrishailacademy.dto.AuthResponse;
import com.shrishailacademy.dto.LoginRequest;
import com.shrishailacademy.dto.RegisterRequest;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.UserRepository;
import com.shrishailacademy.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthService authService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registerShouldThrowWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("Student", "student@example.com", "Password@123", "9999999999");
        when(userRepository.existsByEmail("student@example.com")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.register(request));

        assertEquals("Email already exists", ex.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerShouldEncodePasswordAndReturnAuthResponse() {
        RegisterRequest request = new RegisterRequest("Student", "student@example.com", "Password@123", "9999999999");
        when(userRepository.existsByEmail("student@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });
        when(tokenProvider.generateTokenFromUsername("student@example.com")).thenReturn("jwt-token");

        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertEquals("encoded-password", savedUser.getPassword());
        assertEquals(User.Role.STUDENT, savedUser.getRole());
        assertEquals("jwt-token", response.getToken());
        assertEquals(10L, response.getId());
        assertEquals("student@example.com", response.getEmail());
        assertEquals("STUDENT", response.getRole());
    }

    @Test
    void loginShouldAuthenticateAndReturnAuthResponse() {
        LoginRequest request = new LoginRequest("admin@example.com", "Admin@123");
        Authentication authentication = mock(Authentication.class);
        User user = new User();
        user.setId(2L);
        user.setName("Admin");
        user.setEmail("admin@example.com");
        user.setRole(User.Role.ADMIN);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn("token-123");
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));

        AuthResponse response = authService.login(request);

        assertEquals("token-123", response.getToken());
        assertEquals(2L, response.getId());
        assertEquals("ADMIN", response.getRole());
        assertSame(authentication, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void loginShouldThrowWhenUserNotFoundAfterAuthentication() {
        LoginRequest request = new LoginRequest("missing@example.com", "Password@123");
        Authentication authentication = mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn("token-xyz");
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(request));

        assertEquals("User not found", ex.getMessage());
    }
}
