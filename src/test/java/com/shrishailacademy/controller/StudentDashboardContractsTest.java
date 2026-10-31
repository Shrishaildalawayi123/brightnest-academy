package com.shrishailacademy.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.shrishailacademy.dto.AttendanceRequest;
import com.shrishailacademy.exception.ResourceNotFoundException;
import com.shrishailacademy.model.*;
import com.shrishailacademy.repository.*;
import com.shrishailacademy.service.*;
import com.shrishailacademy.tenant.TenantContext;

import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.*;
import java.util.*;

class StudentDashboardContractsTest {
    final UserRepository users = mock(UserRepository.class);
    final NotificationRepository notifications = mock(NotificationRepository.class);
    final NotificationController controller = new NotificationController(notifications, users);
    User user;
    org.springframework.security.core.userdetails.UserDetails principal;

    @BeforeEach
    void setup() {
        TenantContext.set(7L, "a");
        user = new User();
        user.setId(11L);
        user.setName("Student");
        user.setEmail("student@example.com");
        user.setRole(User.Role.STUDENT);
        principal =
                org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                        .password("unused")
                        .roles("STUDENT")
                        .build();
        when(users.findByEmailAndTenantId(user.getEmail(), 7L)).thenReturn(Optional.of(user));
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void notificationPaginationIsBoundedAndUnreadCountsAreUserScoped() {
        when(notifications.countByUserIdAndTenantIdAndReadFalse(11L, 7L)).thenReturn(3L);
        assertEquals(3L, controller.getUnreadCount(principal).getBody().get("count"));
        controller.getMyNotificationsForStudent(principal, -3, 1000);
        var pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(notifications)
                .findByUserIdAndTenantIdOrderByCreatedAtDesc(eq(11L), eq(7L), pageable.capture());
        assertEquals(0, pageable.getValue().getPageNumber());
        assertEquals(100, pageable.getValue().getPageSize());
        controller.getUnreadNotifications(principal, 0, -1);
        verify(notifications)
                .findByUserIdAndTenantIdAndReadFalseOrderByCreatedAtDesc(
                        eq(11L), eq(7L), pageable.capture());
        assertEquals(1, pageable.getValue().getPageSize());
    }

    @Test
    void notificationReadUpdatesRejectOtherUsersAndTenants() {
        Tenant tenant = new Tenant();
        tenant.setId(7L);
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setUser(user);
        notification.setTenant(tenant);
        when(notifications.findByIdAndTenant_Id(1L, 7L)).thenReturn(Optional.of(notification));
        controller.markAsReadPatch(1L, principal);
        assertTrue(notification.isRead());
        verify(notifications).save(notification);
        User other = new User();
        other.setId(99L);
        notification.setUser(other);
        assertThrows(ResourceNotFoundException.class, () -> controller.markAsRead(1L, principal));
        notification.setUser(user);
        tenant.setId(8L);
        assertThrows(ResourceNotFoundException.class, () -> controller.markAsRead(1L, principal));
        assertThrows(ResourceNotFoundException.class, () -> controller.markAsRead(99L, principal));
        verify(notifications, times(1)).save(any());
    }

    @Test
    void attendanceViewsUseAuthenticatedStudentAndAdminSummaryRequiresExplicitTarget() {
        AttendanceService service = mock(AttendanceService.class);
        UserService userService = mock(UserService.class);
        when(userService.getUserByEmail(user.getEmail())).thenReturn(user);
        var attendanceController = new AttendanceController(service, userService);
        var authentication = new UsernamePasswordAuthenticationToken(user.getEmail(), "unused");
        Course course = new Course();
        course.setId(21L);
        course.setTitle("Maths");
        Attendance a = new Attendance();
        a.setId(1L);
        a.setUser(user);
        a.setCourse(course);
        a.setMarkedBy(user);
        a.setAttendanceDate(LocalDate.now());
        a.setStatus(Attendance.Status.PRESENT);
        when(service.getStudentAttendance(11L)).thenReturn(List.of(a));
        when(service.getStudentCourseAttendance(11L, 21L)).thenReturn(List.of(a));
        assertEquals(
                "Maths",
                attendanceController
                        .getMyAttendance(authentication)
                        .getBody()
                        .get(0)
                        .getCourseTitle());
        assertEquals(
                "PRESENT",
                attendanceController
                        .getMyAttendanceForCourse(21L, authentication)
                        .getBody()
                        .get(0)
                        .getStatus());
        attendanceController.getAttendanceSummary(999L, 21L, authentication);
        verify(service).getAttendanceSummary(11L, 21L);
        user.setRole(User.Role.ADMIN);
        assertThrows(
                IllegalArgumentException.class,
                () -> attendanceController.getAttendanceSummary(null, 21L, authentication));
        when(service.getCourseAttendance(21L)).thenReturn(List.of(a));
        when(service.getCourseAttendanceByDate(21L, LocalDate.now())).thenReturn(List.of(a));
        when(service.getAllAttendance()).thenReturn(List.of(a));
        assertEquals(1, attendanceController.getCourseAttendance(21L, null).getBody().size());
        assertEquals(
                1, attendanceController.getCourseAttendance(21L, LocalDate.now()).getBody().size());
        assertEquals(1, attendanceController.getAllAttendance().getBody().size());
        var request = new AttendanceRequest();
        when(service.markAttendance(request, 11L)).thenReturn(List.of(a));
        assertEquals(
                200,
                attendanceController
                        .markAttendance(request, authentication)
                        .getStatusCode()
                        .value());
    }
}
