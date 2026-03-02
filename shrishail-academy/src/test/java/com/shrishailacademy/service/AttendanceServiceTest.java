package com.shrishailacademy.service;

import com.shrishailacademy.dto.AttendanceRequest;
import com.shrishailacademy.model.Attendance;
import com.shrishailacademy.model.Course;
import com.shrishailacademy.model.Enrollment;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.AttendanceRepository;
import com.shrishailacademy.repository.CourseRepository;
import com.shrishailacademy.repository.EnrollmentRepository;
import com.shrishailacademy.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

        @Mock
        private AttendanceRepository attendanceRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private CourseRepository courseRepository;

        @Mock
        private EnrollmentRepository enrollmentRepository;

        @InjectMocks
        private AttendanceService attendanceService;

        @Test
        void markAttendanceShouldSkipUnenrolledStudents() {
                Long courseId = 5L;
                Long adminId = 99L;
                LocalDate date = LocalDate.of(2026, 2, 26);

                Course course = new Course();
                course.setId(courseId);
                course.setTitle("Science");
                User admin = user(adminId, User.Role.ADMIN);
                User enrolledStudent = user(1L, User.Role.STUDENT);
                User unenrolledStudent = user(2L, User.Role.STUDENT);

                AttendanceRequest request = new AttendanceRequest(
                                courseId,
                                date,
                                List.of(
                                                new AttendanceRequest.StudentAttendance(1L, "PRESENT", "On time"),
                                                new AttendanceRequest.StudentAttendance(2L, "ABSENT", "Not enrolled")));

                when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
                when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
                when(userRepository.findAllById(any())).thenReturn(List.of(enrolledStudent, unenrolledStudent));
                when(enrollmentRepository.findActiveUserIdsByCourseIdAndUserIdIn(eq(courseId), anySet(),
                                eq(Enrollment.Status.CANCELLED)))
                                .thenReturn(Set.of(1L));
                when(attendanceRepository.findByCourseIdAndAttendanceDateAndUserIdIn(courseId, date, Set.of(1L)))
                                .thenReturn(List.of());
                when(attendanceRepository.saveAll(any())).thenAnswer(invocation -> {
                        Iterable<Attendance> records = invocation.getArgument(0);
                        List<Attendance> out = new ArrayList<>();
                        records.forEach(out::add);
                        return out;
                });

                List<Attendance> saved = attendanceService.markAttendance(request, adminId);

                assertEquals(1, saved.size());
                assertSame(enrolledStudent, saved.get(0).getUser());
                assertEquals(Attendance.Status.PRESENT, saved.get(0).getStatus());
                verify(attendanceRepository).findByCourseIdAndAttendanceDateAndUserIdIn(courseId, date, Set.of(1L));
        }

        @Test
        void markAttendanceShouldUpdateExistingRecord() {
                Long courseId = 3L;
                Long adminId = 11L;
                Long studentId = 21L;
                LocalDate date = LocalDate.of(2026, 2, 25);

                Course course = new Course();
                course.setId(courseId);
                course.setTitle("English");
                User admin = user(adminId, User.Role.ADMIN);
                User student = user(studentId, User.Role.STUDENT);

                Attendance existing = new Attendance();
                existing.setId(501L);
                existing.setUser(student);
                existing.setCourse(course);
                existing.setAttendanceDate(date);
                existing.setStatus(Attendance.Status.ABSENT);
                existing.setRemarks("Old remark");

                AttendanceRequest request = new AttendanceRequest(
                                courseId,
                                date,
                                List.of(new AttendanceRequest.StudentAttendance(studentId, "LATE", "Traffic")));

                when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
                when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
                when(userRepository.findAllById(any())).thenReturn(List.of(student));
                when(enrollmentRepository.findActiveUserIdsByCourseIdAndUserIdIn(eq(courseId), anySet(),
                                eq(Enrollment.Status.CANCELLED)))
                                .thenReturn(Set.of(studentId));
                when(attendanceRepository.findByCourseIdAndAttendanceDateAndUserIdIn(courseId, date, Set.of(studentId)))
                                .thenReturn(List.of(existing));
                when(attendanceRepository.saveAll(any())).thenAnswer(invocation -> {
                        Iterable<Attendance> records = invocation.getArgument(0);
                        List<Attendance> out = new ArrayList<>();
                        records.forEach(out::add);
                        return out;
                });

                List<Attendance> saved = attendanceService.markAttendance(request, adminId);

                assertEquals(1, saved.size());
                assertSame(existing, saved.get(0));
                assertEquals(Attendance.Status.LATE, existing.getStatus());
                assertEquals("Traffic", existing.getRemarks());
                assertSame(admin, existing.getMarkedBy());
        }

        @Test
        void markAttendanceShouldThrowForInvalidStatus() {
                Long courseId = 8L;
                Long adminId = 18L;
                Long studentId = 28L;
                LocalDate date = LocalDate.of(2026, 2, 24);

                Course course = new Course();
                course.setId(courseId);
                User admin = user(adminId, User.Role.ADMIN);
                User student = user(studentId, User.Role.STUDENT);

                AttendanceRequest request = new AttendanceRequest(
                                courseId,
                                date,
                                List.of(new AttendanceRequest.StudentAttendance(studentId, "INVALID", null)));

                when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
                when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
                when(userRepository.findAllById(any())).thenReturn(List.of(student));
                when(enrollmentRepository.findActiveUserIdsByCourseIdAndUserIdIn(eq(courseId), anySet(),
                                eq(Enrollment.Status.CANCELLED)))
                                .thenReturn(Set.of(studentId));

                RuntimeException ex = assertThrows(RuntimeException.class,
                                () -> attendanceService.markAttendance(request, adminId));

                assertEquals("Invalid attendance status: INVALID", ex.getMessage());
                verify(attendanceRepository, never()).saveAll(any());
        }

        @Test
        void getAttendanceSummaryShouldCalculatePercentageUsingPresentAndLate() {
                List<Object[]> rawSummary = List.of(
                                new Object[] { Attendance.Status.PRESENT, 5L },
                                new Object[] { Attendance.Status.ABSENT, 3L },
                                new Object[] { Attendance.Status.LATE, 2L });

                when(attendanceRepository.getAttendanceSummary(1L, 2L)).thenReturn(rawSummary);

                Map<String, Object> summary = attendanceService.getAttendanceSummary(1L, 2L);

                assertEquals(5L, summary.get("present"));
                assertEquals(3L, summary.get("absent"));
                assertEquals(2L, summary.get("late"));
                assertEquals(10L, summary.get("total"));
                assertEquals(70L, summary.get("percentage"));
        }

        private User user(Long id, User.Role role) {
                User user = new User();
                user.setId(id);
                user.setRole(role);
                user.setName("User " + id);
                user.setEmail("user" + id + "@example.com");
                user.setPassword("encoded-password");
                return user;
        }
}
