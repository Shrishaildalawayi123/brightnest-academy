import { expect, test } from "@playwright/test";

test("protected endpoint rejects anonymous access", async ({ request }) => {
  const response = await request.get("/api/users/me");
  expect(response.status()).toBe(401);
});

test("logout requires CSRF token when auth cookie exists", async ({ request }) => {
  const email = `e2e-${Date.now()}@example.com`;
  const password = "Student@123!";

  const registerResponse = await request.post("/api/auth/register", {
    data: {
      name: "E2E Student",
      email,
      password,
      phone: "9876543210"
    }
  });
  expect(registerResponse.status()).toBe(200);

  const logoutWithoutCsrf = await request.post("/api/auth/logout");
  expect(logoutWithoutCsrf.status()).toBe(403);

  const state = await request.storageState();
  const csrfToken = state.cookies.find((cookie) => cookie.name === "XSRF-TOKEN")?.value;
  expect(csrfToken).toBeTruthy();

  const logoutWithCsrf = await request.post("/api/auth/logout", {
    headers: { "X-CSRF-Token": csrfToken! }
  });
  expect(logoutWithCsrf.status()).toBe(200);
});
