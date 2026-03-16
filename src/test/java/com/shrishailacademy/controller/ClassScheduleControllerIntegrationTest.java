package com.shrishailacademy.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shrishailacademy.dto.ClassScheduleDTO;
import com.shrishailacademy.model.ClassSchedule;
import com.shrishailacademy.model.Course;
import com.shrishailacademy.model.Tenant;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.ClassScheduleRepository;
import com.shrishailacademy.repository.CourseRepository;
import com.shrishailacademy.repository.TenantRepository;
import com.shrishailacademy.repository.UserRepository;
import com.shrishailacademy.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ClassScheduleController
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ClassScheduleControllerIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private ClassScheduleRepository scheduleRepository;
    
    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TenantRepository tenantRepository;
    
    private Course testCourse;
    private User testTeacher;
    private Tenant testTenant;
    
    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        // Create or get default tenant
        testTenant = tenantRepository.findByTenantKey("default").orElseGet(() -> {
            Tenant t = new Tenant();
            t.setTenantKey("default");
            t.setName("Default Tenant");
            return tenantRepository.save(t);
        });

        // Create test course
        testCourse = new Course();
        testCourse.setTenant(testTenant);
        testCourse.setTitle("Test Mathematics");
        testCourse.setDescription("Test course");
        testCourse.setFee(new java.math.BigDecimal("1000.00"));
        testCourse = courseRepository.save(testCourse);

        // Create test teacher
        testTeacher = new User();
        testTeacher.setTenant(testTenant);
        testTeacher.setEmail("teacher@test.com");
        testTeacher.setPassword("password");
        testTeacher.setName("Test Teacher");
        testTeacher.setRole(User.Role.TEACHER);
        testTeacher = userRepository.save(testTeacher);

        TenantContext.set(testTenant.getId(), testTenant.getTenantKey());
    }

    @AfterEach
    @SuppressWarnings("unused")
    void tearDown() {
        TenantContext.clear();
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGetAllSchedules() throws Exception {
        // Create test schedule
        ClassSchedule schedule = ClassSchedule.builder()
                .tenantId(1L)
                .course(testCourse)
                .teacher(testTeacher)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 30))
                .roomNumber("Room 101")
                .maxStudents(30)
                .isActive(true)
                .build();
        scheduleRepository.save(schedule);
        
        mockMvc.perform(get("/api/v1/schedules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].courseName", is("Test Mathematics")))
                .andExpect(jsonPath("$[0].dayOfWeek", is("MONDAY")));
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateSchedule() throws Exception {
        ClassScheduleDTO dto = ClassScheduleDTO.builder()
                .courseId(testCourse.getId())
                .teacherId(testTeacher.getId())
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 30))
                .roomNumber("Room 101")
                .maxStudents(30)
                .isActive(true)
                .build();
        
        mockMvc.perform(post("/api/v1/schedules")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseName", is("Test Mathematics")))
                .andExpect(jsonPath("$.dayOfWeek", is("MONDAY")))
                .andExpect(jsonPath("$.roomNumber", is("Room 101")));
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testConflictDetection() throws Exception {
        // Create first schedule
        ClassSchedule existingSchedule = ClassSchedule.builder()
                .tenantId(1L)
                .course(testCourse)
                .teacher(testTeacher)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 30))
                .roomNumber("Room 101")
                .maxStudents(30)
                .isActive(true)
                .build();
        scheduleRepository.save(existingSchedule);
        
        // Try to create conflicting schedule
        ClassScheduleDTO conflictingDto = ClassScheduleDTO.builder()
                .courseId(testCourse.getId())
                .teacherId(testTeacher.getId())
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(10, 30)) // Overlaps with existing
                .endTime(LocalTime.of(12, 0))
                .roomNumber("Room 102")
                .maxStudents(25)
                .isActive(true)
                .build();
        
        mockMvc.perform(post("/api/v1/schedules")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conflictingDto)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    @WithMockUser(roles = "STUDENT")
    void testStudentCanViewSchedules() throws Exception {
        mockMvc.perform(get("/api/v1/schedules"))
                .andExpect(status().isOk());
    }
    
    @Test
    @WithMockUser(roles = "STUDENT")
    void testStudentCannotCreateSchedule() throws Exception {
        ClassScheduleDTO dto = ClassScheduleDTO.builder()
                .courseId(testCourse.getId())
                .teacherId(testTeacher.getId())
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 30))
                .roomNumber("Room 101")
                .maxStudents(30)
                .build();
        
        mockMvc.perform(post("/api/v1/schedules")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateSchedule() throws Exception {
        // Create schedule
        ClassSchedule schedule = ClassSchedule.builder()
                .tenantId(1L)
                .course(testCourse)
                .teacher(testTeacher)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 30))
                .roomNumber("Room 101")
                .maxStudents(30)
                .isActive(true)
                .build();
        schedule = scheduleRepository.save(schedule);
        
        // Update schedule
        ClassScheduleDTO updateDto = ClassScheduleDTO.builder()
                .courseId(testCourse.getId())
                .teacherId(testTeacher.getId())
                .dayOfWeek(DayOfWeek.TUESDAY)  // Changed day
                .startTime(LocalTime.of(14, 0))  // Changed time
                .endTime(LocalTime.of(15, 30))
                .roomNumber("Room 202")  // Changed room
                .maxStudents(25)
                .isActive(true)
                .build();
        
        mockMvc.perform(put("/api/v1/schedules/" + schedule.getId())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dayOfWeek", is("TUESDAY")))
                .andExpect(jsonPath("$.roomNumber", is("Room 202")));
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteSchedule() throws Exception {
        // Create schedule
        ClassSchedule schedule = ClassSchedule.builder()
                .tenantId(1L)
                .course(testCourse)
                .teacher(testTeacher)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 30))
                .roomNumber("Room 101")
                .maxStudents(30)
                .isActive(true)
                .build();
        schedule = scheduleRepository.save(schedule);
        
        mockMvc.perform(delete("/api/v1/schedules/" + schedule.getId())
                        .with(csrf()))
                .andExpect(status().isNoContent());
        
        // Verify soft delete
        ClassSchedule deleted = scheduleRepository.findById(schedule.getId()).orElse(null);
        assert deleted != null;
        assert !deleted.getIsActive();
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    void testGenerateSessions() throws Exception {
        // Create schedule
        ClassSchedule schedule = ClassSchedule.builder()
                .tenantId(1L)
                .course(testCourse)
                .teacher(testTeacher)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 30))
                .roomNumber("Room 101")
                .maxStudents(30)
                .isActive(true)
                .build();
        schedule = scheduleRepository.save(schedule);
        
        mockMvc.perform(post("/api/v1/schedules/" + schedule.getId() + "/generate-sessions")
                        .with(csrf())
                        .param("weeksAhead", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Generated")));
    }
}
