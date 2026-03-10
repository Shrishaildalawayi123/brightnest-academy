import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  stages: [
    { duration: "2m", target: 100 },
    { duration: "2m", target: 200 },
    { duration: "2m", target: 300 },
    { duration: "1m", target: 0 }
  ],
  thresholds: {
    http_req_failed: ["rate<0.05"],
    http_req_duration: ["p(95)<1500"]
  }
};

const BASE_URL = __ENV.BASE_URL || "http://127.0.0.1:8080";

export default function () {
  const response = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({
      email: __ENV.STRESS_USER_EMAIL || "nonexistent@example.com",
      password: __ENV.STRESS_USER_PASSWORD || "Wrong@123"
    }),
    {
      headers: { "Content-Type": "application/json" }
    }
  );

  check(response, {
    "login endpoint responds (2xx/4xx/429)": (r) => r.status >= 200 && r.status < 500
  });

  sleep(1);
}
