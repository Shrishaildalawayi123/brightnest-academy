import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  vus: 5,
  duration: "30s",
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<800"]
  }
};

const BASE_URL = __ENV.BASE_URL || "http://127.0.0.1:8080";

export default function () {
  const health = http.get(`${BASE_URL}/health`);
  check(health, {
    "health returns 200": (r) => r.status === 200
  });

  const courses = http.get(`${BASE_URL}/api/courses`);
  check(courses, {
    "courses returns 200": (r) => r.status === 200
  });

  sleep(1);
}
