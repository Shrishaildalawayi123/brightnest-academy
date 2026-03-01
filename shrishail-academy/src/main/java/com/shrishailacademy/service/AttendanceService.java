package com.shrishailacademy.service;

import com.shrishailacademy.dto.AttendanceRequest;
import com.shrishailacademy.exception.BusinessException;
import com.shrishailacademy.exception.ResourceNotFoundException;
import com.shrishailacademy.model.Attendance;
import com.shrishailacademy.model.Course;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.AttendanceRepository;
import com.shrishailacademy.repository.CourseRepository;
import com.shrishailacademy.repository.EnrollmentRepository;
import com.shrishailacademy.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class AttendanceService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceService.class);

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    public AttendanceService(AttendanceRepository attendanceRepository,
            UserRepository userRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository) {
        this.attendanceRepository = attendanceRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    /**
     * Mark attendance for multiple students in a course for a given date (Admin
     * only).
     */
    @Transactional
    public List<Attendance> markAttendance(AttendanceRequest request, Long adminId) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", request.getCourseId()));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", adminId));

        List<Attendance> savedRecords = new ArrayList<>();

        for (AttendanceRequest.StudentAttendance record : request.getRecords()) {
            User student = userRepository.findById(record.getStudentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student", "id", record.getStudentId()));

            // Verify student is enrolled in the course
            if (!enrollmentRepository.existsByUserIdAndCourseIdAndStatusNot(
                    student.getId(), course.getId(),
                    com.shrishailacademy.model.Enrollment.Status.CANCELLED)) {
                log.warn("Student {} is not enrolled in course {}, skipping", student.getId(), course.getId());
                continue;
            }

            Attendance.Status status;
            try {
                status = Attendance.Status.valueOf(record.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessException("Invalid attendance status: " + record.getStatus());
            }

            // Update existing or create new attendance record
            Optional<Attendance> existing = attendanceRepository
                    .findByUserIdAndCourseIdAndAttendanceDate(student.getId(), course.getId(), request.getDate());

            Attendance attendance;
            if (existing.isPresent()) {
                attendance = existing.get();
                attendance.setStatus(status);
                attendance.setRemarks(record.getRemarks());
                attendance.setMarkedBy(admin);
            } else {
                attendance = new Attendance();
                attendance.setUser(student);
                attendance.setCourse(course);
                attendance.setAttendanceDate(request.getDate());
                attendance.setStatus(status);
                attendance.setRemarks(record.getRemarks());
                attendance.setMarkedBy(admin);
            }

            savedRecords.add(attendanceRepository.save(attendance));
        }

        log.info("ATTENDANCE_MARKED: course='{}' date={} records={}", course.getTitle(), request.getDate(),
                savedRecords.size());
        return savedRecords;
    }

    public List<Attendance> getStudentCourseAttendance(Long userId, Long courseId) {
        return attendanceRepository.findByUserIdAndCourseId(userId, courseId);
    }

    public List<Attendance> getStudentAttendance(Long userId) {
        return attendanceRepository.findByUserId(userId);
    }

    public List<Attendance> getCourseAttendanceByDate(Long courseId, LocalDate date) {
        return attendanceRepository.findByCourseIdAndAttendanceDate(courseId, date);
    }

    public List<Attendance> getCourseAttendance(Long courseId) {
        return attendanceRepository.findByCourseId(courseId);
    }

    public List<Attendance> getAllAttendance() {
        return attendanceRepository.findAll();
    }

    public Map<String, Object> getAttendanceSummary(Long userId, Long courseId) {
        List<Object[]> rawSummary = attendanceRepository.getAttendanceSummary(userId, courseId);
        Map<String, Object> summary = new LinkedHashMap<>();
        long total = 0;
        long present = 0;

        for (Object[] row : rawSummary) {
            String status = ((Attendance.Status) row[0]).name();
            long count = (Long) row[1];
            summary.put(status.toLowerCase(), count);
            total += count;
            if ("PRESENT".equals(status) || "LATE".equals(status)) {
                present += count;
            }
        }

        summary.put("total", total);
        summary.put("percentage", total > 0 ? Math.round((present * 100.0) / total) : 0);
        return summary;
    }

    public List<Attendance> getAttendanceByDateRange(Long courseId, LocalDate start, LocalDate end) {
        return attendanceRepository.findByCourseIdAndAttendanceDateBetween(courseId, start, end);
    }
}
