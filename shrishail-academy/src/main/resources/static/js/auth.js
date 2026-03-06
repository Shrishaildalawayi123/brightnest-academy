/**
 * Authentication Helper
 * Handles login, logout, and user session management
 */

const Auth = {
  getToken() {
    const token = localStorage.getItem("token");
    return token && token.trim() ? token.trim() : null;
  },

  /**
   * Check if user is logged in
   */
  isLoggedIn() {
    return this.getToken() !== null && this.getCurrentUser() !== null;
  },

  /**
   * Get current user data from localStorage
   */
  getCurrentUser() {
    const user = localStorage.getItem("user");
    if (!user) {
      return null;
    }

    try {
      return JSON.parse(user);
    } catch (_) {
      localStorage.removeItem("user");
      return null;
    }
  },

  setSession(response) {
    if (!response || !response.token) {
      throw new Error("Login response did not include a JWT token.");
    }

    localStorage.setItem("token", response.token);
    localStorage.setItem(
      "user",
      JSON.stringify({
        id: response.id,
        name: response.name,
        email: response.email,
        role: response.role,
      }),
    );
  },

  clearSession() {
    localStorage.removeItem("token");
    localStorage.removeItem("user");
  },

  /**
   * Check if current user is admin
   */
  isAdmin() {
    const user = this.getCurrentUser();
    return user && (user.role === "ADMIN" || user.role === "ROLE_ADMIN");
  },

  /**
   * Check if current user is student
   */
  isStudent() {
    const user = this.getCurrentUser();
    return user && (user.role === "STUDENT" || user.role === "ROLE_STUDENT");
  },

  /**
   * Login user
   */
  async login(email, password) {
    const response = await API.login(email, password);
    this.setSession(response);
    return response;
  },

  /**
   * Register new user
   */
  async register(userData) {
    const response = await API.register(userData);
    this.setSession(response);
    return response;
  },

  /**
   * Logout user
   */
  logout() {
    API.logout().catch(() => {});
    this.clearSession();
    window.location.href = "index.html";
  },

  /**
   * Redirect if not logged in
   */
  requireAuth() {
    if (!this.isLoggedIn()) {
      window.location.href = "login.html";
    }
  },

  /**
   * Redirect if not admin
   */
  requireAdmin() {
    if (!this.isLoggedIn() || !this.isAdmin()) {
      window.location.href = "index.html";
    }
  },

  /**
   * Redirect to appropriate dashboard
   */
  redirectToDashboard() {
    if (this.isAdmin()) {
      window.location.href = "admin-dashboard.html";
    } else if (this.isStudent()) {
      window.location.href = "student-dashboard.html";
    }
  },
};

// Export for use in other files
window.Auth = Auth;
