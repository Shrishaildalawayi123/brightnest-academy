import { beforeEach, describe, expect, it, vi } from "vitest";
import { loadBrowserScript } from "../test-utils/loadBrowserScript.js";

function jsonResponse(body, status = 200) {
  return {
    status,
    json: vi.fn().mockResolvedValue(body),
  };
}

async function flushAsyncWork() {
  await Promise.resolve();
  await Promise.resolve();
  await Promise.resolve();
}

describe("admin dashboard overview", () => {
  beforeEach(() => {
    document.body.innerHTML = `
      <div id="adminName"></div>
      <div id="dateDisplay"></div>
      <div id="totalStudents"></div>
      <div id="totalCourses"></div>
      <div id="totalEnrollments"></div>
      <div id="activeEnrollments"></div>
      <table id="recentStudents"><tbody></tbody></table>
      <form id="courseForm"></form>
      <select id="payCourse"><option value="">Select</option></select>
    `;

    localStorage.setItem("token", "server-jwt");
    window.Auth = {
      requireAdmin: vi.fn(),
      getCurrentUser: vi.fn().mockReturnValue({ name: "QA Admin" }),
    };
    window.API = {
      getHeaders: vi.fn().mockImplementation((includeAuth = false) => {
        const headers = { "Content-Type": "application/json" };
        if (includeAuth) {
          headers.Authorization = "Bearer server-jwt";
        }
        return headers;
      }),
    };
  });

  it("loads overview data with authenticated users request", async () => {
    const nativeFetch = vi.fn().mockImplementation((url) => {
      if (url === "/api/users") {
        return Promise.resolve(
          jsonResponse([
            {
              name: "QA Admin",
              email: "admin@example.com",
              role: "ADMIN",
              createdAt: "2026-03-06T00:00:00Z",
            },
            {
              name: "Student One",
              email: "student@example.com",
              role: "STUDENT",
              createdAt: "2026-03-06T00:00:00Z",
            },
          ]),
        );
      }
      if (url === "/api/courses") {
        return Promise.resolve(
          jsonResponse([
            { id: 1, title: "English" },
            { id: 2, title: "Maths" },
          ]),
        );
      }
      if (url === "/api/enrollments") {
        return Promise.resolve(
          jsonResponse([{ status: "ACTIVE" }, { status: "CANCELLED" }]),
        );
      }

      throw new Error(`Unexpected fetch URL: ${url}`);
    });
    window.fetch = nativeFetch;

    loadBrowserScript("admin-dashboard.js");
    await flushAsyncWork();

    const usersRequest = nativeFetch.mock.calls.find(
      ([url]) => url === "/api/users",
    );
    expect(usersRequest).toBeTruthy();
    expect(usersRequest[1].headers.Authorization).toBe("Bearer server-jwt");
    expect(window.Auth.requireAdmin).toHaveBeenCalledTimes(1);
    expect(document.getElementById("adminName").textContent).toBe("QA Admin");
    expect(document.getElementById("totalStudents").textContent).toBe("1");
    expect(document.getElementById("totalCourses").textContent).toBe("2");
    expect(document.getElementById("totalEnrollments").textContent).toBe("2");
    expect(document.getElementById("activeEnrollments").textContent).toBe("1");
    expect(
      document.querySelector("#recentStudents tbody").textContent,
    ).toContain("Student One");
  });
});
