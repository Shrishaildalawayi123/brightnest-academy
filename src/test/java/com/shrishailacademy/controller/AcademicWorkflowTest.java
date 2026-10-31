package com.shrishailacademy.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.shrishailacademy.dto.*;
import com.shrishailacademy.model.*;
import com.shrishailacademy.repository.*;
import com.shrishailacademy.tenant.TenantContext;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.*;
import java.util.*;

class AcademicWorkflowTest {
    final UserRepository users = mock(UserRepository.class);
    final AssignmentRepository assignments = mock(AssignmentRepository.class);
    final AssignmentSubmissionRepository submissions = mock(AssignmentSubmissionRepository.class);
    final ClassSessionRepository sessions = mock(ClassSessionRepository.class);
    final SessionAttendanceRepository attendance = mock(SessionAttendanceRepository.class);
    final AssignmentSubmissionController submissionController =
            new AssignmentSubmissionController(submissions, assignments, users);
    final ClassSessionController sessionController = new ClassSessionController(sessions, users);
    final SessionAttendanceController attendanceController =
            new SessionAttendanceController(attendance, sessions, users);
    User student, teacher;
    Course course;
    Assignment assignment;
    ClassSession session;
    Authentication studentAuth, teacherAuth;

    @BeforeEach
    void setup() {
        TenantContext.set(7L, "academy");
        student = user(11L, "Student", User.Role.STUDENT);
        teacher = user(12L, "Teacher", User.Role.TEACHER);
        course = new Course();
        course.setId(21L);
        course.setTitle("Mathematics");
        assignment =
                Assignment.builder()
                        .id(31L)
                        .tenantId(7L)
                        .title("Algebra")
                        .course(course)
                        .teacher(teacher)
                        .maxScore(100)
                        .isPublished(true)
                        .dueDate(LocalDateTime.now().plusDays(1))
                        .build();
        ClassSchedule schedule =
                ClassSchedule.builder()
                        .id(41L)
                        .tenantId(7L)
                        .course(course)
                        .teacher(teacher)
                        .dayOfWeek(DayOfWeek.MONDAY)
                        .startTime(LocalTime.of(10, 0))
                        .endTime(LocalTime.of(11, 0))
                        .roomNumber("A")
                        .build();
        session =
                ClassSession.builder()
                        .id(51L)
                        .tenantId(7L)
                        .schedule(schedule)
                        .sessionDate(LocalDate.now())
                        .build();
        studentAuth = new UsernamePasswordAuthenticationToken(student.getEmail(), "unused");
        teacherAuth = new UsernamePasswordAuthenticationToken(teacher.getEmail(), "unused");
        when(users.findByEmailAndTenantId(student.getEmail(), 7L)).thenReturn(Optional.of(student));
        when(users.findByEmailAndTenantId(teacher.getEmail(), 7L)).thenReturn(Optional.of(teacher));
        when(assignments.findByIdAndTenantId(31L, 7L)).thenReturn(Optional.of(assignment));
        when(sessions.findByIdAndTenantId(51L, 7L)).thenReturn(Optional.of(session));
        when(submissions.save(any()))
                .thenAnswer(
                        i -> {
                            AssignmentSubmission s = i.getArgument(0);
                            s.setId(61L);
                            return s;
                        });
        when(sessions.save(any())).thenAnswer(i -> i.getArgument(0));
        when(attendance.save(any())).thenAnswer(i -> i.getArgument(0));
        when(attendance.saveAll(any())).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    User user(long id, String name, User.Role role) {
        User u = new User();
        u.setId(id);
        u.setName(name);
        u.setRole(role);
        u.setEmail(name.toLowerCase() + "@example.com");
        return u;
    }

    AssignmentSubmissionDTO submissionRequest() {
        return AssignmentSubmissionDTO.builder()
                .assignmentId(31L)
                .studentId(11L)
                .submissionText("My algebra solution")
                .build();
    }

    AssignmentSubmission submission() {
        return AssignmentSubmission.builder()
                .id(61L)
                .tenantId(7L)
                .assignment(assignment)
                .student(student)
                .submissionText("Initial answer")
                .submittedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void submitUsesAuthenticatedStudentAndDetectsLateness() {
        assignment.setDueDate(LocalDateTime.now().minusDays(1));
        var dto = submissionRequest();
        dto.setStudentId(999L); // Submitted identity is never trusted.
        var response = submissionController.submitAssignment(dto, studentAuth);
        assertEquals(201, response.getStatusCode().value());
        assertEquals(11L, response.getBody().getStudentId());
        assertTrue(response.getBody().getIsLate());
        assertFalse(response.getBody().getIsGraded());
        assertEquals("Mathematics", response.getBody().getCourseName());
    }

    @Test
    void duplicateAndUnpublishedSubmissionsCannotBeSaved() {
        when(submissions.findByAssignmentIdAndStudentIdAndTenantId(31L, 11L, 7L))
                .thenReturn(Optional.of(submission()));
        assertThrows(
                IllegalStateException.class,
                () -> submissionController.submitAssignment(submissionRequest(), studentAuth));
        when(submissions.findByAssignmentIdAndStudentIdAndTenantId(31L, 11L, 7L))
                .thenReturn(Optional.empty());
        assignment.setIsPublished(false);
        assertThrows(
                IllegalStateException.class,
                () -> submissionController.submitAssignment(submissionRequest(), studentAuth));
        verify(submissions, never()).save(any());
    }

    @Test
    void studentCanReviseOwnUngradedWorkButNotAnotherStudentsOrGradedWork() {
        var existing = submission();
        when(submissions.findByIdAndTenantId(61L, 7L)).thenReturn(Optional.of(existing));
        assertEquals(
                "My algebra solution",
                submissionController
                        .updateSubmission(61L, submissionRequest(), studentAuth)
                        .getBody()
                        .getSubmissionText());
        existing.grade(90, "Great", teacher.getId());
        assertThrows(
                IllegalStateException.class,
                () -> submissionController.updateSubmission(61L, submissionRequest(), studentAuth));
        existing.setStudent(user(99L, "Other", User.Role.STUDENT));
        assertThrows(
                IllegalArgumentException.class,
                () -> submissionController.updateSubmission(61L, submissionRequest(), studentAuth));
        verify(submissions, times(1)).save(any());
    }

    @ParameterizedTest
    @CsvSource({"95,A", "85,B", "75,C", "65,D", "50,F"})
    void gradingPersistsTeacherFeedbackAndStudentResult(int score, String letter) {
        var existing = submission();
        when(submissions.findByIdAndTenantId(61L, 7L)).thenReturn(Optional.of(existing));
        var dto = submissionRequest();
        dto.setScore(score);
        dto.setFeedback("Reviewed");
        var result = submissionController.gradeSubmission(61L, dto, teacherAuth).getBody();
        assertEquals(letter, result.getLetterGrade());
        assertEquals((double) score, result.getPercentageScore());
        assertEquals("Reviewed", result.getFeedback());
        assertEquals(teacher.getId(), existing.getGradedBy());
        assertTrue(result.getIsGraded());
        assertNotNull(result.getGradedAt());
    }

    @Test
    void excessiveAndMissingScoresDoNotPersist() {
        when(submissions.findByIdAndTenantId(61L, 7L)).thenReturn(Optional.of(submission()));
        var dto = submissionRequest();
        dto.setScore(101);
        assertThrows(
                IllegalArgumentException.class,
                () -> submissionController.gradeSubmission(61L, dto, teacherAuth));
        dto.setScore(null);
        assertThrows(
                IllegalArgumentException.class,
                () -> submissionController.gradeSubmission(61L, dto, teacherAuth));
        verify(submissions, never()).save(any());
    }

    @Test
    void teacherAndAdminUngradedQueuesUseTheirCorrectScope() {
        when(submissions
                        .findByTenantIdAndAssignment_Teacher_IdAndGradedAtIsNullOrderBySubmittedAtAsc(
                                7L, 12L))
                .thenReturn(List.of(submission()));
        assertEquals(1, submissionController.getSubmissions(false, teacherAuth).getBody().size());
        assertTrue(submissionController.getSubmissions(true, teacherAuth).getBody().isEmpty());
        teacher.setRole(User.Role.ADMIN);
        when(submissions.findByTenantIdAndGradedAtIsNullOrderBySubmittedAtAsc(7L))
                .thenReturn(List.of(submission()));
        when(submissions.findUngradedSubmissionsByTenant(31L, 7L))
                .thenReturn(List.of(submission()));
        assertEquals(
                1, submissionController.getUngradedSubmissions(null, teacherAuth).getBody().size());
        assertEquals(
                1, submissionController.getUngradedSubmissions(31L, teacherAuth).getBody().size());
        when(submissions.findByAssignment_IdAndTenantId(31L, 7L)).thenReturn(List.of(submission()));
        assertEquals(1, submissionController.getAssignmentSubmissions(31L).getBody().size());
        when(submissions.findByIdAndTenantId(61L, 7L)).thenReturn(Optional.of(submission()));
        assertEquals(204, submissionController.deleteSubmission(61L).getStatusCode().value());
        verify(submissions).delete(any());
    }

    @Test
    void sessionLifecycleReturnsUpdatedDatesNotesAndCancellationReason() {
        assertEquals(
                ClassSession.SessionStatus.IN_PROGRESS,
                sessionController.startSession(51L).getBody().getStatus());
        assertNotNull(session.getActualStartTime());
        assertEquals(
                ClassSession.SessionStatus.COMPLETED,
                sessionController.completeSession(51L).getBody().getStatus());
        assertNotNull(session.getActualEndTime());
        var dto = new ClassSessionDTO();
        dto.setNotes("Revision lesson");
        assertEquals(
                "Revision lesson",
                sessionController.updateSessionNotes(51L, dto).getBody().getNotes());
        assertEquals(
                "Teacher unavailable",
                sessionController
                        .cancelSession(51L, "Teacher unavailable")
                        .getBody()
                        .getCancellationReason());
        assertEquals(
                "Mathematics", sessionController.getSessionById(51L).getBody().getCourseName());
        assertThrows(IllegalArgumentException.class, () -> sessionController.startSession(999L));
    }

    @Test
    void sessionQueuesSelectAdminTeacherUpcomingAndAttendanceScopes() {
        when(sessions.findTodaySessionsForTeacher(7L, 12L, LocalDate.now()))
                .thenReturn(List.of(session));
        assertEquals(1, sessionController.getSessions(true, teacherAuth).getBody().size());
        teacher.setRole(User.Role.ADMIN);
        when(sessions.findTodaySessions(7L, LocalDate.now())).thenReturn(List.of(session));
        when(sessions.findUpcomingSessions(7L, LocalDate.now())).thenReturn(List.of(session));
        when(sessions.findSessionsNeedingAttendance(7L, LocalDate.now()))
                .thenReturn(List.of(session));
        assertEquals(1, sessionController.getSessions(true, teacherAuth).getBody().size());
        assertEquals(1, sessionController.getSessions(false, teacherAuth).getBody().size());
        assertEquals(1, sessionController.getSessionsNeedingAttendance().getBody().size());
    }

    @ParameterizedTest
    @EnumSource(SessionAttendance.AttendanceStatus.class)
    void attendanceMarkAndCorrectionPersistStatusAndAuditIdentity(
            SessionAttendance.AttendanceStatus status) {
        when(users.findByIdAndTenantId(11L, 7L)).thenReturn(Optional.of(student));
        var dto =
                AttendanceDTO.builder()
                        .studentId(11L)
                        .sessionId(51L)
                        .status(status)
                        .notes("Recorded by teacher")
                        .checkInTime(LocalTime.of(10, 5))
                        .build();
        var result = attendanceController.markAttendance(List.of(dto), teacherAuth);
        assertEquals(201, result.getStatusCode().value());
        assertEquals(status, result.getBody().get(0).getStatus());
        assertTrue(session.getAttendanceMarked());
        var existing =
                SessionAttendance.builder()
                        .id(71L)
                        .tenantId(7L)
                        .session(session)
                        .student(student)
                        .build();
        when(attendance.findByIdAndTenantId(71L, 7L)).thenReturn(Optional.of(existing));
        var updated = attendanceController.updateAttendance(71L, dto, teacherAuth).getBody();
        assertEquals(status, updated.getStatus());
        assertEquals(12L, existing.getMarkedBy());
        assertEquals("Recorded by teacher", updated.getNotes());
        assertNotNull(updated.getMarkedAt());
        when(attendance.findBySession_IdAndTenantId(51L, 7L)).thenReturn(List.of(existing));
        assertEquals(
                "Mathematics",
                attendanceController.getSessionAttendance(51L).getBody().get(0).getCourseName());
    }
}
