package com.shrishailacademy.integration;

import com.shrishailacademy.model.Tenant;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.TenantRepository;
import com.shrishailacademy.repository.UserRepository;
import com.shrishailacademy.security.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full admin CRUD lifecycle tests: course management, blog, enrollment.
 * Uses JwtTokenProvider to create admin tokens for H2 test environment.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminCrudLifecycleIntegrationTest {

    private static final String TENANT = "default";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminBearerToken;
    private String studentBearerToken;
    private Long tenantId;

    @BeforeEach
    void setup() throws Exception {
        // Ensure default tenant exists
        Tenant tenant = tenantRepository.findByTenantKey(TENANT).orElseGet(() -> {
            Tenant t = new Tenant();
            t.setTenantKey(TENANT);
            t.setName("Default Tenant");
            return tenantRepository.save(t);
        });
        tenantId = tenant.getId();

        // Create admin user in DB
        String adminEmail = "lifecycle-admin-" + UUID.randomUUID() + "@example.com";
        User admin = new User();
        admin.setTenant(tenant);
        admin.setName("Lifecycle Admin");
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode("Admin@123!"));
        admin.setPhone("9999999999");
        admin.setRole(User.Role.ADMIN);
        userRepository.save(admin);

        adminBearerToken = jwtTokenProvider.generateTokenFromUsername(
                adminEmail, "ROLE_ADMIN", tenantId);

        // Register student via API and extract token from response
        String studentEmail = "lifecycle-student-" + UUID.randomUUID() + "@example.com";
        MvcResult studentReg = mockMvc.perform(post("/api/auth/register")
                .header("X-Tenant-ID", TENANT)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Lifecycle Student","email":"%s","password":"Student@123!","phone":"8888888888"}
                        """.formatted(studentEmail)))
                .andExpect(status().isOk())
                .andReturn();
        String studentJson = studentReg.getResponse().getContentAsString();
        studentBearerToken = com.jayway.jsonpath.JsonPath.parse(studentJson).read("$.token", String.class);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder withAdmin(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req) {
        return req.header("X-Tenant-ID", TENANT)
                .header("Authorization", "Bearer " + adminBearerToken);
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder withStudent(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req) {
        return req.header("X-Tenant-ID", TENANT)
                .header("Authorization", "Bearer " + studentBearerToken);
    }

    // ===== COURSE CRUD =====

    @Test
    void adminShouldCreateUpdateAndDeleteCourse() throws Exception {
        // Create
        MvcResult createResult = mockMvc.perform(withAdmin(post("/api/courses"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        """
                                {"title":"Integration Test Course","description":"A test course","duration":"3 months","fee":2500}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Integration Test Course"))
                .andReturn();

        String json = createResult.getResponse().getContentAsString();
        Long courseId = com.jayway.jsonpath.JsonPath.parse(json).read("$.data.id", Long.class);

        // Read by ID
        mockMvc.perform(get("/api/courses/" + courseId)
                .header("X-Tenant-ID", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Integration Test Course"));

        // Update
        mockMvc.perform(withAdmin(put("/api/courses/" + courseId))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Updated Course Title","fee":3000}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated Course Title"));

        // Delete
        mockMvc.perform(withAdmin(delete("/api/courses/" + courseId)))
                .andExpect(status().isOk());

        // Verify deleted
        mockMvc.perform(get("/api/courses/" + courseId)
                .header("X-Tenant-ID", TENANT))
                .andExpect(status().isNotFound());
    }

    // ===== ENROLLMENT LIFECYCLE =====

    @Test
    void studentShouldEnrollAndViewCourses() throws Exception {
        // Admin creates a course
        MvcResult courseResult = mockMvc.perform(withAdmin(post("/api/courses"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"Enrollable Course","description":"Test","duration":"6 months","fee":1500}
                        """))
                .andExpect(status().isCreated())
                .andReturn();

        Long courseId = com.jayway.jsonpath.JsonPath.parse(
                courseResult.getResponse().getContentAsString()).read("$.data.id", Long.class);

        // Student enrolls
        mockMvc.perform(withStudent(post("/api/enrollments/" + courseId)))
                .andExpect(status().isOk());

        // Student views their enrollments
        mockMvc.perform(withStudent(get("/api/enrollments/my-courses")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void duplicateEnrollmentShouldBeRejected() throws Exception {
        // Create course
        MvcResult courseResult = mockMvc.perform(withAdmin(post("/api/courses"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"title":"No-Dup Course","fee":1000}
                        """))
                .andExpect(status().isCreated())
                .andReturn();

        Long courseId = com.jayway.jsonpath.JsonPath.parse(
                courseResult.getResponse().getContentAsString()).read("$.data.id", Long.class);

        // First enrollment
        mockMvc.perform(withStudent(post("/api/enrollments/" + courseId)))
                .andExpect(status().isOk());

        // Duplicate enrollment — should fail
        mockMvc.perform(withStudent(post("/api/enrollments/" + courseId)))
                .andExpect(status().isConflict());
    }

    // ===== BLOG CRUD =====

    @Test
    void adminShouldManageBlogPosts() throws Exception {
        // Create post
        MvcResult createResult = mockMvc.perform(withAdmin(post("/api/blog"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title":"Test Blog Post",
                          "content":"This is test content for the blog post.",
                          "category":"LEARNING_TIPS",
                          "author":"Test Admin",
                          "published":true
                        }
                        """))
                .andExpect(status().isCreated())
                .andReturn();

        Long postId = com.jayway.jsonpath.JsonPath.parse(
                createResult.getResponse().getContentAsString()).read("$.data.id", Long.class);

        // Public blog list
        mockMvc.perform(get("/api/blog")
                .header("X-Tenant-ID", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        // Admin can view all posts
        mockMvc.perform(withAdmin(get("/api/blog/all")))
                .andExpect(status().isOk());

        // Toggle publish
        mockMvc.perform(withAdmin(put("/api/blog/" + postId + "/publish")))
                .andExpect(status().isOk());

        // Delete
        mockMvc.perform(withAdmin(delete("/api/blog/" + postId)))
                .andExpect(status().isOk());
    }

    // ===== USER ENDPOINTS =====

    @Test
    void authenticatedUserShouldAccessMeEndpoint() throws Exception {
        mockMvc.perform(withStudent(get("/api/users/me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    void adminShouldListAllUsers() throws Exception {
        mockMvc.perform(withAdmin(get("/api/users")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void adminShouldListStudents() throws Exception {
        mockMvc.perform(withAdmin(get("/api/users/students")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
