import { beforeEach, describe, expect, it, vi } from "vitest";
import { loadBrowserScript } from "../test-utils/loadBrowserScript.js";

describe("Auth helper", () => {
  beforeEach(() => {
    loadBrowserScript("api.js");
    loadBrowserScript("auth.js");
  });

  it("stores token and user profile on successful login", async () => {
    window.API.login = vi.fn().mockResolvedValue({
      id: 99,
      name: "QA Admin",
      email: "qa-admin@example.com",
      role: "ADMIN",
      token: "server-jwt",
    });

    const response = await window.Auth.login(
      "qa-admin@example.com",
      "Admin@123",
    );
    expect(response.role).toBe("ADMIN");

    const storedUser = JSON.parse(localStorage.getItem("user"));
    expect(storedUser).toEqual({
      id: 99,
      name: "QA Admin",
      email: "qa-admin@example.com",
      role: "ADMIN",
    });
    expect(localStorage.getItem("token")).toBe("server-jwt");
  });

  it("correctly identifies admin and student roles", () => {
    localStorage.setItem("user", JSON.stringify({ role: "ADMIN" }));
    expect(window.Auth.isAdmin()).toBe(true);
    expect(window.Auth.isStudent()).toBe(false);

    localStorage.setItem("user", JSON.stringify({ role: "STUDENT" }));
    expect(window.Auth.isAdmin()).toBe(false);
    expect(window.Auth.isStudent()).toBe(true);
  });
});
