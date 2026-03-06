(function () {
  function showError(message) {
    const errorAlert = document.getElementById("errorAlert");
    const successAlert = document.getElementById("successAlert");
    if (successAlert) {
      successAlert.style.display = "none";
    }
    if (errorAlert) {
      errorAlert.textContent = message;
      errorAlert.style.display = "block";
    }
  }

  function showSuccess(message) {
    const errorAlert = document.getElementById("errorAlert");
    const successAlert = document.getElementById("successAlert");
    if (errorAlert) {
      errorAlert.style.display = "none";
    }
    if (successAlert) {
      successAlert.textContent = message;
      successAlert.style.display = "block";
    }
  }

  function isLocalDevHost() {
    const { hostname, protocol } = window.location;
    return (
      protocol === "file:" ||
      hostname === "localhost" ||
      hostname === "127.0.0.1"
    );
  }

  function isDebugEnabled() {
    const storedFlag = localStorage.getItem("authDebug");
    if (storedFlag === "true") {
      return true;
    }
    if (storedFlag === "false") {
      return false;
    }
    return isLocalDevHost();
  }

  function debugLog(eventName, details) {
    if (!isDebugEnabled()) {
      return;
    }

    if (typeof details === "undefined") {
      console.info("[auth-debug]", eventName);
      return;
    }

    console.info("[auth-debug]", eventName, details);
  }

  function ensureDebugBanner() {
    if (!isDebugEnabled() || document.getElementById("authDebugBanner")) {
      return;
    }

    const banner = document.createElement("div");
    banner.id = "authDebugBanner";
    banner.textContent =
      "Local auth debug is enabled. Set localStorage.authDebug = 'false' to hide this banner.";
    banner.style.cssText = [
      "position:sticky",
      "top:0",
      "z-index:1000",
      "padding:0.65rem 1rem",
      "background:#FFF7ED",
      "border-bottom:1px solid #FDBA74",
      "color:#9A3412",
      "font:600 0.9rem/1.4 sans-serif",
      "text-align:center",
    ].join(";");
    document.body.prepend(banner);
  }

  function redirectToDashboard(response) {
    const isAdmin =
      response && (response.role === "ADMIN" || response.role === "ROLE_ADMIN");
    window.location.href = isAdmin
      ? "/admin-dashboard.html"
      : "/student-dashboard.html";
  }

  async function restoreExistingSession() {
    if (!window.Auth || !window.API || !Auth.isLoggedIn()) {
      return;
    }

    try {
      debugLog("restore-session:start");
      await API.getCurrentUser();
      debugLog("restore-session:success", Auth.getCurrentUser());
      redirectToDashboard(Auth.getCurrentUser());
    } catch (_) {
      debugLog("restore-session:failed");
      Auth.clearSession();
    }
  }

  async function onSubmit(event) {
    event.preventDefault();

    const emailInput = document.getElementById("email");
    const passwordInput = document.getElementById("password");
    const loginButton = document.getElementById("loginBtn");

    const email = emailInput.value.trim();
    const password = passwordInput.value;

    debugLog("login:submit", { email });

    loginButton.disabled = true;
    loginButton.textContent = "Signing in...";

    try {
      const response = await Auth.login(email, password);
      debugLog("login:success", { email, role: response && response.role });
      showSuccess("Login successful! Redirecting...");
      setTimeout(() => redirectToDashboard(response), 400);
    } catch (error) {
      console.error("Login request failed:", error);
      const message = error && error.message ? error.message : "Login failed.";
      debugLog("login:error", { email, message });

      if (
        message.includes("Failed to fetch") ||
        message.includes("NetworkError")
      ) {
        showError(
          "Cannot reach the backend. Verify the Spring Boot server is running.",
        );
      } else if (message.includes("Too many")) {
        showError("Too many login attempts. Please wait and try again.");
      } else {
        showError(message);
      }
    } finally {
      loginButton.disabled = false;
      loginButton.textContent = "Sign In";
    }
  }

  document.addEventListener("DOMContentLoaded", () => {
    ensureDebugBanner();
    debugLog("page:ready", {
      path: window.location.pathname || window.location.href,
    });

    const footerEl = document.getElementById("footerPlaceholder");
    if (footerEl && window.getFooterHTML) {
      footerEl.innerHTML = getFooterHTML();
    }

    const loginForm = document.getElementById("loginForm");
    if (!loginForm || !window.Auth || !window.API) {
      return;
    }

    loginForm.addEventListener("submit", onSubmit);
    restoreExistingSession();
  });
})();
