package com.shrishailacademy.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shrishailacademy.dto.AssignmentDTO;
import com.shrishailacademy.model.Assignment;
import com.shrishailacademy.model.Course;
import com.shrishailacademy.model.Tenant;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.AssignmentRepository;
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

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AssignmentController
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AssignmentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AssignmentRepository assignmentRepository;

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
    @WithMockUser(username = "teacher@test.com", roles = "TEACHER")
    void testCreateAssignment() throws Exception {
        AssignmentDTO dto = AssignmentDTO.builder()
                .courseId(testCourse.getId())
                .teacherId(testTeacher.getId())
                .title("Homework 1")
                .description("Complete exercises 1-10")
                .dueDate(LocalDateTime.now().plusDays(7))
                .maxScore(100)
                .isPublished(false)
                .build();

        mockMvc.perform(post("/api/v1/assignments")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Homework 1")))
                .andExpect(jsonPath("$.maxScore", is(100)))
                .andExpect(jsonPath("$.isPublished", is(false)));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void testGetAllAssignments() throws Exception {
        Assignment assignment = Assignment.builder()
                .tenantId(1L)
                .course(testCourse)
                .teacher(testTeacher)
                .title("Test Assignment")
                .description("Description")
                .dueDate(LocalDateTime.now().plusDays(7))
                .maxScore(100)
                .isPublished(true)
                .build();
        assignmentRepository.save(assignment);

        mockMvc.perform(get("/api/v1/assignments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void testPublishAssignment() throws Exception {
        Assignment assignment = Assignment.builder()
                .tenantId(1L)
                .course(testCourse)
                .teacher(testTeacher)
                .title("Test Assignment")
                .description("Description")
                .dueDate(LocalDateTime.now().plusDays(7))
                .maxScore(100)
                .isPublished(false)
                .build();
        assignment = assignmentRepository.save(assignment);

        mockMvc.perform(post("/api/v1/assignments/" + assignment.getId() + "/publish")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPublished", is(true)));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void testStudentCanViewAssignments() throws Exception {
        mockMvc.perform(get("/api/v1/assignments"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void testStudentCannotCreateAssignment() throws Exception {
        AssignmentDTO dto = AssignmentDTO.builder()
                .courseId(testCourse.getId())
                .teacherId(testTeacher.getId())
                .title("Homework 1")
                .description("Complete exercises 1-10")
                .dueDate(LocalDateTime.now().plusDays(7))
                .maxScore(100)
                .build();

        mockMvc.perform(post("/api/v1/assignments")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }
}
