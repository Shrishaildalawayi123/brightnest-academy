package com.shrishailacademy.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shrishailacademy.model.Course;
import com.shrishailacademy.model.Tenant;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.CourseRepository;
import com.shrishailacademy.repository.UserRepository;
import com.shrishailacademy.service.TenantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthorizationAndUserCrudIntegrationTest {

        private static final String TENANT_HEADER = "X-Tenant-ID";
        private static final String DEFAULT_TENANT_KEY = "default";

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private CourseRepository courseRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private TenantService tenantService;

        @Test
        void roleViolationsShouldReturnForbiddenNotInternalServerError() throws Exception {
                String studentToken = registerAndLoginStudent();
                String adminToken = createAndLoginAdmin();

                mockMvc.perform(get("/api/users")
                                .header(TENANT_HEADER, DEFAULT_TENANT_KEY)
                                .header("Authorization", "Bearer " + studentToken))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.status").value(403));

                String createCoursePayload = """
                                {
                                  "title":"Sec Test Course %s",
                                  "description":"test",
                                  "duration":"1 month",
                                  "icon":"book",
                                  "color":"#123456",
                                  "fee":1999.00
                                }
                                """.formatted(UUID.randomUUID());

                mockMvc.perform(post("/api/courses")
                                .header(TENANT_HEADER, DEFAULT_TENANT_KEY)
                                .header("Authorization", "Bearer " + studentToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createCoursePayload))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.status").value(403));

                Tenant defaultTenant = tenantService.ensureDefaultTenantExists();
                Course course = new Course();
                course.setTenant(defaultTenant);
                course.setTitle("Enroll Target " + UUID.randomUUID());
                course.setDescription("desc");
                course.setDuration("1m");
                course.setIcon("i");
                course.setColor("#111111");
                course.setFee(new BigDecimal("1000.00"));
                course = courseRepository.save(course);

                mockMvc.perform(post("/api/enrollments/" + course.getId())
                                .header(TENANT_HEADER, DEFAULT_TENANT_KEY)
                                .header("Authorization", "Bearer " + adminToken))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        void adminCrudEndpointsShouldCreateUpdateAndDeleteUser() throws Exception {
                String adminToken = createAndLoginAdmin();
                String unique = UUID.randomUUID().toString().substring(0, 8);

                String createPayload = """
                                {
                                  "name":"Managed User",
                                  "email":"managed-%s@example.com",
                                  "password":"Strong@123",
                                  "phone":"9999999999",
                                  "role":"STUDENT"
                                }
                                """.formatted(unique);

                MvcResult createResult = mockMvc.perform(post("/api/users")
                                .header(TENANT_HEADER, DEFAULT_TENANT_KEY)
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createPayload))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.email").value("managed-" + unique + "@example.com"))
                                .andReturn();

                Map<?, ?> createBody = objectMapper.readValue(createResult.getResponse().getContentAsString(),
                                Map.class);
                Map<?, ?> data = (Map<?, ?>) createBody.get("data");
                Integer createdId = (Integer) data.get("id");

                String updatePayload = """
                                {
                                  "name":"Managed User Updated",
                                  "phone":"8888888888",
                                  "role":"ADMIN"
                                }
                                """;

                mockMvc.perform(put("/api/users/" + createdId)
                                .header(TENANT_HEADER, DEFAULT_TENANT_KEY)
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updatePayload))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.name").value("Managed User Updated"))
                                .andExpect(jsonPath("$.data.role").value("ADMIN"));

                mockMvc.perform(delete("/api/users/" + createdId)
                                .header(TENANT_HEADER, DEFAULT_TENANT_KEY)
                                .header("Authorization", "Bearer " + adminToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));

                assertTrue(userRepository.findById(createdId.longValue()).isEmpty());
        }

        @Test
        void createCourseShouldSanitizeStoredHtmlPayloads() throws Exception {
                String adminToken = createAndLoginAdmin();

                String createCoursePayload = """
                                {
                                  "title":"<script>alert(1)</script> Algebra",
                                  "description":"<img src=x onerror=alert(1)>desc",
                                  "duration":"6 months",
                                  "icon":"book",
                                  "color":"#123456",
                                  "fee":2500.00
                                }
                                """;

                MvcResult createResult = mockMvc.perform(post("/api/courses")
                                .header(TENANT_HEADER, DEFAULT_TENANT_KEY)
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createCoursePayload))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.title")
                                                .value(org.hamcrest.Matchers.containsString("&lt;script&gt;")))
                                .andReturn();

                Map<?, ?> createBody = objectMapper.readValue(createResult.getResponse().getContentAsString(),
                                Map.class);
                Map<?, ?> data = (Map<?, ?>) createBody.get("data");
                Integer createdId = (Integer) data.get("id");

                Course saved = courseRepository.findById(createdId.longValue()).orElseThrow();
                assertTrue(saved.getTitle().contains("&lt;script&gt;"));
                assertFalse(saved.getTitle().contains("<script>"));
                assertTrue(saved.getDescription().contains("&lt;img"));
                assertFalse(saved.getDescription().contains("<img"));
        }

        @Test
        void unsupportedMethodAndMissingEndpointShouldMapToProperCodes() throws Exception {
                String adminToken = createAndLoginAdmin();

                mockMvc.perform(patch("/api/users/1")
                                .header(TENANT_HEADER, DEFAULT_TENANT_KEY)
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isMethodNotAllowed())
                                .andExpect(jsonPath("$.status").value(405));

                mockMvc.perform(get("/api/not-existing-endpoint")
                                .header(TENANT_HEADER, DEFAULT_TENANT_KEY)
                                .header("Authorization", "Bearer " + adminToken))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value(404));
        }

        private String registerAndLoginStudent() throws Exception {
                String email = "student-" + UUID.randomUUID() + "@example.com";
                String password = "Student@123!";

                String registerPayload = """
                                {
                                  "name":"Student User",
                                  "email":"%s",
                                  "password":"%s",
                                  "phone":"9876543210"
                                }
                                """.formatted(email, password);

                mockMvc.perform(post("/api/auth/register")
                                .header(TENANT_HEADER, DEFAULT_TENANT_KEY)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registerPayload))
                                .andExpect(status().isOk());

                return loginAndExtractToken(email, password);
        }

        private String createAndLoginAdmin() throws Exception {
                String email = "admin-" + UUID.randomUUID() + "@example.com";
                String password = "Admin@123!";

                User admin = new User();
                admin.setTenant(tenantService.ensureDefaultTenantExists());
                admin.setName("Admin User");
                admin.setEmail(email);
                admin.setPhone("9876543210");
                admin.setRole(User.Role.ADMIN);
                admin.setPassword(passwordEncoder.encode(password));
                userRepository.save(admin);

                return loginAndExtractToken(email, password);
        }

        private String loginAndExtractToken(String email, String password) throws Exception {
                String loginPayload = """
                                {
                                  "email":"%s",
                                  "password":"%s"
                                }
                                """.formatted(email, password);

                MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                                .header(TENANT_HEADER, DEFAULT_TENANT_KEY)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginPayload))
                                .andExpect(status().isOk())
                                .andReturn();

                Map<?, ?> loginBody = objectMapper.readValue(loginResult.getResponse().getContentAsString(), Map.class);
                return (String) loginBody.get("token");
        }
}
