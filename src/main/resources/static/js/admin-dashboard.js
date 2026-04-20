function isLocalDevHost() {
  const { hostname, protocol } = window.location;
  return (
    protocol === "file:" || hostname === "localhost" || hostname === "127.0.0.1"
  );
}

function isAdminDebugEnabled() {
  const storedFlag = localStorage.getItem("authDebug");
  if (storedFlag === "true") {
    return true;
  }
  if (storedFlag === "false") {
    return false;
  }
  return isLocalDevHost();
}

function debugAdmin(eventName, details) {
  if (!isAdminDebugEnabled()) {
    return;
  }

  if (typeof details === "undefined") {
    console.info("[admin-debug]", eventName);
    return;
  }

  console.info("[admin-debug]", eventName, details);
}

function ensureAdminDebugBanner() {
  if (!isAdminDebugEnabled() || document.getElementById("adminDebugBanner")) {
    return;
  }

  const banner = document.createElement("div");
  banner.id = "adminDebugBanner";
  banner.textContent =
    "Local admin debug is enabled. Network requests will be logged to the console.";
  banner.style.cssText = [
    "position:sticky",
    "top:0",
    "z-index:1000",
    "padding:0.65rem 1rem",
    "background:#EFF6FF",
    "border-bottom:1px solid #93C5FD",
    "color:#1D4ED8",
    "font:600 0.9rem/1.4 sans-serif",
    "text-align:center",
  ].join(";");
  document.body.prepend(banner);
}

ensureAdminDebugBanner();
debugAdmin("dashboard:init", {
  path: window.location.pathname || window.location.href,
});

Auth.requireAdmin();
const user = Auth.getCurrentUser();
document.getElementById("adminName").textContent = user.name || "Admin";
document.getElementById("dateDisplay").textContent =
  new Date().toLocaleDateString("en-IN", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
  });

function toggleSidebar() {
  const sidebar = document.querySelector(".sidebar");
  const overlay = document.getElementById("sidebarOverlay");
  sidebar.classList.toggle("open");
  overlay.style.display = sidebar.classList.contains("open") ? "block" : "none";
}

function authHeaders() {
  return API.getHeaders(true);
}

// Pagination state
let studentsPage = 0;
let enrollmentsPage = 0;
let paymentsPage = 0;
const pageSize = 20;

// Pagination helper
function renderPaginationControls(containerId, currentPage, totalPages, loadFunction) {
  const container = document.getElementById(containerId);
  if (!container) {
    return;
  }

  const safeTotalPages = Math.max(totalPages || 0, 1);
  const safeCurrentPage = Math.min(Math.max(currentPage || 0, 0), safeTotalPages - 1);
  const prevDisabled = safeCurrentPage <= 0 ? "disabled" : "";
  const nextDisabled = safeCurrentPage >= safeTotalPages - 1 ? "disabled" : "";

  let html = `<div class="pagination-shell">
    <span>Page ${safeCurrentPage + 1} of ${safeTotalPages}</span>
    <button class="btn-icon" type="button" onclick="${loadFunction}(${safeCurrentPage - 1})" ${prevDisabled}>Prev</button>
    <button class="btn-icon" type="button" onclick="${loadFunction}(${safeCurrentPage + 1})" ${nextDisabled}>Next</button>
  </div>`;
  container.innerHTML = html;
}

function unpackPageResponse(payload) {
  if (Array.isArray(payload)) {
    return {
      content: payload,
      totalPages: payload.length > 0 ? 1 : 0,
      totalElements: payload.length,
      number: 0,
    };
  }

  if (payload && Array.isArray(payload.content)) {
    return {
      content: payload.content,
      totalPages: typeof payload.totalPages === "number" ? payload.totalPages : 0,
      totalElements:
        typeof payload.totalElements === "number"
          ? payload.totalElements
          : payload.content.length,
      number: typeof payload.number === "number" ? payload.number : 0,
    };
  }

  return { content: [], totalPages: 0, totalElements: 0, number: 0 };
}

function resolveApiRequestUrl(url) {
  if (typeof url !== "string") {
    return url;
  }

  if (!url.startsWith("/api/")) {
    return url;
  }

  const base = (window.API_BASE_URL || "/api").replace(/\/$/, "");
  if (base.startsWith("http://") || base.startsWith("https://")) {
    const suffix = url.substring(4); // remove leading "/api"
    return `${base}${suffix}`;
  }

  return url;
}

const nativeFetch = window.fetch.bind(window);
function getCookie(name) {
  const cookie = document.cookie
    .split("; ")
    .find((row) => row.startsWith(name + "="));
  return cookie ? decodeURIComponent(cookie.split("=")[1]) : null;
}
window.fetch = async (url, options = {}) => {
  const method = (options.method || "GET").toUpperCase();
  const headers = { ...(options.headers || {}) };
  const token = localStorage.getItem("token");
  const requestUrl =
    typeof url === "string" ? url : url && url.url ? url.url : String(url);
  const resolvedUrl = resolveApiRequestUrl(requestUrl);
  if (token && token.trim() && !headers["Authorization"]) {
    headers["Authorization"] = `Bearer ${token.trim()}`;
  }
  if (["POST", "PUT", "PATCH", "DELETE"].includes(method)) {
    const csrfToken = getCookie("XSRF-TOKEN");
    if (csrfToken) headers["X-CSRF-Token"] = csrfToken;
  }
  debugAdmin("request", { method, url: requestUrl, headers });

  let response;
  try {
    response = await nativeFetch(resolvedUrl, {
      credentials: "include",
      ...options,
      method,
      headers,
    });
  } catch (error) {
    debugAdmin("request:error", {
      method,
      url: requestUrl,
      message:
        error && error.message ? error.message : "Network request failed",
    });
    throw error;
  }

  debugAdmin("response", {
    method,
    url: resolvedUrl,
    status: response.status,
  });
  if (response.status === 401) {
    localStorage.removeItem("user");
    localStorage.removeItem("token");
    window.location.href = "/login.html";
    throw new Error("Unauthorized");
  }
  return response;
};
let deleteId = null;
const titles = {
  overview: "Overview",
  courses: "Manage Courses",
  students: "Students",
  enrollments: "Enrollments",
  attendance: "Attendance",
  payments: "Payments",
  crm: "CRM Leads",
  blog: "Blog Posts",
  demos: "Demo Bookings",
  teachers: "Teacher Applications",
};

function showToast(msg, type = "success") {
  const t = document.getElementById("toast");
  t.textContent = (type === "success" ? "Success: " : "Error: ") + msg;
  t.className = `toast ${type} show`;
  setTimeout(() => t.classList.remove("show"), 3000);
}

function showSection(name) {
  document
    .querySelectorAll(".page-section")
    .forEach((s) => s.classList.remove("active"));
  document.getElementById("section-" + name).classList.add("active");
  document
    .querySelectorAll(".sidebar-link")
    .forEach((l) => l.classList.remove("active"));
  const activeLink = document.querySelector(
    `.sidebar-link[onclick="showSection('${name}')"]`,
  );
  if (activeLink) activeLink.classList.add("active");
  document.getElementById("pageTitle").textContent = titles[name];
  if (name === "overview") loadOverview();
  if (name === "courses") loadCourses();
  if (name === "students") loadStudents();
  if (name === "enrollments") loadEnrollments();
  if (name === "attendance") loadAttendanceSection();
  if (name === "payments") loadPaymentsSection();
  if (name === "crm") loadCrmSection();
  if (name === "blog") loadBlogAdmin();
  if (name === "demos") loadDemosAdmin();
  if (name === "teachers") loadTeacherAppsAdmin();
}

function fmt(dt) {
  return dt ? new Date(dt).toLocaleDateString("en-IN") : "-";
}
function esc(str) {
  const d = document.createElement("div");
  d.textContent = str || "";
  return d.innerHTML.replace(/'/g, "&#39;");
}

function leadStatusBadgeClass(status) {
  const normalized = String(status || "").toUpperCase();
  if (["NEW", "PENDING"].includes(normalized)) return "badge-admin";
  if (["READ", "REPLIED", "SCHEDULED", "CONTACTED", "COMPLETED"].includes(normalized)) return "badge-active";
  return "badge-student";
}

function leadStatusOptions(source) {
  if (source === "contact") return ["NEW", "READ", "REPLIED", "ARCHIVED"];
  if (source === "demo") return ["PENDING", "SCHEDULED", "COMPLETED", "CANCELLED"];
  if (source === "counseling") return ["NEW", "CONTACTED", "COMPLETED", "CANCELLED"];
  if (source === "chatbot") return ["NEW", "CONTACTED", "ENROLLED", "CLOSED"];
  return [];
}

function followUpOptions() {
  return ["NONE", "PENDING", "COMPLETED", "MISSED"];
}

function crmInputId(prefix, source, sourceId) {
  return `${prefix}-${source}-${sourceId}`;
}

function fmtDateTimeLocal(dt) {
  if (!dt) return "";
  const date = new Date(dt);
  const tzOffsetMs = date.getTimezoneOffset() * 60000;
  return new Date(date.getTime() - tzOffsetMs).toISOString().slice(0, 16);
}

function crmQueryParams() {
  const params = new URLSearchParams();
  const source = document.getElementById("crmSourceFilter")?.value || "all";
  const status = document.getElementById("crmStatusFilter")?.value || "all";
  const followUpStatus =
    document.getElementById("crmFollowUpStatusFilter")?.value || "all";
  const assignee = document.getElementById("crmAssigneeFilter")?.value.trim();
  const fromDate = document.getElementById("crmFromDate")?.value;
  const toDate = document.getElementById("crmToDate")?.value;
  const query = document.getElementById("crmSearch")?.value.trim();

  params.set("source", source);
  params.set("status", status);
  params.set("followUpStatus", followUpStatus);
  if (assignee) params.set("assignee", assignee);
  if (fromDate) params.set("fromDate", fromDate);
  if (toDate) params.set("toDate", toDate);
  if (query) params.set("q", query);
  return params;
}

// Overview
async function loadOverview() {
  try {
    const [users, courses, enrollmentsPayload] = await Promise.all([
      fetch("/api/users/students", { headers: authHeaders() }).then((r) =>
        r.json(),
      ),
      fetch("/api/courses").then((r) => r.json()),
      fetch(`/api/enrollments?page=0&size=${pageSize}`, {
        headers: authHeaders(),
      }).then((r) => r.json()),
    ]);
    const enrollments = unpackPageResponse(enrollmentsPayload).content;
    const students = Array.isArray(users)
      ? users.filter((u) => u.role === "STUDENT")
      : [];
    document.getElementById("totalStudents").textContent = students.length;
    document.getElementById("totalCourses").textContent = Array.isArray(courses)
      ? courses.length
      : 0;
    document.getElementById("totalEnrollments").textContent = Array.isArray(
      enrollments,
    )
      ? enrollments.length
      : 0;
    document.getElementById("activeEnrollments").textContent = Array.isArray(
      enrollments,
    )
      ? enrollments.filter((e) => e.status === "ACTIVE").length
      : 0;
    const tbody = document.querySelector("#recentStudents tbody");
    tbody.innerHTML =
      students
        .slice(0, 5)
        .map(
          (u) => `
                    <tr><td><strong>${esc(u.name)}</strong></td><td>${esc(u.email)}</td>
                    <td><span class="badge badge-${u.role.toLowerCase()}">${u.role}</span></td>
                    <td>${fmt(u.createdAt)}</td></tr>`,
        )
        .join("") || '<tr><td colspan="4">No students yet</td></tr>';
  } catch (e) {
    console.error(e);
  }
}

// Courses
async function loadCourses() {
  const tbody = document.querySelector("#coursesTable tbody");
  try {
    const courses = await fetch("/api/courses").then((r) => r.json());
    tbody.innerHTML =
      (Array.isArray(courses) ? courses : [])
        .map(
          (c) => `
                    <tr>
                        <td style="font-size:1.5rem;">${esc(c.icon)}</td>
                        <td><strong>${esc(c.title)}</strong><br><small style="color:var(--neutral-400);">${esc(c.description || "").substring(0, 60)}...</small></td>
                        <td>Duration: ${esc(c.duration)}</td>
                        <td style="font-weight:700;color:var(--primary-700);">${c.fee ? "₹ " + Number(c.fee).toLocaleString() : "-"}</td>
                        <td>
                          <button class="btn-icon" onclick="editCourse(${c.id},'${esc(c.title)}','${esc(c.description || "")}','${esc(c.duration || "")}','${esc(c.icon || "")}','${esc(c.color || "")}',${c.fee || 0})" title="Edit">Edit</button>
                          <button class="btn-icon btn-danger" onclick="deleteCourse(${c.id},'${esc(c.title)}')" title="Delete">Delete</button>
                        </td>
                    </tr>`,
        )
        .join("") ||
      '<tr><td colspan="5" class="empty-state">No courses found</td></tr>';
  } catch (e) {
    tbody.innerHTML = '<tr><td colspan="4">Error loading courses</td></tr>';
  }
}

// Students
async function loadStudents(page = studentsPage) {
  studentsPage = page;
  const tbody = document.querySelector("#studentsTable tbody");
  try {
    const payload = await fetch(
      `/api/users?page=${studentsPage}&size=${pageSize}`,
      { headers: authHeaders() },
    ).then((r) => r.json());
    const usersPage = unpackPageResponse(payload);
    const users = usersPage.content;
    tbody.innerHTML =
      (Array.isArray(users) ? users : [])
        .map(
          (u) => `
                    <tr><td><strong>${esc(u.name)}</strong></td><td>${esc(u.email)}</td>
                    <td>${esc(u.phone || "-")}</td>
                    <td><span class="badge badge-${u.role.toLowerCase()}">${u.role}</span></td>
                    <td>
                      <div class="table-action-group">
                        <select id="roleSelect-${u.id}" class="form-input" aria-label="Change role for ${esc(u.name)}">
                          <option value="ADMIN" ${u.role === "ADMIN" ? "selected" : ""}>ADMIN</option>
                          <option value="TEACHER" ${u.role === "TEACHER" ? "selected" : ""}>TEACHER</option>
                          <option value="STUDENT" ${u.role === "STUDENT" ? "selected" : ""}>STUDENT</option>
                        </select>
                        <button class="btn-icon" type="button" onclick="changeStudentRole(${u.id}, '${esc(u.role)}')">Change Role</button>
                      </div>
                    </td></tr>`,
        )
        .join("") || '<tr><td colspan="5">No users found</td></tr>';
    renderPaginationControls(
      "studentsPagination",
      usersPage.number,
      usersPage.totalPages,
      "loadStudents",
    );
  } catch (e) {
    tbody.innerHTML = '<tr><td colspan="5">Error loading students</td></tr>';
    renderPaginationControls("studentsPagination", 0, 0, "loadStudents");
  }
}

async function changeStudentRole(userId, currentRole) {
  const roleSelect = document.getElementById(`roleSelect-${userId}`);
  const role = roleSelect ? roleSelect.value : null;
  if (!role) {
    showToast("Please select a valid role", "error");
    return;
  }

  if (role === currentRole) {
    showToast("Select a different role before updating", "error");
    return;
  }

  try {
    const res = await fetch(`/api/users/${userId}/role`, {
      method: "PATCH",
      headers: authHeaders(),
      body: JSON.stringify({ role }),
    });
    const data = await res.json();
    if (!res.ok) {
      showToast(data.message || "Failed to update role", "error");
      return;
    }
    showToast("Role updated successfully");
    loadStudents(studentsPage);
  } catch (e) {
    showToast("Error updating role", "error");
  }
}

// Enrollments
async function loadEnrollments(page = enrollmentsPage) {
  enrollmentsPage = Math.max(page, 0);
  const tbody = document.querySelector("#enrollmentsTable tbody");
  try {
    const payload = await fetch(
      `/api/enrollments?page=${enrollmentsPage}&size=${pageSize}`,
      {
      headers: authHeaders(),
      },
    ).then((r) => r.json());
    const enrollmentsPageData = unpackPageResponse(payload);
    const enrollments = enrollmentsPageData.content;
    tbody.innerHTML =
      (Array.isArray(enrollments) ? enrollments : [])
        .map(
          (e) => `
                    <tr>
                        <td><strong>${esc(e.user ? e.user.name : "-")}</strong><br><small>${esc(e.user ? e.user.email : "")}</small></td>
                        <td>${esc(e.course ? e.course.title : "-")}</td>
                        <td><span class="badge badge-active">${e.status || "ACTIVE"}</span></td>
                        <td>${fmt(e.enrolledAt)}</td>
                    </tr>`,
        )
        .join("") || '<tr><td colspan="4">No enrollments yet</td></tr>';
    renderPaginationControls(
      "enrollmentsPagination",
      enrollmentsPageData.number,
      enrollmentsPageData.totalPages,
      "loadEnrollments",
    );
  } catch (e) {
    tbody.innerHTML = '<tr><td colspan="4">Error loading enrollments</td></tr>';
    renderPaginationControls("enrollmentsPagination", 0, 0, "loadEnrollments");
  }
}

// Course modal
function openCourseModal(reset = true) {
  if (reset) {
    document.getElementById("modalTitle").textContent = "Add New Course";
    document.getElementById("courseId").value = "";
    document.getElementById("courseForm").reset();
    document.getElementById("courseColor").value = "#3B82F6";
  }
  document.getElementById("modalAlert").style.display = "none";
  document.getElementById("courseModal").classList.add("show");
}

function editCourse(id, title, desc, duration, icon, color, fee) {
  document.getElementById("modalTitle").textContent = "Edit Course";
  document.getElementById("courseId").value = id;
  document.getElementById("courseTitle").value = title;
  document.getElementById("courseDesc").value = desc;
  document.getElementById("courseDuration").value = duration;
  document.getElementById("courseIcon").value = icon;
  document.getElementById("courseColor").value = color || "#3B82F6";
  document.getElementById("courseFee").value = fee || "";
  openCourseModal(false);
}

function closeModal() {
  document.getElementById("courseModal").classList.remove("show");
}

document.getElementById("courseForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  const id = document.getElementById("courseId").value;
  const data = {
    title: document.getElementById("courseTitle").value.trim(),
    description: document.getElementById("courseDesc").value.trim(),
    duration: document.getElementById("courseDuration").value.trim(),
    icon: document.getElementById("courseIcon").value.trim(),
    color: document.getElementById("courseColor").value,
    fee: document.getElementById("courseFee").value
      ? parseFloat(document.getElementById("courseFee").value)
      : null,
  };
  const btn = document.getElementById("saveBtn");
  btn.textContent = "Saving...";
  btn.disabled = true;
  try {
    const url = id ? `/api/courses/${id}` : "/api/courses";
    const method = id ? "PUT" : "POST";
    await fetch(url, {
      method,
      headers: authHeaders(),
      body: JSON.stringify(data),
    });
    closeModal();
    showToast(id ? "Course updated!" : "Course added!");
    loadCourses();
    loadOverview();
  } catch (e) {
    const alert = document.getElementById("modalAlert");
    alert.textContent = "Failed to save course. Please try again.";
    alert.className = "alert-msg error";
    alert.style.display = "block";
  }
  btn.textContent = "Save Course";
  btn.disabled = false;
});

function deleteCourse(id, name) {
  deleteId = id;
  document.getElementById("deleteCourseName").textContent =
    `"${name}" will be permanently deleted.`;
  document.getElementById("deleteModal").classList.add("show");
}

async function confirmDelete() {
  try {
    await fetch(`/api/courses/${deleteId}`, {
      method: "DELETE",
      headers: authHeaders(),
    });
    document.getElementById("deleteModal").classList.remove("show");
    showToast("Course deleted!");
    loadCourses();
    loadOverview();
  } catch (e) {
    showToast("Failed to delete course.", "error");
  }
}

// ===== ATTENDANCE =====
let attStudents = [];
async function loadAttendanceSection() {
  // Set today's date as default
  if (!document.getElementById("attDate").value) {
    document.getElementById("attDate").value = new Date()
      .toISOString()
      .split("T")[0];
  }
  // Load courses into dropdown
  try {
    const courses = await fetch("/api/courses").then((r) => r.json());
    const sel = document.getElementById("attCourse");
    sel.innerHTML =
      '<option value="">Select Course</option>' +
      (Array.isArray(courses) ? courses : [])
        .map((c) => `<option value="${c.id}">${esc(c.title)}</option>`)
        .join("");
  } catch (e) {
    console.error(e);
  }
  loadRecentAttendance();
}

async function loadCourseStudents() {
  const courseId = document.getElementById("attCourse").value;
  if (!courseId) {
    document.getElementById("attendanceForm").style.display = "none";
    return;
  }
  try {
    const enrollmentsPayload = await fetch(`/api/enrollments?page=0&size=1000`, {
      headers: authHeaders(),
    }).then((r) => r.json());
    const enrollments = unpackPageResponse(enrollmentsPayload).content;
    attStudents = (Array.isArray(enrollments) ? enrollments : []).filter(
      (e) => e.course && e.course.id == courseId && e.status === "ACTIVE",
    );
    const tbody = document.querySelector("#attendanceTable tbody");
    if (attStudents.length === 0) {
      tbody.innerHTML =
        '<tr><td colspan="3">No active students enrolled in this course</td></tr>';
    } else {
      tbody.innerHTML = attStudents
        .map(
          (e) => `
                        <tr>
                            <td><strong>${esc(e.user.name)}</strong><br><small>${esc(e.user.email)}</small></td>
                            <td>
                                <select class="form-input att-status" data-student-id="${e.user.id}" style="width:auto;">
                                    <option value="PRESENT" selected>Present</option>
                                    <option value="ABSENT">Absent</option>
                                    <option value="LATE">Late</option>
                                </select>
                            </td>
                            <td><input type="text" class="form-input att-remarks" data-student-id="${e.user.id}" placeholder="Optional remarks" style="min-width:150px;"></td>
                        </tr>`,
        )
        .join("");
    }
    document.getElementById("attendanceForm").style.display = "block";
  } catch (e) {
    console.error(e);
  }
}

async function submitAttendance() {
  const courseId = document.getElementById("attCourse").value;
  const date = document.getElementById("attDate").value;
  if (!courseId || !date) {
    showToast("Select course and date", "error");
    return;
  }

  const records = [];
  document.querySelectorAll(".att-status").forEach((sel) => {
    const sid = sel.dataset.studentId;
    const remarks = document.querySelector(
      `.att-remarks[data-student-id="${sid}"]`,
    ).value;
    records.push({ studentId: parseInt(sid), status: sel.value, remarks });
  });

  try {
    const res = await fetch("/api/attendance/mark", {
      method: "POST",
      headers: authHeaders(),
      body: JSON.stringify({ courseId: parseInt(courseId), date, records }),
    });
    const data = await res.json();
    if (res.ok) {
      showToast(`Attendance marked for ${records.length} students!`);
      loadRecentAttendance();
    } else {
      showToast(data.message || "Failed to mark attendance", "error");
    }
  } catch (e) {
    showToast("Error submitting attendance", "error");
  }
}

async function loadRecentAttendance() {
  const tbody = document.querySelector("#recentAttendance tbody");
  try {
    const records = await fetch("/api/attendance", {
      headers: authHeaders(),
    }).then((r) => r.json());
    const list = Array.isArray(records) ? records.slice(0, 50) : [];
    tbody.innerHTML =
      list
        .map(
          (a) => `
                    <tr>
                        <td>${esc(a.user ? a.user.name : "-")}</td>
                        <td>${esc(a.course ? a.course.title : "-")}</td>
                        <td>${a.attendanceDate || "-"}</td>
                        <td><span class="badge badge-${a.status === "PRESENT" ? "active" : a.status === "LATE" ? "student" : "admin"}">${a.status}</span></td>
                        <td>${esc(a.remarks || "-")}</td>
                    </tr>`,
        )
        .join("") || '<tr><td colspan="5">No attendance records yet</td></tr>';
  } catch (e) {
    tbody.innerHTML = '<tr><td colspan="5">Error loading records</td></tr>';
  }
}

// ===== PAYMENTS =====
async function loadPaymentsSection() {
  loadPaymentStats();
  loadPaymentDropdowns();
  loadAllPayments();
}

// ===== CRM LEADS =====
async function loadCrmSection() {
  try {
    const [leadsResponse, statsResponse] = await Promise.all([
      fetch(`/api/admin/leads?${crmQueryParams().toString()}`, {
        headers: authHeaders(),
      }).then((r) => r.json()),
      fetch("/api/admin/leads/stats", { headers: authHeaders() }).then((r) =>
        r.json(),
      ),
    ]);

    const leads = Array.isArray(leadsResponse.data) ? leadsResponse.data : [];
    const stats = statsResponse.data || {};
    document.getElementById("crmTotalLeads").textContent =
      stats.totalLeads || 0;
    document.getElementById("crmNewLeads").textContent = stats.newLeads || 0;
    document.getElementById("crmContactedLeads").textContent =
      stats.contactedLeads || 0;
    document.getElementById("crmHighIntentLeads").textContent =
      stats.highIntentLeads || 0;
    document.getElementById("crmReminderToday").textContent =
      stats.reminderToday || 0;
    document.getElementById("crmReminderOverdue").textContent =
      stats.reminderOverdue || 0;
    document.getElementById("crmReminderNext7").textContent =
      stats.reminderNext7Days || 0;

    const tbody = document.querySelector("#crmLeadsTable tbody");
    tbody.innerHTML =
      leads
        .map((lead) => {
          const assigneeInputId = crmInputId("crm-assignee", lead.source, lead.sourceId);
          const followUpAtInputId = crmInputId("crm-follow-at", lead.source, lead.sourceId);
          const followUpStatusInputId = crmInputId("crm-follow-status", lead.source, lead.sourceId);
          const followUpNotesInputId = crmInputId("crm-follow-notes", lead.source, lead.sourceId);
          const options = leadStatusOptions(lead.source)
            .map(
              (status) =>
                `<option value="${status}" ${lead.status === status ? "selected" : ""}>${status}</option>`,
            )
            .join("");
          const followUpStatusOptions = followUpOptions()
            .map(
              (status) =>
                `<option value="${status}" ${lead.followUpStatus === status ? "selected" : ""}>${status}</option>`,
            )
            .join("");
          return `
            <tr>
              <td><strong>${esc(lead.leadName)}</strong><br><small style="color:#64748b;">${esc(lead.guardianName || "")}</small></td>
              <td><span class="badge badge-student">${esc(lead.sourceLabel)}</span></td>
              <td>${esc(lead.subject || "-")}<br><small style="color:#64748b;">${esc([lead.grade, lead.board].filter(Boolean).join(" • ") || "-")}</small></td>
              <td>
                ${lead.email ? `<a href="mailto:${esc(lead.email)}">${esc(lead.email)}</a><br>` : ""}
                ${lead.phone ? `<a href="tel:${esc(lead.phone)}">${esc(lead.phone)}</a>` : "<span style=\"color:#94a3b8;\">No phone</span>"}
              </td>
              <td>
                <div style="display:flex;flex-direction:column;gap:0.5rem;min-width:140px;">
                  <span class="badge ${leadStatusBadgeClass(lead.status)}">${esc(lead.status)}</span>
                  <select onchange="updateCrmLeadStatus('${lead.source}', ${lead.sourceId}, this.value)" style="padding:0.3rem;border-radius:6px;border:1px solid #e5e7eb;font-size:0.8rem;">
                    ${options}
                  </select>
                </div>
              </td>
              <td>
                <input id="${assigneeInputId}" type="text" class="form-input" value="${esc(lead.assignee || "")}" placeholder="Assign owner" style="min-width:140px;padding:0.45rem 0.6rem;font-size:0.8rem;">
              </td>
              <td>
                <div style="display:flex;flex-direction:column;gap:0.4rem;min-width:170px;">
                  <select id="${followUpStatusInputId}" class="form-input" style="padding:0.35rem 0.45rem;font-size:0.8rem;">
                    ${followUpStatusOptions}
                  </select>
                  <input id="${followUpAtInputId}" type="datetime-local" class="form-input" value="${fmtDateTimeLocal(lead.followUpAt)}" style="padding:0.35rem 0.45rem;font-size:0.8rem;">
                  <input id="${followUpNotesInputId}" type="text" class="form-input" value="${esc(lead.followUpNotes || "")}" placeholder="Notes" style="padding:0.35rem 0.45rem;font-size:0.8rem;">
                  <button class="btn-icon" onclick="updateCrmLeadPipeline('${lead.source}', ${lead.sourceId})" style="font-size:0.8rem;border:1px solid #cbd5e1;padding:0.3rem 0.4rem;">Save</button>
                </div>
              </td>
              <td>${fmt(lead.createdAt)}</td>
              <td>${esc(lead.summary || "-")}</td>
            </tr>`;
        })
        .join("") || '<tr><td colspan="9">No leads matched the current filter</td></tr>';
  } catch (e) {
    console.error(e);
    document.querySelector("#crmLeadsTable tbody").innerHTML =
      '<tr><td colspan="9">Error loading leads</td></tr>';
  }
}

function applyCrmFilters() {
  loadCrmSection();
}

async function updateCrmLeadStatus(source, sourceId, status) {
  try {
    await fetch(`/api/admin/leads/${source}/${sourceId}/status?status=${encodeURIComponent(status)}`, {
      method: "PUT",
      headers: authHeaders(),
    });
    showToast("Lead status updated");
    loadCrmSection();
  } catch (e) {
    showToast("Error updating lead status", "error");
  }
}

async function updateCrmLeadPipeline(source, sourceId) {
  const assignee = document
    .getElementById(crmInputId("crm-assignee", source, sourceId))
    ?.value.trim();
  const followUpAt = document
    .getElementById(crmInputId("crm-follow-at", source, sourceId))
    ?.value;
  const followUpStatus = document
    .getElementById(crmInputId("crm-follow-status", source, sourceId))
    ?.value;
  const followUpNotes = document
    .getElementById(crmInputId("crm-follow-notes", source, sourceId))
    ?.value.trim();

  try {
    await fetch(`/api/admin/leads/${source}/${sourceId}/pipeline`, {
      method: "PUT",
      headers: authHeaders(),
      body: JSON.stringify({
        assignee: assignee || null,
        followUpAt: followUpAt || null,
        followUpStatus: followUpStatus || "NONE",
        followUpNotes: followUpNotes || null,
      }),
    });
    showToast("Pipeline updated");
    loadCrmSection();
  } catch (e) {
    showToast("Error updating follow-up details", "error");
  }
}

async function exportCrmLeads() {
  try {
    const response = await fetch(`/api/admin/leads/export?${crmQueryParams().toString()}`, {
      headers: authHeaders(),
    });
    if (!response.ok) {
      throw new Error("Export failed");
    }

    const blob = await response.blob();
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = "crm-leads.csv";
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  } catch (e) {
    showToast("Error exporting leads", "error");
  }
}

async function loadPaymentStats() {
  try {
    const res = await fetch("/api/payments/stats", { headers: authHeaders() });
    const data = await res.json();
    const stats = data.data || data;
    document.getElementById("totalRevenue").textContent =
      "₹ " + (stats.totalRevenue || 0).toLocaleString();
    document.getElementById("successPayments").textContent =
      stats.successCount || 0;
    document.getElementById("pendingPayments").textContent =
      stats.pendingCount || 0;
    document.getElementById("failedPayments").textContent =
      stats.failedCount || 0;
  } catch (e) {
    console.error(e);
  }
}

async function loadPaymentDropdowns() {
  try {
    const [students, courses] = await Promise.all([
      fetch("/api/users/students", { headers: authHeaders() }).then((r) =>
        r.json(),
      ),
      fetch("/api/courses").then((r) => r.json()),
    ]);
    document.getElementById("payStudent").innerHTML =
      '<option value="">Select Student</option>' +
      (Array.isArray(students) ? students : [])
        .map(
          (s) =>
            `<option value="${s.id}">${esc(s.name)} (${esc(s.email)})</option>`,
        )
        .join("");
    document.getElementById("payCourse").innerHTML =
      '<option value="">Select Course</option>' +
      (Array.isArray(courses) ? courses : [])
        .map(
          (c) =>
            `<option value="${c.id}" data-fee="${c.fee || 0}">${esc(c.title)} - ₹ ${c.fee || 0}</option>`,
        )
        .join("");
  } catch (e) {
    console.error(e);
  }
}

// Auto-fill fee when course is selected
document.getElementById("payCourse").addEventListener("change", function () {
  const opt = this.options[this.selectedIndex];
  if (opt.dataset.fee)
    document.getElementById("payAmount").value = opt.dataset.fee;
});

async function recordManualPayment() {
  const userId = document.getElementById("payStudent").value;
  const courseId = document.getElementById("payCourse").value;
  const amount = document.getElementById("payAmount").value;
  const method = document.getElementById("payMethod").value;
  if (!userId || !courseId || !amount) {
    showToast("Fill all required fields", "error");
    return;
  }

  try {
    const res = await fetch(`/api/payments/manual/${userId}`, {
      method: "POST",
      headers: authHeaders(),
      body: JSON.stringify({
        courseId: parseInt(courseId),
        amount: parseFloat(amount),
        paymentMethod: method,
      }),
    });
    const data = await res.json();
    if (res.ok) {
      showToast("Payment recorded successfully!");
      loadPaymentStats();
      loadAllPayments();
    } else {
      showToast(data.message || "Failed to record payment", "error");
    }
  } catch (e) {
    showToast("Error recording payment", "error");
  }
}

async function loadAllPayments(page = paymentsPage) {
  paymentsPage = Math.max(page, 0);
  const tbody = document.querySelector("#paymentsTable tbody");
  try {
    const payload = await fetch(
      `/api/payments?page=${paymentsPage}&size=${pageSize}`,
      {
        headers: authHeaders(),
      },
    ).then((r) => r.json());
    const paymentsPageData = unpackPageResponse(payload);
    const payments = paymentsPageData.content;
    tbody.innerHTML =
      (Array.isArray(payments) ? payments : [])
        .map(
          (p) => `
                    <tr>
                        <td>${esc(p.user ? p.user.name : "-")}</td>
                        <td>${esc(p.course ? p.course.title : "-")}</td>
                        <td style="font-weight:700;">₹ ${p.amount ? p.amount.toLocaleString() : 0}</td>
                        <td>${p.paymentMethod || "-"}</td>
                        <td><span class="badge badge-${p.status === "SUCCESS" ? "active" : p.status === "PENDING" ? "student" : "admin"}">${p.status}</span></td>
                        <td><small>${esc(p.receiptNumber || "-")}</small></td>
                        <td>${p.paidAt ? fmt(p.paidAt) : fmt(p.createdAt)}</td>
                        <td>
                          ${p.status === "PENDING" ? `<button class="btn-icon" onclick="confirmPayment(${p.id})" title="Confirm">Confirm</button><button class="btn-icon btn-danger" onclick="failPaymentAction(${p.id})" title="Reject">Reject</button>` : "-"}
                        </td>
                    </tr>`,
        )
        .join("") || '<tr><td colspan="8">No payments yet</td></tr>';
    renderPaginationControls(
      "paymentsPagination",
      paymentsPageData.number,
      paymentsPageData.totalPages,
      "loadAllPayments",
    );
  } catch (e) {
    tbody.innerHTML = '<tr><td colspan="8">Error loading payments</td></tr>';
    renderPaginationControls("paymentsPagination", 0, 0, "loadAllPayments");
  }
}

async function confirmPayment(paymentId) {
  try {
    const res = await fetch(`/api/payments/${paymentId}/confirm`, {
      method: "POST",
      headers: authHeaders(),
    });
    if (res.ok) {
      showToast("Payment confirmed!");
      loadPaymentStats();
      loadAllPayments();
    } else {
      showToast("Failed to confirm payment", "error");
    }
  } catch (e) {
    showToast("Error", "error");
  }
}

async function failPaymentAction(paymentId) {
  try {
    const res = await fetch(
      `/api/payments/${paymentId}/fail?reason=Admin%20rejected`,
      { method: "POST", headers: authHeaders() },
    );
    if (res.ok) {
      showToast("Payment marked as failed");
      loadPaymentStats();
      loadAllPayments();
    } else {
      showToast("Failed to update payment", "error");
    }
  } catch (e) {
    showToast("Error", "error");
  }
}

// ===== BLOG MANAGEMENT =====
async function loadBlogAdmin() {
  const tbody = document.querySelector("#blogTable tbody");
  try {
    const posts = await fetch("/api/blog/all", { headers: authHeaders() }).then(
      (r) => r.json(),
    );
    tbody.innerHTML =
      (Array.isArray(posts) ? posts : [])
        .map(
          (p) => `
                    <tr>
                        <td><strong>${esc(p.title)}</strong><br><small style="color:#94a3b8;">${esc(p.slug)}</small></td>
                        <td>${esc((p.category || "").replace(/_/g, " "))}</td>
                        <td><span class="badge ${p.published ? "badge-active" : "badge-admin"}">${p.published ? "Published" : "Draft"}</span></td>
                        <td>${p.publishedAt ? fmt(p.publishedAt) : "-"}</td>
                        <td>
                          <button class="btn-icon" onclick="togglePublish(${p.id})" title="${p.published ? "Unpublish" : "Publish"}">${p.published ? "Unpublish" : "Publish"}</button>
                          <button class="btn-icon btn-danger" onclick="deleteBlogPost(${p.id})" title="Delete">Delete</button>
                        </td>
                    </tr>`,
        )
        .join("") || '<tr><td colspan="5">No blog posts yet</td></tr>';
  } catch (e) {
    tbody.innerHTML = '<tr><td colspan="5">Error loading posts</td></tr>';
  }
}

async function togglePublish(id) {
  try {
    await fetch(`/api/blog/${id}/publish`, {
      method: "PUT",
      headers: authHeaders(),
    });
    showToast("Publish status toggled");
    loadBlogAdmin();
  } catch (e) {
    showToast("Error toggling publish", "error");
  }
}

async function deleteBlogPost(id) {
  if (!confirm("Delete this blog post?")) return;
  try {
    await fetch(`/api/blog/${id}`, {
      method: "DELETE",
      headers: authHeaders(),
    });
    showToast("Blog post deleted");
    loadBlogAdmin();
  } catch (e) {
    showToast("Error deleting post", "error");
  }
}

function openBlogModal() {
  const html = `<div class="modal-overlay show" id="blogModal" onclick="if(event.target===this)this.remove()">
                <div class="modal" style="max-width:600px;max-height:90vh;overflow-y:auto;">
                    <div class="modal-header"><div class="modal-title">New Blog Post</div><button class="modal-close" onclick="document.getElementById('blogModal').remove()">x</button></div>
                    <form onsubmit="saveBlogPost(event)">
                        <div class="form-group"><label class="form-label">Title *</label><input type="text" id="blogTitle" class="form-input" required></div>
                        <div class="form-group"><label class="form-label">Excerpt</label><input type="text" id="blogExcerpt" class="form-input" placeholder="Short summary for listing"></div>
                        <div class="form-group"><label class="form-label">Category *</label>
                            <select id="blogCategory" class="form-input" required>
                                <option value="">Select</option><option value="LEARNING_TIPS">Learning Tips</option><option value="EXAM_STRATEGIES">Exam Strategies</option><option value="LANGUAGE_INSIGHTS">Language Insights</option><option value="ACADEMY_NEWS">Academy News</option>
                            </select>
                        </div>
                        <div class="form-group"><label class="form-label">Author</label><input type="text" id="blogAuthor" class="form-input" value="BrightNest Team"></div>
                        <div class="form-group"><label class="form-label">Cover Image URL</label><input type="url" id="blogCoverImageUrl" class="form-input" placeholder="https://example.com/cover.jpg"></div>
                        <div class="form-group"><label class="form-label">Content (HTML) *</label><textarea id="blogContent" class="form-input" rows="8" required placeholder="<p>Your article content...</p>"></textarea></div>
                        <div class="form-group"><label style="display:flex;align-items:center;gap:0.5rem;cursor:pointer;"><input type="checkbox" id="blogPublished"> Publish immediately</label></div>
                        <button type="submit" class="btn-submit">Save Blog Post</button>
                    </form>
                </div>
            </div>`;
  document.body.insertAdjacentHTML("beforeend", html);
}

async function saveBlogPost(e) {
  e.preventDefault();
  const data = {
    title: document.getElementById("blogTitle").value,
    excerpt: document.getElementById("blogExcerpt").value || null,
    category: document.getElementById("blogCategory").value,
    author: document.getElementById("blogAuthor").value || "BrightNest Team",
    coverImageUrl: document.getElementById("blogCoverImageUrl").value || null,
    content: document.getElementById("blogContent").value,
    published: document.getElementById("blogPublished").checked,
  };
  try {
    const res = await fetch("/api/blog", {
      method: "POST",
      headers: authHeaders(),
      body: JSON.stringify(data),
    });
    if (res.ok) {
      showToast("Blog post created!");
      document.getElementById("blogModal").remove();
      loadBlogAdmin();
    } else {
      const err = await res.json();
      showToast(err.message || "Error creating post", "error");
    }
  } catch (e) {
    showToast("Error creating post", "error");
  }
}

// ===== DEMO BOOKINGS MANAGEMENT =====
async function loadDemosAdmin() {
  try {
    const [bookings, stats] = await Promise.all([
      fetch("/api/demo-booking", { headers: authHeaders() }).then((r) =>
        r.json(),
      ),
      fetch("/api/demo-booking/stats", { headers: authHeaders() }).then((r) =>
        r.json(),
      ),
    ]);
    document.getElementById("totalDemos").textContent =
      (stats.PENDING || 0) +
      (stats.SCHEDULED || 0) +
      (stats.COMPLETED || 0) +
      (stats.CANCELLED || 0);
    document.getElementById("pendingDemos").textContent = stats.PENDING || 0;
    document.getElementById("scheduledDemos").textContent =
      stats.SCHEDULED || 0;
    document.getElementById("completedDemos").textContent =
      stats.COMPLETED || 0;
    const tbody = document.querySelector("#demosTable tbody");
    tbody.innerHTML =
      (Array.isArray(bookings) ? bookings : [])
        .map(
          (d) => `
                    <tr>
                        <td><strong>${esc(d.studentName)}</strong><br><small>${esc(d.parentName || "")}</small></td>
                        <td>${esc(d.subject)}</td><td>${esc(d.grade)}</td><td>${d.classMode || "-"}</td>
                        <td><a href="tel:${d.phone}">${esc(d.phone)}</a></td>
                        <td><select onchange="updateDemoStatus(${d.id},this.value)" style="padding:0.3rem;border-radius:6px;border:1px solid #e5e7eb;font-size:0.8rem;">
                            ${["PENDING", "SCHEDULED", "COMPLETED", "CANCELLED"].map((s) => `<option value="${s}" ${d.status === s ? "selected" : ""}>${s}</option>`).join("")}
                        </select></td>
                        <td>${fmt(d.createdAt)}</td>
                        <td><a href="tel:${d.phone}" title="Call student" style="font-size:1.2rem;">📞</a></td>
                    </tr>`,
        )
        .join("") || '<tr><td colspan="8">No demo bookings yet</td></tr>';
  } catch (e) {
    console.error(e);
    document.querySelector("#demosTable tbody").innerHTML =
      '<tr><td colspan="8">Error loading</td></tr>';
  }
}

async function updateDemoStatus(id, status) {
  try {
    await fetch(`/api/demo-booking/${id}/status?status=${status}`, {
      method: "PUT",
      headers: authHeaders(),
    });
    showToast("Status updated");
    loadDemosAdmin();
  } catch (e) {
    showToast("Error updating status", "error");
  }
}

// ===== TEACHER APPLICATIONS MANAGEMENT =====
async function loadTeacherAppsAdmin() {
  try {
    const [apps, stats] = await Promise.all([
      fetch("/api/teacher-applications", { headers: authHeaders() }).then((r) =>
        r.json(),
      ),
      fetch("/api/teacher-applications/stats", { headers: authHeaders() }).then(
        (r) => r.json(),
      ),
    ]);
    document.getElementById("totalTeacherApps").textContent =
      (stats.NEW || 0) +
      (stats.REVIEWED || 0) +
      (stats.CONTACTED || 0) +
      (stats.HIRED || 0) +
      (stats.REJECTED || 0);
    document.getElementById("newTeacherApps").textContent = stats.NEW || 0;
    document.getElementById("hiredTeacherApps").textContent = stats.HIRED || 0;
    const tbody = document.querySelector("#teacherAppsTable tbody");
    tbody.innerHTML =
      (Array.isArray(apps) ? apps : [])
        .map(
          (a) => `
                    <tr>
                        <td><strong>${esc(a.fullName)}</strong></td>
                        <td>${esc(a.email)}</td>
                        <td><a href="tel:${a.phone}">${esc(a.phone)}</a></td>
                        <td>${esc(a.subjectExpertise)}</td>
                        <td>${a.experience || "-"}</td>
                        <td><select onchange="updateTeacherStatus(${a.id},this.value)" style="padding:0.3rem;border-radius:6px;border:1px solid #e5e7eb;font-size:0.8rem;">
                            ${["NEW", "REVIEWED", "CONTACTED", "HIRED", "REJECTED"].map((s) => `<option value="${s}" ${a.status === s ? "selected" : ""}>${s}</option>`).join("")}
                        </select></td>
                        <td>${fmt(a.createdAt)}</td>
                        <td><a href="mailto:${a.email}" title="Email" style="font-size:0.95rem;">Email</a></td>
                    </tr>`,
        )
        .join("") || '<tr><td colspan="8">No applications yet</td></tr>';
  } catch (e) {
    console.error(e);
    document.querySelector("#teacherAppsTable tbody").innerHTML =
      '<tr><td colspan="8">Error loading</td></tr>';
  }
}

async function updateTeacherStatus(id, status) {
  try {
    await fetch(`/api/teacher-applications/${id}/status`, {
      method: "PUT",
      headers: authHeaders(),
      body: JSON.stringify({ status }),
    });
    showToast("Status updated");
    loadTeacherAppsAdmin();
  } catch (e) {
    showToast("Error updating status", "error");
  }
}

// Initial load
loadOverview();
