import { beforeEach, describe, expect, it, vi } from "vitest";
import { loadBrowserScript } from "../test-utils/loadBrowserScript.js";

describe("API helper", () => {
  beforeEach(() => {
    loadBrowserScript("api.js");
  });

  it("adds Authorization header when token exists", () => {
    localStorage.setItem("token", "jwt-token");
    const headers = window.API.getHeaders(true);
    expect(headers.Authorization).toBe("Bearer jwt-token");
  });

  it("adds CSRF header for mutating requests", async () => {
    document.cookie = "XSRF-TOKEN=test-csrf-token; path=/";

    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ success: true }), {
        status: 200,
        headers: { "Content-Type": "application/json" }
      })
    );
    global.fetch = fetchMock;

    await window.API.request("/contact", {
      method: "POST",
      headers: window.API.getHeaders(),
      body: JSON.stringify({ name: "Test" })
    });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [, options] = fetchMock.mock.calls[0];
    expect(options.headers["X-CSRF-Token"]).toBe("test-csrf-token");
  });

  it("clears client-side session on 401 responses", async () => {
    localStorage.setItem("token", "expired-token");
    localStorage.setItem("user", JSON.stringify({ email: "user@example.com" }));

    global.fetch = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ error: "Unauthorized - please login" }), {
        status: 401,
        headers: { "Content-Type": "application/json" }
      })
    );

    await expect(
      window.API.request("/users/me", {
        headers: window.API.getHeaders(true)
      })
    ).rejects.toThrow();

    expect(localStorage.getItem("token")).toBeNull();
    expect(localStorage.getItem("user")).toBeNull();
  });
});
