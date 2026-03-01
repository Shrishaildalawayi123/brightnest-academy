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
    http_req_duration: ["p(95)<500"],
    checks: ["rate>0.99"]
  }
};

const BASE_URL = __ENV.BASE_URL || "http://127.0.0.1:8080";

export default function () {
  const publicApi = http.get(`${BASE_URL}/api/courses`);
  check(publicApi, {
    "public api status 200": (r) => r.status === 200
  });

  const health = http.get(`${BASE_URL}/actuator/health`);
  check(health, {
    "actuator health status 200": (r) => r.status === 200
  });

  sleep(1);
}
