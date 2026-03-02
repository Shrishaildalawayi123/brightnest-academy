package com.shrishailacademy.service;

import com.shrishailacademy.dto.AttendanceRequest;
import com.shrishailacademy.exception.AccessDeniedException;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
        if (request == null) {
            throw new BusinessException("Attendance request is required");
        }
        if (request.getRecords() == null || request.getRecords().isEmpty()) {
            throw new BusinessException("At least one attendance record is required");
        }
        if (request.getRecords().size() > 500) {
            throw new BusinessException("Too many attendance records in one request");
        }

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", request.getCourseId()));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", adminId));

        if (!admin.isAdmin()) {
            throw new AccessDeniedException("Only admins can mark attendance");
        }

        LocalDate attendanceDate = request.getDate();
        if (attendanceDate == null) {
            throw new BusinessException("Date is required");
        }

        // De-duplicate and validate student IDs (avoid ambiguous updates)
        Map<Long, AttendanceRequest.StudentAttendance> recordByStudentId = new LinkedHashMap<>();
        for (AttendanceRequest.StudentAttendance record : request.getRecords()) {
            if (record == null || record.getStudentId() == null) {
                throw new BusinessException("Student ID is required for all records");
            }
            if (recordByStudentId.putIfAbsent(record.getStudentId(), record) != null) {
                throw new BusinessException("Duplicate studentId in attendance records: " + record.getStudentId());
            }
        }

        Set<Long> studentIds = new HashSet<>(recordByStudentId.keySet());

        // Bulk fetch students
        Map<Long, User> studentsById = new HashMap<>();
        userRepository.findAllById(studentIds).forEach(u -> studentsById.put(u.getId(), u));
        for (Long studentId : studentIds) {
            if (!studentsById.containsKey(studentId)) {
                throw new ResourceNotFoundException("Student", "id", studentId);
            }
        }

        // Bulk enrollment check (active only)
        Set<Long> enrolledStudentIds = enrollmentRepository.findActiveUserIdsByCourseIdAndUserIdIn(
                course.getId(), studentIds, com.shrishailacademy.model.Enrollment.Status.CANCELLED);

        // Bulk existing attendance lookup for the date
        Map<Long, Attendance> existingByStudentId = new HashMap<>();
        if (!enrolledStudentIds.isEmpty()) {
            List<Attendance> existing = attendanceRepository.findByCourseIdAndAttendanceDateAndUserIdIn(
                    course.getId(), attendanceDate, enrolledStudentIds);
            for (Attendance attendance : existing) {
                if (attendance.getUser() != null) {
                    existingByStudentId.put(attendance.getUser().getId(), attendance);
                }
            }
        }

        List<Attendance> toSave = new ArrayList<>();
        int skippedNotEnrolled = 0;

        for (Map.Entry<Long, AttendanceRequest.StudentAttendance> entry : recordByStudentId.entrySet()) {
            Long studentId = entry.getKey();
            AttendanceRequest.StudentAttendance record = entry.getValue();

            if (!enrolledStudentIds.contains(studentId)) {
                skippedNotEnrolled++;
                log.warn("Student {} is not enrolled in course {}, skipping", studentId, course.getId());
                continue;
            }

            User student = studentsById.get(studentId);
            Attendance.Status status = parseStatus(record.getStatus());
            String remarks = normalizeRemarks(record.getRemarks());

            Attendance attendance = existingByStudentId.get(studentId);
            if (attendance == null) {
                attendance = new Attendance();
                attendance.setUser(student);
                attendance.setCourse(course);
                attendance.setAttendanceDate(attendanceDate);
            } else {
                // Ensure we keep attached references to avoid lazy-loading issues after tx
                attendance.setUser(student);
                attendance.setCourse(course);
            }

            attendance.setStatus(status);
            attendance.setRemarks(remarks);
            attendance.setMarkedBy(admin);
            toSave.add(attendance);
        }

        List<Attendance> savedRecords = attendanceRepository.saveAll(toSave);

        log.info("ATTENDANCE_MARKED: course='{}' date={} records={}", course.getTitle(), request.getDate(),
                savedRecords.size());
        if (skippedNotEnrolled > 0) {
            log.info("ATTENDANCE_SKIPPED_NOT_ENROLLED: courseId={} date={} skipped={}", course.getId(), attendanceDate,
                    skippedNotEnrolled);
        }
        return savedRecords;
    }

    @Transactional(readOnly = true)
    public List<Attendance> getStudentCourseAttendance(Long userId, Long courseId) {
        return attendanceRepository.findByUserIdAndCourseId(userId, courseId);
    }

    @Transactional(readOnly = true)
    public List<Attendance> getStudentAttendance(Long userId) {
        return attendanceRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Attendance> getCourseAttendanceByDate(Long courseId, LocalDate date) {
        return attendanceRepository.findByCourseIdAndAttendanceDate(courseId, date);
    }

    @Transactional(readOnly = true)
    public List<Attendance> getCourseAttendance(Long courseId) {
        return attendanceRepository.findByCourseId(courseId);
    }

    @Transactional(readOnly = true)
    public List<Attendance> getAllAttendance() {
        return attendanceRepository.findAll();
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public List<Attendance> getAttendanceByDateRange(Long courseId, LocalDate start, LocalDate end) {
        return attendanceRepository.findByCourseIdAndAttendanceDateBetween(courseId, start, end);
    }

    private static Attendance.Status parseStatus(String rawStatus) {
        if (rawStatus == null) {
            throw new BusinessException("Attendance status is required");
        }
        String normalized = rawStatus.trim().toUpperCase(Locale.ROOT);
        try {
            return Attendance.Status.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid attendance status: " + rawStatus);
        }
    }

    private static String normalizeRemarks(String remarks) {
        if (remarks == null) {
            return null;
        }
        String trimmed = remarks.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        // Basic normalization: remove NUL and other control chars (except tab/newline)
        String cleaned = trimmed.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "");
        if (cleaned.length() > 500) {
            cleaned = cleaned.substring(0, 500);
        }
        return cleaned;
    }
}
