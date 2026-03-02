/**
 * Authentication Helper
 * Handles login, logout, and user session management
 */

const Auth = {
  /**
   * Check if user is logged in
   */
  isLoggedIn() {
    return localStorage.getItem("user") !== null;
  },

  /**
   * Get current user data from localStorage
   */
  getCurrentUser() {
    const user = localStorage.getItem("user");
    return user ? JSON.parse(user) : null;
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
    try {
      const response = await API.login(email, password);

      // Store JWT for Bearer-token API calls (optional; cookies still work too).
      if (response && response.token) {
        localStorage.setItem("token", response.token);
      } else {
        localStorage.removeItem("token");
      }

      // Store only user profile; JWT is now managed via HttpOnly cookie.
      localStorage.setItem(
        "user",
        JSON.stringify({
          id: response.id,
          name: response.name,
          email: response.email,
          role: response.role,
        }),
      );

      return response;
    } catch (error) {
      throw error;
    }
  },

  /**
   * Register new user
   */
  async register(userData) {
    try {
      const response = await API.register(userData);

      if (response && response.token) {
        localStorage.setItem("token", response.token);
      } else {
        localStorage.removeItem("token");
      }

      // Store only user profile; JWT is now managed via HttpOnly cookie.
      localStorage.setItem(
        "user",
        JSON.stringify({
          id: response.id,
          name: response.name,
          email: response.email,
          role: response.role,
        }),
      );

      return response;
    } catch (error) {
      throw error;
    }
  },

  /**
   * Logout user
   */
  logout() {
    API.logout().catch(() => {});
    localStorage.removeItem("token");
    localStorage.removeItem("user");
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
