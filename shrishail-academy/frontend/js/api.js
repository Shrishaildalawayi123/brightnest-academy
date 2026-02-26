/**
 * API Service - Handles all HTTP requests to backend
 * Base URL: http://localhost:8080/api
 */

const API_BASE_URL = "/api";

const API = {
  /**
   * Get auth token from localStorage
   */
  getToken() {
    return localStorage.getItem("token");
  },

  /**
   * Get auth headers
   */
  getHeaders(includeAuth = false) {
    const headers = {
      "Content-Type": "application/json",
    };

    if (includeAuth) {
      const token = this.getToken();
      if (token) {
        headers["Authorization"] = `Bearer ${token}`;
      }
    }

    return headers;
  },

  /**
   * Make HTTP request
   */
  async request(endpoint, options = {}) {
    const url = `${API_BASE_URL}${endpoint}`;
    const response = await fetch(url, options);

    if (!response.ok) {
      const error = await response
        .json()
        .catch(() => ({ message: "Request failed" }));
      throw new Error(error.message || "Request failed");
    }

    return response.json();
  },

  // ============================================
  // Authentication APIs
  // ============================================

  /**
   * Register new student
   */
  async register(data) {
    return this.request("/auth/register", {
      method: "POST",
      headers: this.getHeaders(),
      body: JSON.stringify(data),
    });
  },

  /**
   * Login
   */
  async login(email, password) {
    return this.request("/auth/login", {
      method: "POST",
      headers: this.getHeaders(),
      body: JSON.stringify({ email, password }),
    });
  },

  // ============================================
  // Course APIs
  // ============================================

  /**
   * Get all courses (public)
   */
  async getCourses() {
    return this.request("/courses", {
      method: "GET",
      headers: this.getHeaders(),
    });
  },

  /**
   * Get course by ID
   */
  async getCourse(id) {
    return this.request(`/courses/${id}`, {
      method: "GET",
      headers: this.getHeaders(),
    });
  },

  /**
   * Create course (admin only)
   */
  async createCourse(courseData) {
    return this.request("/courses", {
      method: "POST",
      headers: this.getHeaders(true),
      body: JSON.stringify(courseData),
    });
  },

  /**
   * Update course (admin only)
   */
  async updateCourse(id, courseData) {
    return this.request(`/courses/${id}`, {
      method: "PUT",
      headers: this.getHeaders(true),
      body: JSON.stringify(courseData),
    });
  },

  /**
   * Delete course (admin only)
   */
  async deleteCourse(id) {
    return this.request(`/courses/${id}`, {
      method: "DELETE",
      headers: this.getHeaders(true),
    });
  },

  // ============================================
  // Enrollment APIs
  // ============================================

  /**
   * Enroll in course (student only)
   */
  async enrollInCourse(courseId) {
    return this.request(`/enrollments/${courseId}`, {
      method: "POST",
      headers: this.getHeaders(true),
    });
  },

  /**
   * Get student's enrolled courses
   */
  async getMyEnrollments() {
    return this.request("/enrollments/my-courses", {
      method: "GET",
      headers: this.getHeaders(true),
    });
  },

  /**
   * Get all enrollments (admin only)
   */
  async getAllEnrollments() {
    return this.request("/enrollments", {
      method: "GET",
      headers: this.getHeaders(true),
    });
  },

  /**
   * Cancel enrollment
   */
  async cancelEnrollment(enrollmentId) {
    return this.request(`/enrollments/${enrollmentId}`, {
      method: "DELETE",
      headers: this.getHeaders(true),
    });
  },

  // ============================================
  // User APIs
  // ============================================

  /**
   * Get current user profile
   */
  async getCurrentUser() {
    return this.request("/users/me", {
      method: "GET",
      headers: this.getHeaders(true),
    });
  },

  /**
   * Get all students (admin only)
   */
  async getAllStudents() {
    return this.request("/users/students", {
      method: "GET",
      headers: this.getHeaders(true),
    });
  },

  /**
   * Get all users (admin only)
   */
  async getAllUsers() {
    return this.request("/users", {
      method: "GET",
      headers: this.getHeaders(true),
    });
  },
};

// Export for use in other files
window.API = API;
