import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  stages: [
    { duration: "2m", target: 25 },
    { duration: "5m", target: 100 },
    { duration: "2m", target: 0 }
  ],
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<300"],
    checks: ["rate>0.99"]
  }
};

const BASE_URL = __ENV.BASE_URL;

if (!BASE_URL) {
  throw new Error("BASE_URL env var is required. Example: BASE_URL=https://staging.example.com k6 run load-staging.js");
}

export default function () {
  const courses = http.get(`${BASE_URL}/api/courses`);
  check(courses, {
    "courses endpoint returns 200": (r) => r.status === 200
  });

  const health = http.get(`${BASE_URL}/actuator/health`);
  check(health, {
    "actuator health endpoint returns 200": (r) => r.status === 200
  });

  sleep(1);
}
