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
const titles = {
  overview: "ðŸ“Š Overview",
  myCourses: "ðŸ“š My Courses",
  allCourses: "ðŸ” Browse Courses",
  myAttendance: "âœ… My Attendance",
  myPayments: "ðŸ’° My Payments",
  profile: "ðŸ‘¤ My Profile",
};

function showToast(msg, type = "success") {
  const t = document.getElementById("toast");
  t.innerHTML = (type === "success" ? "âœ… " : "âŒ ") + msg;
  t.className = `toast ${type} show`;
  setTimeout(() => {
    t.classList.remove("show");
  }, 3000);
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
                    <div class="course-icon">${course.icon || "ðŸ“š"}</div>
                    <div class="course-title">${course.title}</div>
                    <div class="course-desc">${course.description || ""}</div>
                    <div class="course-meta">
                        <span class="course-duration">â±ï¸ ${course.duration || "N/A"}</span>
                        <span class="badge ${isEnrolled ? "badge-enrolled" : "badge-available"}">${isEnrolled ? "Enrolled" : "Available"}</span>
                    </div>
                    ${
                      isEnrolled
                        ? `<button class="btn-enroll enrolled" disabled>âœ… Already Enrolled</button>`
                        : `<button class="btn-enroll primary" onclick="enrollInCourse(${course.id}, this)">Enroll Now</button>`
                    }
                </div>
            </div>`;
}

function renderAllCourses() {
  const grid = document.getElementById("allCoursesGrid");
  if (!allCourses.length) {
    grid.innerHTML =
      '<div class="empty-state"><div class="icon">ðŸ“­</div><p>No courses available</p></div>';
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
      '<div class="empty-state"><div class="icon">ðŸ“š</div><p>You have not enrolled in any courses yet.</p><br><button onclick="showSection(\'allCourses\')" class="btn-enroll primary" style="width:auto;padding:0.5rem 1.5rem;">Browse Courses</button></div>';
    return;
  }
  grid.innerHTML = myEnrollments
    .map((e) => {
      const c = e.course || {};
      return courseCard(c, true);
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
    showToast("Successfully enrolled! ðŸŽ‰");
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
                        <td style="font-weight:700;">â‚¹${p.amount ? p.amount.toLocaleString() : 0}</td>
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
    const [courses, enrollments] = await Promise.all([
      fetch("/api/courses").then((r) => r.json()),
      fetch("/api/enrollments/my-courses", { headers: hdr }).then((r) =>
        r.json(),
      ),
    ]);
    allCourses = Array.isArray(courses) ? courses : [];
    myEnrollments = Array.isArray(enrollments) ? enrollments : [];

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
        '<div class="empty-state"><div class="icon">ðŸ“š</div><p>No enrollments yet. <a href="#" onclick="showSection(\'allCourses\')">Browse courses</a></p></div>';
    } else {
      oc.innerHTML = myEnrollments
        .slice(0, 3)
        .map((e) => courseCard(e.course || {}, true))
        .join("");
    }
  } catch (e) {
    console.error("Load error:", e);
  }
}

loadData();
