package com.shrishailacademy.service;

import com.shrishailacademy.dto.AttendanceRequest;
import com.shrishailacademy.model.Attendance;
import com.shrishailacademy.model.Course;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.AttendanceRepository;
import com.shrishailacademy.repository.CourseRepository;
import com.shrishailacademy.repository.EnrollmentRepository;
import com.shrishailacademy.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class AttendanceService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceService.class);

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    /**
     * Mark attendance for multiple students in a course for a given date (Admin
     * only)
     */
    public List<Attendance> markAttendance(AttendanceRequest request, Long adminId) {
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin user not found"));

        List<Attendance> savedRecords = new ArrayList<>();

        for (AttendanceRequest.StudentAttendance record : request.getRecords()) {
            User student = userRepository.findById(record.getStudentId())
                    .orElseThrow(() -> new RuntimeException("Student not found: ID " + record.getStudentId()));

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
                throw new RuntimeException("Invalid attendance status: " + record.getStatus());
            }

            // Update existing or create new
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

        log.info("Attendance marked for course {} on {}: {} records", course.getTitle(), request.getDate(),
                savedRecords.size());
        return savedRecords;
    }

    /**
     * Get attendance records for a student in a specific course
     */
    public List<Attendance> getStudentCourseAttendance(Long userId, Long courseId) {
        return attendanceRepository.findByUserIdAndCourseId(userId, courseId);
    }

    /**
     * Get all attendance records for a student
     */
    public List<Attendance> getStudentAttendance(Long userId) {
        return attendanceRepository.findByUserId(userId);
    }

    /**
     * Get attendance for a course on a specific date
     */
    public List<Attendance> getCourseAttendanceByDate(Long courseId, LocalDate date) {
        return attendanceRepository.findByCourseIdAndAttendanceDate(courseId, date);
    }

    /**
     * Get all attendance records for a course
     */
    public List<Attendance> getCourseAttendance(Long courseId) {
        return attendanceRepository.findByCourseId(courseId);
    }

    /**
     * Get all attendance records (admin)
     */
    public List<Attendance> getAllAttendance() {
        return attendanceRepository.findAll();
    }

    /**
     * Get attendance summary (present/absent/late counts) for a student in a course
     */
    public Map<String, Object> getAttendanceSummary(Long userId, Long courseId) {
        List<Object[]> rawSummary = attendanceRepository.getAttendanceSummary(userId, courseId);
        Map<String, Object> summary = new HashMap<>();
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

    /**
     * Get attendance for a date range
     */
    public List<Attendance> getAttendanceByDateRange(Long courseId, LocalDate start, LocalDate end) {
        return attendanceRepository.findByCourseIdAndAttendanceDateBetween(courseId, start, end);
    }
}
