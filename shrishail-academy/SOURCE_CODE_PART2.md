# 📝 Complete Source Code - Part 2: Controllers & Services

## 6. Enrollment Service
**Path**: `src/main/java/com/shrishailacademy/service/EnrollmentService.java`

```java
package com.shrishailacademy.service;

import com.shrishailacademy.model.Course;
import com.shrishailacademy.model.Enrollment;
import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.CourseRepository;
import com.shrishailacademy.repository.EnrollmentRepository;
import com.shrishailacademy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EnrollmentService {
    
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CourseRepository courseRepository;
    
    public Enrollment enrollStudent(Long userId, Long courseId) {
        if (enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new RuntimeException("Student already enrolled in this course");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));
        
        Enrollment enrollment = new Enrollment();
        enrollment.setUser(user);
        enrollment.setCourse(course);
        enrollment.setStatus(Enrollment.Status.ACTIVE);
        
        return enrollmentRepository.save(enrollment);
    }
    
    public List<Enrollment> getStudentEnrollments(Long userId) {
        return enrollmentRepository.findByUserId(userId);
    }
    
    public List<Enrollment> getAllEnrollments() {
        return enrollmentRepository.findAll();
    }
    
    public void cancelEnrollment(Long enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));
        enrollment.setStatus(Enrollment.Status.CANCELLED);
        enrollmentRepository.save(enrollment);
    }
}
```

---

## 7. User Service
**Path**: `src/main/java/com/shrishailacademy/service/UserService.java`

```java
package com.shrishailacademy.service;

import com.shrishailacademy.model.User;
import com.shrishailacademy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    public List<User> getAllStudents() {
        return userRepository.findAllStudents();
    }
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
```

---

## 8. Auth Controller
**Path**: `src/main/java/com/shrishailacademy/controller/AuthController.java`

```java
package com.shrishailacademy.controller;

import com.shrishailacademy.dto.ApiResponse;
import com.shrishailacademy.dto.AuthResponse;
import com.shrishailacademy.dto.LoginRequest;
import com.shrishailacademy.dto.RegisterRequest;
import com.shrishailacademy.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid email or password"));
        }
    }
}
```

---

## 9. Course Controller
**Path**: `src/main/java/com/shrishailacademy/controller/CourseController.java`

```java
package com.shrishailacademy.controller;

import com.shrishailacademy.dto.ApiResponse;
import com.shrishailacademy.model.Course;
import com.shrishailacademy.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@CrossOrigin(origins = "*")
public class CourseController {
    
    @Autowired
    private CourseService courseService;
    
    @GetMapping
    public ResponseEntity<List<Course>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getCourseById(@PathVariable Long id) {
        try {
            Course course = courseService.getCourseById(id);
            return ResponseEntity.ok(course);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createCourse(@Valid @RequestBody Course course) {
        try {
            Course created = courseService.createCourse(course);
            return ResponseEntity.ok(ApiResponse.success("Course created successfully", created));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateCourse(@PathVariable Long id, @RequestBody Course course) {
        try {
            Course updated = courseService.updateCourse(id, course);
            return ResponseEntity.ok(ApiResponse.success("Course updated successfully", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCourse(@PathVariable Long id) {
        try {
            courseService.deleteCourse(id);
            return ResponseEntity.ok(ApiResponse.success("Course deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
```

---

## 10. Enrollment Controller
**Path**: `src/main/java/com/shrishailacademy/controller/EnrollmentController.java`

```java
package com.shrishailacademy.controller;

import com.shrishailacademy.dto.ApiResponse;
import com.shrishailacademy.model.Enrollment;
import com.shrishailacademy.model.User;
import com.shrishailacademy.service.EnrollmentService;
import com.shrishailacademy.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
@CrossOrigin(origins = "*")
public class EnrollmentController {
    
    @Autowired
    private EnrollmentService enrollmentService;
    
    @Autowired
    private UserService userService;
    
    @PostMapping("/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> enrollInCourse(@PathVariable Long courseId, Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userService.getUserByEmail(email);
            Enrollment enrollment = enrollmentService.enrollStudent(user.getId(), courseId);
            return ResponseEntity.ok(ApiResponse.success("Enrolled successfully", enrollment));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/my-courses")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getMyEnrollments(Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userService.getUserByEmail(email);
            List<Enrollment> enrollments = enrollmentService.getStudentEnrollments(user.getId());
            return ResponseEntity.ok(enrollments);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Enrollment>> getAllEnrollments() {
        return ResponseEntity.ok(enrollmentService.getAllEnrollments());
    }
    
    @DeleteMapping("/{enrollmentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STUDENT')")
    public ResponseEntity<?> cancelEnrollment(@PathVariable Long enrollmentId) {
        try {
            enrollmentService.cancelEnrollment(enrollmentId);
            return ResponseEntity.ok(ApiResponse.success("Enrollment cancelled"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
```

---

## 11. User Controller
**Path**: `src/main/java/com/shrishailacademy/controller/UserController.java`

```java
package com.shrishailacademy.controller;

import com.shrishailacademy.dto.ApiResponse;
import com.shrishailacademy.model.User;
import com.shrishailacademy.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        try {
            String email = authentication.getName();
            User user = userService.getUserByEmail(email);
            // Don't return password
            user.setPassword(null);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/students")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllStudents() {
        List<User> students = userService.getAllStudents();
        // Remove passwords
        students.forEach(student -> student.setPassword(null));
        return ResponseEntity.ok(students);
    }
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        // Remove passwords
        users.forEach(user -> user.setPassword(null));
        return ResponseEntity.ok(users);
    }
}
```

---

**🎉 All Backend Source Code Complete!**

These files complete the Spring Boot backend implementation with:
- ✅ JWT Authentication
- ✅ Role-based Access Control
- ✅ Course Management
- ✅ Enrollment System
- ✅ User Management
- ✅ RESTful APIs

**Next**: Copy each code block to the specified file path and the backend will be ready to run!
