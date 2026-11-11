// Laptop-friendly load test.  Run:
//   k6 run -e OWNER_PHONE=9150435807 -e OWNER_PASSWORD=CanKhata2026 loadtest\k6-laptop.js
// 4 minutes, up to 100 virtual users. Each user logs in once, then browses owner pages.
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 20 },
    { duration: '1m',  target: 50 },
    { duration: '1m30s', target: 100 },
    { duration: '1m',  target: 100 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
};

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
let loggedIn = false;

export default function () {
  if (!loggedIn) {
    const r = http.post(`${BASE}/login`, { login: __ENV.OWNER_PHONE, password: __ENV.OWNER_PASSWORD });
    check(r, { 'login ok': (x) => x.status === 200 });
    loggedIn = true;
  }
  check(http.get(`${BASE}/home`), { 'home 200': (x) => x.status === 200 });
  sleep(1);
  check(http.get(`${BASE}/owner/customers`), { 'customers 200': (x) => x.status === 200 });
  sleep(1);
  check(http.get(`${BASE}/owner/customer?id=1`), { 'customer 200': (x) => x.status === 200 });
  sleep(1);
  check(http.get(`${BASE}/owner/sheet?date=2026-11-10`), { 'sheet 200': (x) => x.status === 200 });
  sleep(1);
  check(http.get(`${BASE}/owner/invoices?month=2026-11`), { 'invoices 200': (x) => x.status === 200 });
  sleep(1);
  check(http.get(`${BASE}/health`), { 'health 200': (x) => x.status === 200 });
  sleep(1);
}
