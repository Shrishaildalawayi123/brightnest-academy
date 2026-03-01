/**
 * BrightNest Academy - Main JavaScript
 * Handles UI interactions, navigation, and dynamic content
 */

// ========== Page Initialization ==========
document.addEventListener("DOMContentLoaded", () => {
  initializeApp();
});

function initializeApp() {
  // Set current year in footer
  const yearElement = document.getElementById("currentYear");
  if (yearElement) {
    yearElement.textContent = new Date().getFullYear();
  }

  // Initialize mobile menu
  initializeMobileMenu();

  // Initialize scroll effects
  initializeScrollEffects();

  // Initialize form validations
  initializeForms();

  // Update navigation based on auth status
  updateNavigation();

  console.log("🎓 BrightNest Academy initialized");
}

// ========== Mobile Menu ==========
function initializeMobileMenu() {
  const menuToggle = document.getElementById("menuToggle");
  const navLinks = document.getElementById("navLinks");

  if (menuToggle && navLinks) {
    menuToggle.addEventListener("click", () => {
      navLinks.classList.toggle("active");
      menuToggle.classList.toggle("active");
    });

    // Close menu when clicking a link (but not dropdown parent)
    navLinks.querySelectorAll(".nav-link").forEach((link) => {
      link.addEventListener("click", (e) => {
        // If this is a dropdown parent on mobile, toggle dropdown instead
        const parentDropdown = link.closest(".nav-dropdown");
        if (parentDropdown && window.innerWidth <= 768) {
          e.preventDefault();
          parentDropdown.classList.toggle("open");
          return;
        }
        navLinks.classList.remove("active");
        menuToggle.classList.remove("active");
      });
    });

    // Close mobile menu when clicking outside
    document.addEventListener("click", (e) => {
      if (
        navLinks.classList.contains("active") &&
        !navLinks.contains(e.target) &&
        !menuToggle.contains(e.target)
      ) {
        navLinks.classList.remove("active");
        menuToggle.classList.remove("active");
      }
    });
  }
}

// ========== Scroll Effects ==========
function initializeScrollEffects() {
  const header = document.getElementById("header");

  if (header) {
    window.addEventListener("scroll", () => {
      if (window.scrollY > 50) {
        header.classList.add("scrolled");
      } else {
        header.classList.remove("scrolled");
      }
    });
  }

  // Smooth scroll for anchor links
  document.querySelectorAll('a[href^="#"]').forEach((anchor) => {
    anchor.addEventListener("click", function (e) {
      e.preventDefault();
      const target = document.querySelector(this.getAttribute("href"));
      if (target) {
        target.scrollIntoView({
          behavior: "smooth",
          block: "start",
        });
      }
    });
  });
}

// ========== Form Validation ==========
function initializeForms() {
  const forms = document.querySelectorAll("form[data-validate]");

  forms.forEach((form) => {
    form.addEventListener("submit", (e) => {
      if (!validateForm(form)) {
        e.preventDefault();
      }
    });

    // Real-time validation
    const inputs = form.querySelectorAll("input, textarea");
    inputs.forEach((input) => {
      input.addEventListener("blur", () => validateField(input));
      input.addEventListener("input", () => clearFieldError(input));
    });
  });
}

function validateForm(form) {
  let isValid = true;
  const inputs = form.querySelectorAll("input[required], textarea[required]");

  inputs.forEach((input) => {
    if (!validateField(input)) {
      isValid = false;
    }
  });

  return isValid;
}

function validateField(field) {
  const value = field.value.trim();
  const type = field.type;
  let isValid = true;
  let errorMessage = "";

  // Required validation
  if (field.hasAttribute("required") && !value) {
    isValid = false;
    errorMessage = "This field is required";
  }

  // Email validation
  else if (type === "email" && value) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(value)) {
      isValid = false;
      errorMessage = "Please enter a valid email address";
    }
  }

  // Phone validation
  else if (type === "tel" && value) {
    const phoneRegex = /^[\d\s\+\-\(\)]+$/;
    if (!phoneRegex.test(value)) {
      isValid = false;
      errorMessage = "Please enter a valid phone number";
    }
  }

  // Password validation
  else if (type === "password" && value) {
    const passwordRegex =
      /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&#]).{8,}$/;
    if (!passwordRegex.test(value)) {
      isValid = false;
      errorMessage =
        "Password must be 8+ chars with uppercase, lowercase, number, and special character";
    }
  }

  if (!isValid) {
    showFieldError(field, errorMessage);
  } else {
    clearFieldError(field);
  }

  return isValid;
}

function showFieldError(field, message) {
  clearFieldError(field);

  field.classList.add("error");
  const errorDiv = document.createElement("div");
  errorDiv.className = "field-error";
  errorDiv.textContent = message;
  errorDiv.style.color = "#EF4444";
  errorDiv.style.fontSize = "0.875rem";
  errorDiv.style.marginTop = "0.25rem";

  field.parentNode.appendChild(errorDiv);
}

function clearFieldError(field) {
  field.classList.remove("error");
  const errorDiv = field.parentNode.querySelector(".field-error");
  if (errorDiv) {
    errorDiv.remove();
  }
}

// ========== Navigation Update Based on Auth ==========
function updateNavigation() {
  const navLinks = document.querySelector(".nav-links");
  if (!navLinks) return;

  // Check if Auth module is available and user is logged in
  const isLoggedIn = typeof Auth !== "undefined" && Auth.isLoggedIn();
  const user = isLoggedIn ? Auth.getCurrentUser() : null;

  // Find login button
  const loginButton = navLinks.querySelector('a[href="login.html"]');

  if (isLoggedIn && user) {
    // Replace login button with user menu
    if (loginButton) {
      const userMenu = document.createElement("li");
      userMenu.innerHTML = `
                <div class="user-menu" style="display:flex;gap:0.75rem;align-items:center;">
                    <a href="${user.role === "ADMIN" ? "admin-dashboard.html" : "student-dashboard.html"}" 
                       class="btn btn-primary btn-sm" style="background:var(--gradient-primary);color:white;">
                       Dashboard
                    </a>
                    <button onclick="Auth.logout()" class="btn btn-secondary btn-sm" style="border-color:var(--primary-600);color:var(--primary-600);">Logout</button>
                </div>
            `;

      loginButton.parentElement.replaceWith(userMenu);
    }
  }
}

// ========== Helper Functions ==========

/**
 * Show loading state on button
 */
function setButtonLoading(button, isLoading = true) {
  if (isLoading) {
    button.dataset.originalText = button.textContent;
    button.disabled = true;
    button.innerHTML = '<span class="loading"></span> Loading...';
  } else {
    button.disabled = false;
    button.textContent = button.dataset.originalText || "Submit";
  }
}

/**
 * Show notification/toast message
 */
function showNotification(message, type = "info") {
  const notification = document.createElement("div");
  notification.className = `notification notification-${type}`;
  notification.textContent = message;

  Object.assign(notification.style, {
    position: "fixed",
    top: "20px",
    right: "20px",
    padding: "1rem 1.5rem",
    borderRadius: "8px",
    background:
      type === "success" ? "#10B981" : type === "error" ? "#EF4444" : "#3B82F6",
    color: "white",
    fontWeight: "600",
    boxShadow: "0 10px 15px -3px rgba(0, 0, 0, 0.1)",
    zIndex: "9999",
    animation: "slideIn 0.3s ease-out",
  });

  document.body.appendChild(notification);

  setTimeout(() => {
    notification.style.animation = "slideOut 0.3s ease-in";
    setTimeout(() => notification.remove(), 300);
  }, 3000);
}

/**
 * Format date to readable string
 */
function formatDate(dateString) {
  const date = new Date(dateString);
  return date.toLocaleDateString("en-US", {
    year: "numeric",
    month: "long",
    day: "numeric",
  });
}

/**
 * Truncate text to specified length
 */
function truncateText(text, maxLength = 100) {
  if (text.length <= maxLength) return text;
  return text.substring(0, maxLength) + "...";
}

/**
 * Debounce function for performance
 */
function debounce(func, wait = 300) {
  let timeout;
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout);
      func(...args);
    };
    clearTimeout(timeout);
    timeout = setTimeout(later, wait);
  };
}

// ========== Global Error Handler ==========
window.addEventListener("error", (event) => {
  console.error("Global error:", event.error);
});

window.addEventListener("unhandledrejection", (event) => {
  console.error("Unhandled promise rejection:", event.reason);
});

// Export functions for use in other scripts
window.AppUtils = {
  setButtonLoading,
  showNotification,
  formatDate,
  truncateText,
  debounce,
};

console.log("📱 App utilities loaded");
