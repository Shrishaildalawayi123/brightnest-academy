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
    response = await nativeFetch(url, {
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

  debugAdmin("response", { method, url: requestUrl, status: response.status });
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
  overview: "ðŸ“Š Overview",
  courses: "ðŸ“š Manage Courses",
  students: "ðŸ‘¥ Students",
  enrollments: "ðŸ“‹ Enrollments",
  attendance: "âœ… Attendance",
  payments: "ðŸ’° Payments",
  blog: "ðŸ“ Blog Posts",
  demos: "ðŸŽ¯ Demo Bookings",
  teachers: "ðŸ§‘â€ðŸ« Teacher Applications",
};

function showToast(msg, type = "success") {
  const t = document.getElementById("toast");
  t.textContent = (type === "success" ? "âœ… " : "âŒ ") + msg;
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

// Overview
async function loadOverview() {
  try {
    const [users, courses, enrollments] = await Promise.all([
      fetch("/api/users", { headers: authHeaders() }).then((r) => r.json()),
      fetch("/api/courses").then((r) => r.json()),
      fetch("/api/enrollments", { headers: authHeaders() }).then((r) =>
        r.json(),
      ),
    ]);
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
                        <td>â±ï¸ ${esc(c.duration)}</td>
                        <td style="font-weight:700;color:var(--primary-700);">${c.fee ? "â‚¹" + Number(c.fee).toLocaleString() : "-"}</td>
                        <td>
                            <button class="btn-icon" onclick="editCourse(${c.id},'${esc(c.title)}','${esc(c.description || "")}','${esc(c.duration || "")}','${esc(c.icon || "")}','${esc(c.color || "")}',${c.fee || 0})">âœï¸</button>
                            <button class="btn-icon btn-danger" onclick="deleteCourse(${c.id},'${esc(c.title)}')">ðŸ—‘ï¸</button>
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
async function loadStudents() {
  const tbody = document.querySelector("#studentsTable tbody");
  try {
    const users = await fetch("/api/users", { headers: authHeaders() }).then(
      (r) => r.json(),
    );
    tbody.innerHTML =
      (Array.isArray(users) ? users : [])
        .map(
          (u) => `
                    <tr><td><strong>${esc(u.name)}</strong></td><td>${esc(u.email)}</td>
                    <td>${esc(u.phone || "-")}</td>
                    <td><span class="badge badge-${u.role.toLowerCase()}">${u.role}</span></td></tr>`,
        )
        .join("") || '<tr><td colspan="4">No users found</td></tr>';
  } catch (e) {
    tbody.innerHTML = '<tr><td colspan="4">Error loading students</td></tr>';
  }
}

// Enrollments
async function loadEnrollments() {
  const tbody = document.querySelector("#enrollmentsTable tbody");
  try {
    const enrollments = await fetch("/api/enrollments", {
      headers: authHeaders(),
    }).then((r) => r.json());
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
  } catch (e) {
    tbody.innerHTML = '<tr><td colspan="4">Error loading enrollments</td></tr>';
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
    const enrollments = await fetch("/api/enrollments", {
      headers: authHeaders(),
    }).then((r) => r.json());
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
                                    <option value="PRESENT" selected>âœ… Present</option>
                                    <option value="ABSENT">âŒ Absent</option>
                                    <option value="LATE">â° Late</option>
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

async function loadPaymentStats() {
  try {
    const res = await fetch("/api/payments/stats", { headers: authHeaders() });
    const data = await res.json();
    const stats = data.data || data;
    document.getElementById("totalRevenue").textContent =
      "â‚¹" + (stats.totalRevenue || 0).toLocaleString();
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
    const [users, courses] = await Promise.all([
      fetch("/api/users", { headers: authHeaders() }).then((r) => r.json()),
      fetch("/api/courses").then((r) => r.json()),
    ]);
    const students = (Array.isArray(users) ? users : []).filter(
      (u) => u.role === "STUDENT",
    );
    document.getElementById("payStudent").innerHTML =
      '<option value="">Select Student</option>' +
      students
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
            `<option value="${c.id}" data-fee="${c.fee || 0}">${esc(c.title)} - â‚¹${c.fee || 0}</option>`,
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

async function loadAllPayments() {
  const tbody = document.querySelector("#paymentsTable tbody");
  try {
    const payments = await fetch("/api/payments", {
      headers: authHeaders(),
    }).then((r) => r.json());
    tbody.innerHTML =
      (Array.isArray(payments) ? payments : [])
        .map(
          (p) => `
                    <tr>
                        <td>${esc(p.user ? p.user.name : "-")}</td>
                        <td>${esc(p.course ? p.course.title : "-")}</td>
                        <td style="font-weight:700;">â‚¹${p.amount ? p.amount.toLocaleString() : 0}</td>
                        <td>${p.paymentMethod || "-"}</td>
                        <td><span class="badge badge-${p.status === "SUCCESS" ? "active" : p.status === "PENDING" ? "student" : "admin"}">${p.status}</span></td>
                        <td><small>${esc(p.receiptNumber || "-")}</small></td>
                        <td>${p.paidAt ? fmt(p.paidAt) : fmt(p.createdAt)}</td>
                        <td>
                            ${p.status === "PENDING" ? `<button class="btn-icon" onclick="confirmPayment(${p.id})" title="Confirm">âœ…</button><button class="btn-icon btn-danger" onclick="failPaymentAction(${p.id})" title="Reject">âŒ</button>` : "-"}
                        </td>
                    </tr>`,
        )
        .join("") || '<tr><td colspan="8">No payments yet</td></tr>';
  } catch (e) {
    tbody.innerHTML = '<tr><td colspan="8">Error loading payments</td></tr>';
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
                            <button class="btn-icon" onclick="togglePublish(${p.id})" title="${p.published ? "Unpublish" : "Publish"}">${p.published ? "ðŸ“¤" : "ðŸ“¥"}</button>
                            <button class="btn-icon btn-danger" onclick="deleteBlogPost(${p.id})" title="Delete">ðŸ—‘ï¸</button>
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
                    <div class="modal-header"><div class="modal-title">New Blog Post</div><button class="modal-close" onclick="document.getElementById('blogModal').remove()">Ã—</button></div>
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
                        <td><a href="mailto:${a.email}" title="Email" style="font-size:1.2rem;">âœ‰ï¸</a></td>
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
    await fetch(`/api/teacher-applications/${id}/status?status=${status}`, {
      method: "PUT",
      headers: authHeaders(),
    });
    showToast("Status updated");
    loadTeacherAppsAdmin();
  } catch (e) {
    showToast("Error updating status", "error");
  }
}

// Initial load
loadOverview();
