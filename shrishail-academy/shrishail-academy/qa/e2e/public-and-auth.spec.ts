import { expect, test } from "@playwright/test";

test("health endpoint returns UP", async ({ request }) => {
  const response = await request.get("/health");
  expect(response.status()).toBe(200);
  const body = await response.json();
  expect(body.status).toBe("UP");
});

test("invalid login shows error alert", async ({ page }) => {
  await page.goto("/login.html");
  await page.fill("#email", "invalid-user@example.com");
  await page.fill("#password", "Wrong@123");
  await page.click("#loginBtn");
  await expect(page.locator("#errorAlert")).toBeVisible();
});

test("public courses endpoint is accessible without authentication", async ({ request }) => {
  const response = await request.get("/api/courses");
  expect(response.status()).toBe(200);
  const body = await response.json();
  expect(Array.isArray(body)).toBe(true);
});
