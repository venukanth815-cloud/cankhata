# 💧 CanKhata - water can delivery and deposit ledger

Core Java 17 + Servlets + JSP + JDBC + MySQL 8 + Bootstrap 5. **No Spring, no Hibernate, no React.**
Tracks every can that leaves the depot, every empty that comes back, and every rupee owed.

Design document: see the CanKhata design doc (architecture, ER diagram, business rules R1 to R14, API list, roadmap).

## Release 1 (this version)
Project skeleton, full database schema, connection pool, login with roles, security filters, responsive pages,
staff management, health and metrics endpoints, logging, load test script, unit tests.

| URL | Who | What |
| --- | --- | --- |
| `/login` | everyone | Phone or email + password |
| `/home` | logged in | Dashboard, cards depend on role |
| `/owner/users` | owner | List, search, paginate, add staff |
| `/health` | public | `{"status":"UP"}` if the database answers (503 if not) |
| `/metrics` | owner | Memory, CPU, threads, GC, DB pool, traffic, p95 latency (JSON) |
| `/api/me` | logged in | Who am I (JSON) |

## Run it locally (free)
You need Docker Desktop. Nothing else.
```
copy .env.example .env      (Windows)   |   cp .env.example .env   (Mac/Linux)
# edit .env: set your own passwords and a SESSION_SECRET of 32+ random characters
docker compose up --build
```
Open http://localhost:8080 and log in with `OWNER_PHONE` / `OWNER_PASSWORD` from your `.env`.
The first owner is created automatically the first time the app starts.

Without Docker: install JDK 17, Maven, Tomcat 10.1 and MySQL 8; create database `cankhata_db`; set the environment
variables listed below; run `mvn clean package`; copy `target/cankhata.war` to Tomcat's `webapps/ROOT.war`.

### Settings (environment variables, nothing is hardcoded)
| Name | Meaning |
| --- | --- |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | MySQL connection (required) |
| `SESSION_SECRET` | 32+ random characters, signs login cookies (required) |
| `OWNER_PHONE`, `OWNER_PASSWORD`, `OWNER_NAME` | first owner, only used when the users table is empty |
| `POOL_SIZE` | database connections, default 20 |
| `COOKIE_SECURE` | `true` when served over HTTPS (set it on Render) |
| `BCRYPT_COST` | default 12 |
| `LOG_DIR`, `LOG_LEVEL` | log folder (default `logs`) and level (default INFO) |

## Tests
```
mvn test
```
Unit tests (JUnit 5): validators, signed cookie (tampering, expiry), login rules (lockout, roles, duplicates, hashing).

## Monitoring (all free)
| Check | How |
| --- | --- |
| Is it alive? | open `/health` |
| Memory, CPU, threads, GC, traffic, p95 | log in as owner, open `/metrics` |
| DB connections (app side) | `/metrics` -> `dbPool`: `active`, `idle`, `waitingThreads` (waiting above 0 for long = pool too small or a slow query) |
| DB connections (MySQL side) | `docker compose exec db mysql -uroot -p -e "SHOW PROCESSLIST; SHOW STATUS LIKE 'Threads_connected';"` |
| Live graphs | install VisualVM (free), add a JMX connection to `localhost:9010` (docker compose only; no password, local use only) |
| Logs | `docker compose exec app tail -f /usr/local/tomcat/logs/cankhata.log`; find one request with `grep "\[a1b2c3d4\]"` (the id is also in the `X-Request-Id` response header) |
| Slow requests | lines starting with `SLOW` (over 1 second) |
| GC / out-of-memory | `gc.log` in the same folder; a `.hprof` heap dump appears there if memory runs out (open with Eclipse MAT) |
| Slow SQL | MySQL slow query log is on (queries over 1 s) |

## Load test (free, on your own PC)
Do not load test the free Render plan: it has 512 MB RAM and sleeps. Test locally.
1. Install k6 from https://k6.io (free).
2. `k6 run -e OWNER_PHONE=9876543210 -e OWNER_PASSWORD=ChangeMe123 loadtest/k6-load.js`
3. Watch `/metrics`, `docker stats` and `SHOW PROCESSLIST` while it runs.
Targets: 500 virtual users, 95% of requests under 500 ms, under 1% errors. Add the results to `docs/load-test.md`.
Seeding 10,000 customers is part of release 7, once the customer module exists.

## Deploy free (Render + Aiven MySQL)
1. Aiven free MySQL (same as your Car Service project). Use `DB_URL=jdbc:mysql://HOST:PORT/defaultdb?sslMode=REQUIRED&serverTimezone=UTC`.
2. Render: New Web Service from this GitHub repo, runtime Docker.
3. Environment: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `SESSION_SECRET`, `OWNER_PHONE`, `OWNER_PASSWORD`, `COOKIE_SECURE=true`, and `PORT=8080` (Tomcat listens on 8080).
4. Health check path: `/health`.

## How a request travels (read this for interviews)
```
Browser -> EncodingFilter (UTF-8, security headers)
        -> RequestLogFilter (request id, timing, metrics)
        -> AuthFilter (signed cookie, CSRF check)
        -> RoleFilter (is this role allowed on this URL?)
        -> Servlet (reads input, calls ONE service, picks the JSP)
        -> Service (business rules, validation)
        -> DAO (SQL with PreparedStatement) -> HikariCP pool -> MySQL
        -> JSP (HTML, every value printed with <c:out> to stop XSS)
```
Errors anywhere are thrown as exceptions and handled in one place: `ErrorHandler`.

## Folder map
```
src/main/java/com/cankhata/
  filter/      EncodingFilter, RequestLogFilter, AuthFilter, RoleFilter
  controller/  BaseServlet, LoginServlet, LogoutServlet, HomeServlet, UsersServlet, AppInitListener, api/*
  service/     AuthService, AuditService, AppContext
  dao/         UserDao, AuditDao
  model/       User, SessionUser, Role
  exception/   AppException and its children
  util/        Config, DbConnection (pool), SessionToken (signed cookie + CSRF), Validator,
               PasswordUtil (BCrypt), Metrics, ErrorHandler, SchemaInitializer, Json, RequestUtil
src/main/resources/  schema.sql (all 16 tables), logback.xml
src/main/webapp/     WEB-INF/web.xml, WEB-INF/views/*.jsp, assets/
```

## Security in release 1
BCrypt passwords; signed HttpOnly SameSite cookie (no server memory, so the app can scale to many instances);
CSRF token on every POST; login lock after 5 wrong attempts (10 minutes); same error message for unknown user and wrong password;
PreparedStatement everywhere; `<c:out>` in JSP; security headers (CSP, no framing, no sniffing);
no secret in code or logs; JSPs hidden under `WEB-INF`.

## Known limits
- Login lockout counters are per server instance (in memory).
- A signed cookie cannot be revoked before it expires (30 minutes).
- Maven could not run where this code was written; unit tests were run against stand-ins for the servlet API, pool and BCrypt.
  Run `mvn test` on your PC first, and send any error text back.

## What is built (all screens)

| Who | Screen | URL |
|---|---|---|
| Owner | Staff, Customers, Routes, Can types | `/owner/users`, `/owner/customers`, `/owner/routes`, `/owner/cantypes` |
| Owner | One customer's account: standing orders, skips, payments, deposit, lost/damaged cans, create customer login | `/owner/customer?id=` |
| Owner | Daily sheet: generate, van check, close day | `/owner/sheet` |
| Owner | Billing: monthly invoices, who owes money | `/owner/invoices` |
| Delivery boy | Today's stops: delivered / empties / cash / missed | `/driver/today` |
| Customer | Cans held, balance, skip a day, bills | `/customer/home` |

First run order: Can types -> Staff (add a delivery boy) -> Routes -> Customers -> open a customer's Account and add a standing order -> Daily sheet -> Generate.
Run the unit tests with `mvn test`. The SQL for the accounting screens has not been run against MySQL yet, so report any error from the logs.
