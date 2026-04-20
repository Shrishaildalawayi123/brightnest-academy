// Require auth
Auth.requireAuth();
const user = Auth.getCurrentUser();
if (user && (user.role === "ADMIN" || user.role === "ROLE_ADMIN"))
  window.location.href = "/admin-dashboard.html";
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
  if (token && token.trim() && !headers["Authorization"]) {
    headers["Authorization"] = `Bearer ${token.trim()}`;
  }
  if (["POST", "PUT", "PATCH", "DELETE"].includes(method)) {
    const csrfToken = getCookie("XSRF-TOKEN");
    if (csrfToken) headers["X-CSRF-Token"] = csrfToken;
  }
  const response = await nativeFetch(url, {
    credentials: "include",
    ...options,
    method,
    headers,
  });
  if (response.status === 401) {
    localStorage.removeItem("user");
    localStorage.removeItem("token");
    window.location.href = "/login.html";
    throw new Error("Unauthorized");
  }
  return response;
};

function toggleSidebar() {
  const sidebar = document.querySelector(".sidebar");
  const overlay = document.getElementById("sidebarOverlay");
  sidebar.classList.toggle("open");
  overlay.style.display = sidebar.classList.contains("open") ? "block" : "none";
}

// Set user info
document.getElementById("userName").textContent = user.name || "Student";
document.getElementById("sidebarStudentName").textContent =
  user.name || "Student";
document.getElementById("userAvatar").textContent = (user.name ||
  "S")[0].toUpperCase();
document.getElementById("bigAvatar").textContent = (user.name ||
  "S")[0].toUpperCase();
document.getElementById("profileName").textContent = user.name || "-";
document.getElementById("profileEmail").textContent = user.email || "-";
document.getElementById("dateDisplay").textContent =
  new Date().toLocaleDateString("en-IN", {
    weekday: "long",
    day: "numeric",
    month: "long",
    year: "numeric",
  });

let allCourses = [];
let myEnrollments = [];
let myProgress = [];
let notifications = [];
const titles = {
  overview: "Overview",
  myCourses: "My Courses",
  allCourses: "Browse Courses",
  myAttendance: "My Attendance",
  myPayments: "My Payments",
  profile: "My Profile",
};

function showToast(msg, type = "success") {
  const t = document.getElementById("toast");
  t.innerHTML = (type === "success" ? "Success: " : "Error: ") + msg;
  t.className = `toast ${type} show`;
  setTimeout(() => {
    t.classList.remove("show");
  }, 3000);
}

function formatRelativeTime(dateValue) {
  if (!dateValue) return "just now";
  const then = new Date(dateValue).getTime();
  const now = Date.now();
  const diffSec = Math.max(0, Math.floor((now - then) / 1000));

  if (diffSec < 60) return "just now";
  const min = Math.floor(diffSec / 60);
  if (min < 60) return `${min} minute${min === 1 ? "" : "s"} ago`;
  const hr = Math.floor(min / 60);
  if (hr < 24) return `${hr} hour${hr === 1 ? "" : "s"} ago`;
  const day = Math.floor(hr / 24);
  return `${day} day${day === 1 ? "" : "s"} ago`;
}

function notificationIconFor(type) {
  const normalized = (type || "").toUpperCase();
  if (normalized.includes("PAYMENT")) return "💳";
  if (normalized.includes("ATTEND")) return "✅";
  if (normalized.includes("ASSIGN")) return "📝";
  if (normalized.includes("ENROLL")) return "🎓";
  return "🔔";
}

function updateNotificationsBadge() {
  const badge = document.getElementById("notificationsBadge");
  const unread = notifications.filter((n) => !n.read).length;
  if (unread > 0) {
    badge.textContent = unread > 99 ? "99+" : String(unread);
    badge.classList.add("show");
  } else {
    badge.classList.remove("show");
  }
}

function renderNotifications() {
  const list = document.getElementById("notificationsList");
  if (!notifications.length) {
    list.innerHTML =
      '<div class="notifications-empty">No notifications yet.</div>';
    updateNotificationsBadge();
    return;
  }

  list.innerHTML = notifications
    .map(
      (n) => `
      <article class="notification-item" data-id="${n.id}">
        <span class="notification-icon">${notificationIconFor(n.type)}</span>
        <div>
          <p class="notification-message">${esc(n.message || "Notification")}</p>
          <div class="notification-time">${formatRelativeTime(n.createdAt)}</div>
        </div>
      </article>
    `,
    )
    .join("");

  list.querySelectorAll(".notification-item").forEach((el) => {
    el.addEventListener("click", async () => {
      const notificationId = Number(el.getAttribute("data-id"));
      await markNotificationRead(notificationId);
    });
  });

  updateNotificationsBadge();
}

async function fetchNotifications() {
  try {
    const result = await API.getMyNotifications();
    notifications = Array.isArray(result) ? result : [];
    renderNotifications();
  } catch (e) {
    const list = document.getElementById("notificationsList");
    list.innerHTML =
      '<div class="notifications-empty">Failed to load notifications.</div>';
  }
}

async function markNotificationRead(notificationId) {
  if (!notificationId) return;

  try {
    await API.markNotificationRead(notificationId);
    notifications = notifications.map((n) =>
      n.id === notificationId ? { ...n, read: true } : n,
    );
    updateNotificationsBadge();
    await fetchNotifications();
  } catch (e) {
    showToast("Failed to mark notification as read", "error");
  }
}

function openNotificationsPanel() {
  const panel = document.getElementById("notificationsPanel");
  panel.classList.add("open");
  panel.setAttribute("aria-hidden", "false");
}

function closeNotificationsPanel() {
  const panel = document.getElementById("notificationsPanel");
  panel.classList.remove("open");
  panel.setAttribute("aria-hidden", "true");
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

  if (name === "allCourses") renderAllCourses();
  if (name === "myCourses") renderMyCourses();
  if (name === "myAttendance") loadStudentAttendance();
  if (name === "myPayments") loadStudentPayments();
}

function courseCard(course, isEnrolled) {
  return `
            <div class="course-card">
                <div class="course-banner" style="background:${course.color || "#3B82F6"};"></div>
                <div class="course-body">
                <div class="course-icon">${course.icon || "COURSE"}</div>
                    <div class="course-title">${course.title}</div>
                    <div class="course-desc">${course.description || ""}</div>
                    <div class="course-meta">
                  <span class="course-duration">Duration: ${course.duration || "N/A"}</span>
                        <span class="badge ${isEnrolled ? "badge-enrolled" : "badge-available"}">${isEnrolled ? "Enrolled" : "Available"}</span>
                    </div>
                    ${
                      isEnrolled
                        ? `<button class="btn-enroll enrolled" disabled>Already Enrolled</button>`
                        : `<button class="btn-enroll primary" onclick="enrollInCourse(${course.id}, this)">Enroll Now</button>`
                    }
                </div>
            </div>`;
}

function renderAllCourses() {
  const grid = document.getElementById("allCoursesGrid");
  if (!allCourses.length) {
    grid.innerHTML =
      '<div class="empty-state"><div class="icon">Info</div><p>No courses available</p></div>';
    return;
  }
  const enrolledIds = myEnrollments.map((e) =>
    e.course ? e.course.id : e.courseId || null,
  );
  grid.innerHTML = allCourses
    .map((c) => courseCard(c, enrolledIds.includes(c.id)))
    .join("");
}

function renderMyCourses() {
  const grid = document.getElementById("myCoursesGrid");
  if (!myEnrollments.length) {
    grid.innerHTML =
      '<div class="empty-state"><div class="icon">Info</div><p>You have not enrolled in any courses yet.</p><br><button onclick="showSection(\'allCourses\')" class="btn-enroll primary" style="width:auto;padding:0.5rem 1.5rem;">Browse Courses</button></div>';
    return;
  }
  grid.innerHTML = myEnrollments
    .map((e) => {
      const c = e.course || {};
      return courseCard(c, true);
    })
    .join("");

  renderMyProgress();
}

function renderMyProgress() {
  const progressGrid = document.getElementById("myProgressGrid");
  if (!progressGrid) return;

  if (!myProgress.length) {
    progressGrid.innerHTML =
      '<div class="empty-state"><div class="icon">Info</div><p>No progress data available yet.</p></div>';
    return;
  }

  progressGrid.innerHTML = myProgress
    .map((p) => {
      const attendance = Number(p.attendancePercent || 0);
      const submitted = Number(p.assignmentsSubmitted || 0);
      const totalAssignments = Number(p.assignmentsTotal || 0);
      const averageScore =
        p.averageScore === null || p.averageScore === undefined
          ? "N/A"
          : `${Math.round(Number(p.averageScore))}%`;

      return `
        <article class="progress-card">
          <h4>${esc(p.courseName || "Course")}</h4>
          <div class="progress-row">
            ${attendance.toFixed(0)}% attendance
            <div class="progress-track">
              <div class="progress-fill" style="width:${Math.max(0, Math.min(100, attendance))}%;"></div>
            </div>
          </div>
          <div class="progress-row">
            ${submitted}/${totalAssignments} submitted, avg ${averageScore}
          </div>
        </article>
      `;
    })
    .join("");
}

async function enrollInCourse(courseId, btn) {
  btn.disabled = true;
  btn.textContent = "Enrolling...";
  try {
    const res = await fetch(`/api/enrollments/${courseId}`, { method: "POST" });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || "Enrollment failed");
    }
    showToast("Successfully enrolled!");
    // Refresh
    await loadData();
    renderAllCourses();
  } catch (e) {
    showToast("Enrollment failed. Please try again.", "error");
    btn.disabled = false;
    btn.textContent = "Enroll Now";
  }
}

// ===== STUDENT ATTENDANCE =====
function esc(str) {
  const d = document.createElement("div");
  d.textContent = str || "";
  return d.innerHTML;
}
function fmt(dt) {
  return dt ? new Date(dt).toLocaleDateString("en-IN") : "-";
}

async function loadStudentAttendance() {
  const hdr = {};
  const courseId = document.getElementById("attFilterCourse").value;
  try {
    // Load course filter options
    if (
      document.getElementById("attFilterCourse").options.length <= 1 &&
      myEnrollments.length
    ) {
      myEnrollments.forEach((e) => {
        if (e.course) {
          const opt = document.createElement("option");
          opt.value = e.course.id;
          opt.textContent = e.course.title;
          document.getElementById("attFilterCourse").appendChild(opt);
        }
      });
    }

    const url = courseId
      ? `/api/attendance/my-attendance/${courseId}`
      : "/api/attendance/my-attendance";
    const records = await fetch(url, { headers: hdr }).then((r) => r.json());
    const list = Array.isArray(records) ? records : [];

    // Calculate stats
    let present = 0,
      absent = 0,
      late = 0;
    list.forEach((a) => {
      if (a.status === "PRESENT") present++;
      else if (a.status === "ABSENT") absent++;
      else if (a.status === "LATE") late++;
    });
    document.getElementById("attTotal").textContent = list.length;
    document.getElementById("attPresent").textContent = present;
    document.getElementById("attLate").textContent = late;
    document.getElementById("attAbsent").textContent = absent;

    const tbody = document.querySelector("#myAttendanceTable tbody");
    tbody.innerHTML =
      list
        .map(
          (a) => `
                    <tr>
                        <td>${esc(a.course ? a.course.title : "-")}</td>
                        <td>${a.attendanceDate || "-"}</td>
                        <td><span class="badge badge-${a.status === "PRESENT" ? "enrolled" : a.status === "LATE" ? "available" : "admin"}">${a.status}</span></td>
                        <td>${esc(a.remarks || "-")}</td>
                    </tr>`,
        )
        .join("") || '<tr><td colspan="4">No attendance records yet</td></tr>';
  } catch (e) {
    console.error(e);
    document.querySelector("#myAttendanceTable tbody").innerHTML =
      '<tr><td colspan="4">Error loading attendance</td></tr>';
  }
}

// ===== STUDENT PAYMENTS =====
async function loadStudentPayments() {
  const hdr = {};
  const tbody = document.querySelector("#myPaymentsTable tbody");
  try {
    const payments = await fetch("/api/payments/my-payments", {
      headers: hdr,
    }).then((r) => r.json());
    const list = Array.isArray(payments) ? payments : [];
    tbody.innerHTML =
      list
        .map(
          (p) => `
                    <tr>
                        <td>${esc(p.course ? p.course.title : "-")}</td>
                        <td style="font-weight:700;">₹ ${p.amount ? p.amount.toLocaleString() : 0}</td>
                        <td>${p.paymentMethod || "-"}</td>
                        <td><span class="badge badge-${p.status === "SUCCESS" ? "enrolled" : p.status === "PENDING" ? "available" : "admin"}">${p.status}</span></td>
                        <td><small>${esc(p.receiptNumber || "-")}</small></td>
                        <td>${p.paidAt ? fmt(p.paidAt) : fmt(p.createdAt)}</td>
                    </tr>`,
        )
        .join("") || '<tr><td colspan="6">No payment records yet</td></tr>';
  } catch (e) {
    console.error(e);
    tbody.innerHTML = '<tr><td colspan="6">Error loading payments</td></tr>';
  }
}

async function loadData() {
  try {
    const hdr = {};
    const [courses, enrollments, progress] = await Promise.all([
      fetch("/api/courses").then((r) => r.json()),
      fetch("/api/enrollments/my-courses", { headers: hdr }).then((r) =>
        r.json(),
      ),
      API.getMyProgress().catch(() => []),
    ]);
    allCourses = Array.isArray(courses) ? courses : [];
    myEnrollments = Array.isArray(enrollments) ? enrollments : [];
    myProgress = Array.isArray(progress) ? progress : [];

    // Stats
    document.getElementById("enrolledCount").textContent = myEnrollments.length;
    document.getElementById("activeCount").textContent = myEnrollments.filter(
      (e) => e.status === "ACTIVE",
    ).length;
    document.getElementById("availableCount").textContent = allCourses.length;

    // Overview courses
    const enrolledIds = myEnrollments.map((e) =>
      e.course ? e.course.id : null,
    );
    const oc = document.getElementById("overviewCourses");
    if (!myEnrollments.length) {
      oc.innerHTML =
        '<div class="empty-state"><div class="icon">Info</div><p>No enrollments yet. <a href="#" onclick="showSection(\'allCourses\')">Browse courses</a></p></div>';
    } else {
      oc.innerHTML = myEnrollments
        .slice(0, 3)
        .map((e) => courseCard(e.course || {}, true))
        .join("");
    }

    renderMyProgress();
  } catch (e) {
    console.error("Load error:", e);
  }
}

document
  .getElementById("notificationsBell")
  .addEventListener("click", async () => {
    openNotificationsPanel();
    await fetchNotifications();
  });

document
  .getElementById("notificationsClose")
  .addEventListener("click", closeNotificationsPanel);

document.addEventListener("keydown", (event) => {
  if (event.key === "Escape") {
    closeNotificationsPanel();
  }
});

loadData();
fetchNotifications();
setInterval(fetchNotifications, 60000);
