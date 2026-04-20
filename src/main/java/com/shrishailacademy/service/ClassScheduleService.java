package com.shrishailacademy.service;

import com.shrishailacademy.model.ClassSchedule;
import com.shrishailacademy.model.ClassSession;
import com.shrishailacademy.model.Course;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.ClassScheduleRepository;
import com.shrishailacademy.repository.ClassSessionRepository;
import com.shrishailacademy.repository.CourseRepository;
import com.shrishailacademy.repository.UserRepository;
import com.shrishailacademy.tenant.TenantContext;
import com.shrishailacademy.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClassScheduleService {
    
    private final ClassScheduleRepository scheduleRepository;
    private final ClassSessionRepository sessionRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    
    /**
     * Create a new class schedule with conflict detection
     */
    @Transactional
    public ClassSchedule createSchedule(ClassSchedule schedule) {
        Long tenantId = TenantContext.requireTenantId();
        schedule.setTenantId(tenantId);
        
        // Validate time order
        if (!schedule.getEndTime().isAfter(schedule.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        
        // Check for conflicts with existing schedules
        List<ClassSchedule> conflicts = scheduleRepository.findConflictingSchedules(
            tenantId,
            schedule.getTeacher().getId(),
            schedule.getDayOfWeek(),
            schedule.getStartTime(),
            schedule.getEndTime()
        );
        
        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException(
                "Schedule conflict: Teacher already has a class at this time"
            );
        }
        
        ClassSchedule saved = scheduleRepository.save(schedule);
        log.info("Created class schedule: {} for course: {} on {}", 
            saved.getId(), saved.getCourse().getTitle(), saved.getDayOfWeek());
        
        // Generate sessions for next 4 weeks
        generateSessionsForSchedule(saved, 4);
        
        return saved;
    }
    
    /**
     * Get all active schedules for current tenant
     */
    public List<ClassSchedule> getAllActiveSchedules() {
        Long tenantId = TenantContext.requireTenantId();
        return scheduleRepository.findByTenantIdAndIsActiveTrueOrderByDayOfWeekAsc(tenantId);
    }
    
    /**
     * Get schedules for a specific course
     */
    public List<ClassSchedule> getSchedulesByCourse(Long courseId) {
        Long tenantId = TenantContext.requireTenantId();
        return scheduleRepository.findByTenantIdAndCourseIdOrderByDayOfWeekAsc(tenantId, courseId);
    }
    
    /**
     * Get schedules for a specific teacher
     */
    public List<ClassSchedule> getSchedulesByTeacher(Long teacherId) {
        Long tenantId = TenantContext.requireTenantId();
        return scheduleRepository.findByTenantIdAndTeacherIdOrderByDayOfWeekAsc(tenantId, teacherId);
    }

        public ClassSchedule getScheduleById(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        return scheduleRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("ClassSchedule", "id", id));
        }

        public ClassSchedule createSchedule(Long courseId,
            Long teacherId,
            java.time.DayOfWeek dayOfWeek,
            java.time.LocalTime startTime,
            java.time.LocalTime endTime,
            String roomNumber,
            Integer maxStudents,
            Boolean isActive) {
        Long tenantId = TenantContext.requireTenantId();
        Course course = courseRepository.findByIdAndTenantId(courseId, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));
        User teacher = userRepository.findByIdAndTenantId(teacherId, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", teacherId));

        ClassSchedule schedule = ClassSchedule.builder()
            .tenantId(tenantId)
            .course(course)
            .teacher(teacher)
            .dayOfWeek(dayOfWeek)
            .startTime(startTime)
            .endTime(endTime)
            .roomNumber(roomNumber)
            .maxStudents(maxStudents)
            .isActive(Boolean.TRUE.equals(isActive) || isActive == null)
            .build();

        return createSchedule(schedule);
        }

        public ClassSchedule updateSchedule(Long id,
            Long courseId,
            Long teacherId,
            java.time.DayOfWeek dayOfWeek,
            java.time.LocalTime startTime,
            java.time.LocalTime endTime,
            String roomNumber,
            Integer maxStudents,
            Boolean isActive) {
        Long tenantId = TenantContext.requireTenantId();
        Course course = courseRepository.findByIdAndTenantId(courseId, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));
        User teacher = userRepository.findByIdAndTenantId(teacherId, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", teacherId));

        ClassSchedule updates = ClassSchedule.builder()
            .tenantId(tenantId)
            .course(course)
            .teacher(teacher)
            .dayOfWeek(dayOfWeek)
            .startTime(startTime)
            .endTime(endTime)
            .roomNumber(roomNumber)
            .maxStudents(maxStudents)
            .isActive(Boolean.TRUE.equals(isActive) || isActive == null)
            .build();

        return updateSchedule(id, updates);
        }

        @Transactional
        public void generateSessionsForScheduleId(Long scheduleId, int weeksAhead) {
        ClassSchedule schedule = getScheduleById(scheduleId);
        generateSessionsForSchedule(schedule, weeksAhead);
        }
    
    /**
     * Update an existing schedule
     */
    @Transactional
    public ClassSchedule updateSchedule(Long id, ClassSchedule updates) {
        Long tenantId = TenantContext.requireTenantId();
        
        ClassSchedule existing = scheduleRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));
        
        // Update fields
        existing.setCourse(updates.getCourse());
        existing.setTeacher(updates.getTeacher());
        existing.setDayOfWeek(updates.getDayOfWeek());
        existing.setStartTime(updates.getStartTime());
        existing.setEndTime(updates.getEndTime());
        existing.setRoomNumber(updates.getRoomNumber());
        existing.setMaxStudents(updates.getMaxStudents());
        existing.setIsActive(updates.getIsActive());
        
        return scheduleRepository.save(existing);
    }
    
    /**
     * Delete/deactivate a schedule
     */
    @Transactional
    public void deleteSchedule(Long id) {
        Long tenantId = TenantContext.requireTenantId();
        
        ClassSchedule schedule = scheduleRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new IllegalArgumentException("Schedule not found"));
        
        schedule.setIsActive(false);
        scheduleRepository.save(schedule);
        
        log.info("Deactivated class schedule: {}", id);
    }
    
    /**
     * Generate class sessions from schedule for next N weeks
     */
    @Transactional
    public void generateSessionsForSchedule(ClassSchedule schedule, int weeksAhead) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusWeeks(weeksAhead);
        
        for (LocalDate date = today; date.isBefore(endDate); date = date.plusDays(1)) {
            if (date.getDayOfWeek() == schedule.getDayOfWeek()) {
                // Check if session already exists
                if (!sessionRepository.existsByScheduleIdAndSessionDate(schedule.getId(), date)) {
                    ClassSession session = ClassSession.builder()
                        .tenantId(schedule.getTenantId())
                        .schedule(schedule)
                        .sessionDate(date)
                        .status(ClassSession.SessionStatus.SCHEDULED)
                        .build();
                    
                    sessionRepository.save(session);
                    log.debug("Generated session for schedule {} on {}", schedule.getId(), date);
                }
            }
        }
    }
    
    /**
     * Scheduled task: Generate sessions for next week (runs every Sunday at 2 AM)
     */
    @Scheduled(cron = "0 0 2 * * SUN")
    @Transactional
    public void autoGenerateWeeklySchedule() {
        log.info("Running auto-generation of weekly class sessions");
        
        List<ClassSchedule> activeSchedules = scheduleRepository.findByIsActiveTrueOrderByTenantIdAscDayOfWeekAsc();
        
        for (ClassSchedule schedule : activeSchedules) {
            try {
                generateSessionsForSchedule(schedule, 2); // Generate 2 weeks ahead
            } catch (Exception e) {
                log.error("Failed to generate sessions for schedule {}: {}", 
                    schedule.getId(), e.getMessage());
            }
        }
        
        log.info("Completed auto-generation for {} schedules", activeSchedules.size());
    }
}
