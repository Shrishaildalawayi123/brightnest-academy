(function () {
  Auth.requireAuth();

  const currentUser = Auth.getCurrentUser();
  const normalizedRole = ((currentUser && currentUser.role) || "").replace(
    "ROLE_",
    "",
  );

  if (!currentUser) {
    window.location.href = "/login.html";
    return;
  }

  if (!["TEACHER", "ADMIN"].includes(normalizedRole)) {
    window.location.href = "/index.html";
    return;
  }

  const ui = {
    heroSubtitle: document.getElementById("heroSubtitle"),
    todayBadge: document.getElementById("todayBadge"),
    todaySessionsCount: document.getElementById("todaySessionsCount"),
    pendingGradingCount: document.getElementById("pendingGradingCount"),
    activeCoursesCount: document.getElementById("activeCoursesCount"),
    studentsEnrolledCount: document.getElementById("studentsEnrolledCount"),
    recentSubmissionsBody: document.getElementById("recentSubmissionsBody"),
    todaySessionsList: document.getElementById("todaySessionsList"),
    logoutLink: document.getElementById("logoutLink"),
  };

  if (ui.logoutLink) {
    ui.logoutLink.addEventListener("click", (event) => {
      event.preventDefault();
      Auth.logout();
    });
  }

  ui.heroSubtitle.textContent = `Welcome back, ${currentUser.name || "Teacher"}. Here's your live classroom snapshot.`;
  ui.todayBadge.textContent = new Date().toLocaleDateString("en-IN", {
    weekday: "long",
    day: "numeric",
    month: "short",
  });

  function authHeaders() {
    return API.getHeaders(true, false);
  }

  function escapeHtml(value) {
    const div = document.createElement("div");
    div.textContent = value == null ? "" : String(value);
    return div.innerHTML;
  }

  function formatDateTime(value) {
    if (!value) {
      return "-";
    }

    return new Date(value).toLocaleString("en-IN", {
      day: "numeric",
      month: "short",
      year: "numeric",
      hour: "numeric",
      minute: "2-digit",
    });
  }

  function formatSessionWindow(session) {
    const start = session.scheduledStartTime || session.actualStartTime || "";
    const end = session.scheduledEndTime || session.actualEndTime || "";
    return [start, end].filter(Boolean).join(" - ") || "Time not set";
  }

  function statusClass(status) {
    switch (status) {
      case "IN_PROGRESS":
        return "status-in-progress";
      case "COMPLETED":
        return "status-completed";
      case "CANCELLED":
        return "status-cancelled";
      default:
        return "status-scheduled";
    }
  }

  function renderSessions(sessions) {
    ui.todaySessionsCount.textContent = sessions.length;

    if (!sessions.length) {
      ui.todaySessionsList.innerHTML =
        '<div class="empty-state-inline">No sessions scheduled for today.</div>';
      return;
    }

    ui.todaySessionsList.innerHTML = sessions
      .map(
        (session) => `
          <article class="session-item">
            <div class="session-title-row">
              <div class="session-title">${escapeHtml(session.courseName || "Class Session")}</div>
              <span class="dashboard-pill ${statusClass(session.status)}">${escapeHtml(session.status || "SCHEDULED")}</span>
            </div>
            <div class="session-meta">${escapeHtml(formatSessionWindow(session))}</div>
            <div class="session-meta">Room: ${escapeHtml(session.roomNumber || "Online / TBA")}</div>
            <div class="session-meta">Teacher: ${escapeHtml(session.teacherName || currentUser.name || "Teacher")}</div>
          </article>
        `,
      )
      .join("");
  }

  function renderSubmissions(submissions) {
    ui.pendingGradingCount.textContent = submissions.length;

    if (!submissions.length) {
      ui.recentSubmissionsBody.innerHTML =
        '<tr><td colspan="5" class="text-center">No pending grading right now.</td></tr>';
      return;
    }

    ui.recentSubmissionsBody.innerHTML = submissions
      .slice(0, 8)
      .map(
        (submission) => `
          <tr>
            <td>${escapeHtml(submission.studentName || "-")}</td>
            <td>${escapeHtml(submission.assignmentTitle || "-")}</td>
            <td>${escapeHtml(submission.courseName || "-")}</td>
            <td>${escapeHtml(formatDateTime(submission.submittedAt))}</td>
            <td class="table-actions">
              <a href="/manage-assignments.html#ungraded" class="btn btn-sm btn-primary">Grade</a>
            </td>
          </tr>
        `,
      )
      .join("");
  }

  function renderSummary(summary) {
    ui.activeCoursesCount.textContent = summary.activeCourses || 0;
    ui.studentsEnrolledCount.textContent = summary.studentsEnrolled || 0;
  }

  async function loadDashboard() {
    try {
      const [summary, sessions, submissions] = await Promise.all([
        API.request("/teacher-dashboard/summary", {
          method: "GET",
          headers: authHeaders(),
        }),
        API.request("/sessions?today=true", {
          method: "GET",
          headers: authHeaders(),
        }),
        API.request("/assignment-submissions?graded=false", {
          method: "GET",
          headers: authHeaders(),
        }),
      ]);

      renderSummary(summary || {});
      renderSessions(Array.isArray(sessions) ? sessions : []);
      renderSubmissions(Array.isArray(submissions) ? submissions : []);
    } catch (error) {
      ui.todaySessionsList.innerHTML =
        '<div class="empty-state-inline">Unable to load today\'s sessions.</div>';
      ui.recentSubmissionsBody.innerHTML =
        '<tr><td colspan="5" class="text-center">Unable to load submissions.</td></tr>';

      if (typeof showNotification === "function") {
        showNotification(error.message || "Failed to load dashboard", "error");
      }
    }
  }

  loadDashboard();
})();
