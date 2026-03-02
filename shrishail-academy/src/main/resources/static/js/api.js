/**
 * API Service - Handles all HTTP requests to backend
 * Base URL: /api  (relative - works from any port since served by Spring Boot)
 */

function resolveApiBaseUrl() {
  // Optional overrides for non-Spring-hosted frontend dev.
  // - window.__API_BASE_URL__ = "http://localhost:8080/api"
  // - localStorage.apiBaseUrl = "http://localhost:8080/api"
  const fromWindow = window.__API_BASE_URL__;
  if (typeof fromWindow === "string" && fromWindow.trim())
    return fromWindow.trim().replace(/\/$/, "");

  const fromStorage = localStorage.getItem("apiBaseUrl");
  if (typeof fromStorage === "string" && fromStorage.trim()) {
    return fromStorage.trim().replace(/\/$/, "");
  }

  if (window.location && window.location.protocol === "file:") {
    return "http://localhost:8080/api";
  }

  return "/api";
}

const API_BASE_URL = resolveApiBaseUrl();

const API = {
  getCookie(name) {
    const cookie = document.cookie
      .split("; ")
      .find((row) => row.startsWith(name + "="));
    return cookie ? decodeURIComponent(cookie.split("=")[1]) : null;
  },

  getCsrfToken() {
    return this.getCookie("XSRF-TOKEN");
  },

  getHeaders(includeAuth = false) {
    const headers = { "Content-Type": "application/json" };
    if (includeAuth) {
      const token = localStorage.getItem("token");
      if (token && token.trim()) {
        headers["Authorization"] = `Bearer ${token.trim()}`;
      }
    }
    return headers;
  },

  async request(endpoint, options = {}) {
    const url = `${API_BASE_URL}${endpoint}`;
    const method = (options.method || "GET").toUpperCase();
    const headers = {
      ...(options.headers || {}),
    };

    const csrfProtectedMethods = ["POST", "PUT", "PATCH", "DELETE"];
    if (csrfProtectedMethods.includes(method)) {
      const csrfToken = this.getCsrfToken();
      if (csrfToken) {
        headers["X-CSRF-Token"] = csrfToken;
      }
    }

    try {
      const response = await fetch(url, {
        credentials: "include",
        ...options,
        method,
        headers,
      });

      if (response.status === 401) {
        localStorage.removeItem("user");
        // Clean up any legacy token if it exists.
        localStorage.removeItem("token");
      }

      if (!response.ok) {
        let errMsg = "Request failed";
        try {
          const errBody = await response.json();
          errMsg = errBody.message || errBody.error || errMsg;
        } catch (_) {}
        throw new Error(errMsg);
      }
      // Handle empty responses (e.g. DELETE)
      const text = await response.text();
      if (!text || text.trim() === "") return null;
      return JSON.parse(text);
    } catch (e) {
      if (e.name === "SyntaxError") {
        console.error("JSON parse error for", url, e);
        throw new Error("Server returned invalid response");
      }
      throw e;
    }
  },

  // ===== AUTH =====
  async register(data) {
    return this.request("/auth/register", {
      method: "POST",
      headers: this.getHeaders(),
      body: JSON.stringify(data),
    });
  },
  async login(email, password) {
    return this.request("/auth/login", {
      method: "POST",
      headers: this.getHeaders(),
      body: JSON.stringify({ email, password }),
    });
  },
  async logout() {
    return this.request("/auth/logout", {
      method: "POST",
      headers: this.getHeaders(),
    });
  },

  // ===== COURSES =====
  async getCourses() {
    return this.request("/courses", {
      method: "GET",
      headers: this.getHeaders(),
    });
  },
  async getCourse(id) {
    return this.request(`/courses/${id}`, {
      method: "GET",
      headers: this.getHeaders(),
    });
  },
  async createCourse(courseData) {
    return this.request("/courses", {
      method: "POST",
      headers: this.getHeaders(true),
      body: JSON.stringify(courseData),
    });
  },
  async updateCourse(id, courseData) {
    return this.request(`/courses/${id}`, {
      method: "PUT",
      headers: this.getHeaders(true),
      body: JSON.stringify(courseData),
    });
  },
  async deleteCourse(id) {
    return this.request(`/courses/${id}`, {
      method: "DELETE",
      headers: this.getHeaders(true),
    });
  },

  // ===== ENROLLMENTS =====
  async enrollInCourse(courseId) {
    return this.request(`/enrollments/${courseId}`, {
      method: "POST",
      headers: this.getHeaders(true),
    });
  },
  async getMyEnrollments() {
    return this.request("/enrollments/my-courses", {
      method: "GET",
      headers: this.getHeaders(true),
    });
  },
  async getAllEnrollments() {
    return this.request("/enrollments", {
      method: "GET",
      headers: this.getHeaders(true),
    });
  },
  async cancelEnrollment(enrollmentId) {
    return this.request(`/enrollments/${enrollmentId}`, {
      method: "DELETE",
      headers: this.getHeaders(true),
    });
  },

  // ===== USERS =====
  async getCurrentUser() {
    return this.request("/users/me", {
      method: "GET",
      headers: this.getHeaders(true),
    });
  },
  async getAllStudents() {
    return this.request("/users/students", {
      method: "GET",
      headers: this.getHeaders(true),
    });
  },
  async getAllUsers() {
    return this.request("/users", {
      method: "GET",
      headers: this.getHeaders(true),
    });
  },

  // ===== BLOG (Public) =====
  async getBlogPosts(category) {
    const q = category ? `?category=${encodeURIComponent(category)}` : "";
    return this.request(`/blog${q}`, {
      method: "GET",
      headers: this.getHeaders(),
    });
  },
  async getBlogPost(slug) {
    return this.request(`/blog/${encodeURIComponent(slug)}`, {
      method: "GET",
      headers: this.getHeaders(),
    });
  },
  async getBlogCategories() {
    return this.request("/blog/categories", {
      method: "GET",
      headers: this.getHeaders(),
    });
  },

  // ===== BLOG (Admin) =====
  async getAllBlogPosts() {
    return this.request("/blog/all", {
      method: "GET",
      headers: this.getHeaders(true),
    });
  },
  async createBlogPost(data) {
    return this.request("/blog", {
      method: "POST",
      headers: this.getHeaders(true),
      body: JSON.stringify(data),
    });
  },
  async updateBlogPost(id, data) {
    return this.request(`/blog/${id}`, {
      method: "PUT",
      headers: this.getHeaders(true),
      body: JSON.stringify(data),
    });
  },
  async toggleBlogPublish(id) {
    return this.request(`/blog/${id}/publish`, {
      method: "PUT",
      headers: this.getHeaders(true),
    });
  },
  async deleteBlogPost(id) {
    return this.request(`/blog/${id}`, {
      method: "DELETE",
      headers: this.getHeaders(true),
    });
  },

  // ===== DEMO BOOKING (Public) =====
  async submitDemoBooking(data) {
    return this.request("/demo-booking", {
      method: "POST",
      headers: this.getHeaders(),
      body: JSON.stringify(data),
    });
  },

  // ===== DEMO BOOKING (Admin) =====
  async getDemoBookings(status) {
    const q = status ? `?status=${encodeURIComponent(status)}` : "";
    return this.request(`/demo-booking${q}`, {
      method: "GET",
      headers: this.getHeaders(true),
    });
  },
  async updateDemoStatus(id, status) {
    return this.request(
      `/demo-booking/${id}/status?status=${encodeURIComponent(status)}`,
      { method: "PUT", headers: this.getHeaders(true) },
    );
  },
  async getDemoStats() {
    return this.request("/demo-booking/stats", {
      method: "GET",
      headers: this.getHeaders(true),
    });
  },

  // ===== TEACHER APPLICATIONS (Public) =====
  async submitTeacherApplication(data) {
    return this.request("/teacher-applications", {
      method: "POST",
      headers: this.getHeaders(),
      body: JSON.stringify(data),
    });
  },

  // ===== TEACHER APPLICATIONS (Admin) =====
  async getTeacherApplications(status) {
    const q = status ? `?status=${encodeURIComponent(status)}` : "";
    return this.request(`/teacher-applications${q}`, {
      method: "GET",
      headers: this.getHeaders(true),
    });
  },
  async updateTeacherAppStatus(id, status) {
    return this.request(
      `/teacher-applications/${id}/status?status=${encodeURIComponent(status)}`,
      { method: "PUT", headers: this.getHeaders(true) },
    );
  },
  async getTeacherAppStats() {
    return this.request("/teacher-applications/stats", {
      method: "GET",
      headers: this.getHeaders(true),
    });
  },
};

window.API = API;
