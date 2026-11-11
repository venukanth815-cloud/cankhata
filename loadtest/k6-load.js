// Load test with k6 (free): https://k6.io  ->  k6 run -e OWNER_PHONE=9876543210 -e OWNER_PASSWORD=ChangeMe123 loadtest/k6-load.js
// Ramps from 0 to 500 virtual users, holds, then ramps down. Thresholds = the pass/fail targets from the design document.
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '2m', target: 50 },
    { duration: '3m', target: 200 },
    { duration: '5m', target: 500 },
    { duration: '10m', target: 500 },   // hold; make this 30m for a soak test (memory leaks show up here)
    { duration: '1m', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],      // under 1% errors
    http_req_duration: ['p(95)<500'],    // 95% of requests under 500 ms
  },
};

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
let loggedIn = false;                    // each virtual user has its own copy

export default function () {
  if (!loggedIn) {                       // log in ONCE per user (BCrypt is deliberately slow)
    const r = http.post(`${BASE}/login`, { login: __ENV.OWNER_PHONE, password: __ENV.OWNER_PASSWORD });
    check(r, { 'login ok': (x) => x.status === 200 });
    loggedIn = true;
  }
  check(http.get(`${BASE}/home`), { 'home 200': (x) => x.status === 200 });
  sleep(1);
  const page = 1 + Math.floor(Math.random() * 5);
  check(http.get(`${BASE}/owner/users?page=${page}`), { 'users 200': (x) => x.status === 200 });
  sleep(1);
  check(http.get(`${BASE}/health`), { 'health 200': (x) => x.status === 200 });
  sleep(1);
}
