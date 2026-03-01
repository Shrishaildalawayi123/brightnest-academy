import { expect, test } from "@playwright/test";

test("login endpoint enforces rate limits", async ({ request }) => {
  let lastStatus = 0;

  for (let i = 0; i < 7; i++) {
    const response = await request.post("/api/auth/login", {
      data: {
        email: "nonexistent@example.com",
        password: "Wrong@123"
      }
    });
    lastStatus = response.status();
    if (lastStatus === 429) {
      break;
    }
  }

  expect(lastStatus).toBe(429);
});
