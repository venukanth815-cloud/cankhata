# CanKhata

A web app for water-can delivery businesses. It tracks every can that goes out, every empty that comes back, the deposit held, and the money each customer owes.

**Live demo:** https://cankhata.onrender.com (free tier, the first request after idle can take up to a minute)

Built with plain **Java 17, Jakarta Servlets, JSP/JSTL, JDBC (HikariCP) and MySQL 8**. No Spring, no ORM, no frontend framework. The goal was to understand what those frameworks do for you.

## Screenshots

![Owner dashboard](docs/screenshots/home.png)

## Features

- **Three roles:** owner, delivery boy, customer. Each role only sees its own pages.
- **Customers, routes, can types:** prices and deposits per can type, streets grouped into routes.
- **Standing orders and skips:** a customer can skip a day from their own portal.
- **Daily sheet:** generated per day, delivery boy marks delivered, empties collected, cash received, or missed. Owner does the van check and closes the day.
- **Can ledger:** append-only, so cans in the customer's hands always add up. Lost or damaged cans charge the deposit.
- **Billing:** monthly invoices, payments (idempotent, a double click cannot take cash twice), who owes money.
- **Accounts:** owner can create customer logins and reset any password. Everyone can change their own.
- **Server monitor:** live memory, CPU, threads, GC, DB pool and p95 latency.
- Mobile friendly pages.

## Architecture

```
Browser
  -> EncodingFilter   UTF-8 and security headers (CSP)
  -> RequestLogFilter request id, timing, metrics
  -> AuthFilter       signed cookie, CSRF check
  -> RoleFilter       is this role allowed on this URL
  -> Servlet          reads input, calls one service, picks the JSP
  -> Service          business rules, transactions
  -> DAO              PreparedStatement SQL
  -> HikariCP pool -> MySQL 8
  -> JSP              every value printed with <c:out> (XSS safe)
```

Design choices worth knowing:

- **Stateless login.** The session is an HMAC-signed cookie, so there is no server memory and the app can run as many instances.
- **Money in paise** (integers), never floating point.
- **Transactions with row locks** (`SELECT ... FOR UPDATE`) wherever two requests could touch the same customer balance.
- **POST, redirect, GET** after every form, so refresh never resubmits.
- **One error handler** for every exception type.

## Security

BCrypt passwords, HttpOnly SameSite cookie, CSRF token on every POST, login lockout (5 failures in 10 minutes), the same message for unknown user and wrong password, PreparedStatement everywhere, `<c:out>` in JSP, CSP with no inline scripts, JSPs hidden under `WEB-INF`, no secrets in code (everything from environment variables), audit log of sensitive actions.

## Testing and performance

- JUnit 5 unit tests: validators, signed cookie (tampering, expiry), login rules, password reset and change, schedule, money.
- k6 load test on a laptop (Docker, MySQL 8): **100 virtual users, 0% errors, p95 latency 3.19 ms** across login, home, customers, daily sheet, invoices and health. Script: `loadtest/k6-laptop.js`.

```
k6 run -e OWNER_PHONE=<phone> -e OWNER_PASSWORD=<password> loadtest/k6-laptop.js
```

## Monitoring

| What | Where |
|---|---|
| Alive check | `/health` (503 if the DB does not answer) |
| Metrics JSON | `/metrics` (owner only) |
| Dashboard | `/owner/monitor`, refreshes every 3 seconds |
| Logs | request id on every line and in the `X-Request-Id` header, `SLOW` lines over 1 second |
| GC and OOM | `gc.log`, heap dump on out-of-memory |

## Run locally

You need Docker Desktop.

```
copy .env.example .env        (Windows)   |   cp .env.example .env   (Mac/Linux)
# edit .env: your own passwords, SESSION_SECRET of 32+ random characters
docker compose up --build
```

Open http://localhost:8080 and log in with `OWNER_PHONE` and `OWNER_PASSWORD`. The first owner is created when the users table is empty.

### Settings (environment variables)

| Name | Meaning |
|---|---|
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | MySQL connection |
| `SESSION_SECRET` | 32+ random characters, signs the cookie |
| `OWNER_PHONE`, `OWNER_PASSWORD`, `OWNER_NAME` | first owner (only when no users exist) |
| `POOL_SIZE` | DB connections, default 20 |
| `COOKIE_SECURE` | `true` behind HTTPS |
| `BCRYPT_COST` | default 12 |

## Deployment (free)

Hosted on **Render** (Docker web service, `render.yaml` blueprint, auto deploy on push to `main`) with **Aiven** free MySQL (SSL required). JVM heap is capped at 256 MB and the pool at 5 connections to fit Render's 512 MB. UptimeRobot pings `/health` every 5 minutes to keep the free instance awake.

## Project layout

```
src/main/java/com/cankhata/
  filter/       EncodingFilter, RequestLogFilter, AuthFilter, RoleFilter
  controller/   one servlet per screen
  service/      business rules (Auth, Billing, Delivery, Sheet, Subscription...)
  dao/          SQL
  model/        User, Customer, CanType, Route, Money...
  util/         Config, DbConnection, SessionToken, Sql, Metrics, PasswordUtil...
src/main/resources/   schema.sql, logback.xml
src/main/webapp/      JSP views, CSS, JS
loadtest/             k6 scripts
```

## Known limits

- Login lockout counters live in memory, per instance.
- A signed cookie cannot be revoked before it expires, so a password reset does not log out an old session.
- Free hosting sleeps when idle and has no email (so no forgot-password by email yet).

## Ideas for next

Reports and charts, WhatsApp reminder links, Excel/PDF export, audit log screen, email password reset.
