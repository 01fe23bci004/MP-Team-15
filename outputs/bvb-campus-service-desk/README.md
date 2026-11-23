# BVB Campus Service Desk

A local, semester-scope service request and dispatch prototype for BVB, Hubballi. It implements the Gate 0/Gate 1 requirements: request intake and priority ordering, technician eligibility, atomic concurrent assignment, and traceable status updates. Payments, GPS tracking, mobile apps, AI prediction, external providers, and cloud deployment are outside the approved scope.

## Run on macOS

Requirements: Java 21 and MySQL. Install with Homebrew (`brew install openjdk@21 mysql`) or use the official installers. In a new Terminal window, make the Homebrew JDK available and start MySQL:

```bash
export JAVA_HOME="$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
brew services start mysql
```

Then create a database and local app user:

```sql
CREATE DATABASE bvb_campus;
CREATE USER 'bvb_app'@'localhost' IDENTIFIED BY 'bvb_local_password';
GRANT ALL PRIVILEGES ON bvb_campus.* TO 'bvb_app'@'localhost';
```

From this folder, double-click `run-local.command` or run `./mvnw spring-boot:run` (the wrapper downloads Maven on first use), then open http://localhost:8080. The backend creates the schema and demo records at startup. Configure `DB_URL`, `DB_USER`, `DB_PASSWORD` to use another local MySQL account.

Demo logins (username / password):

| Role | Login |
|---|---|
| Admin | `admin` / `Admin@123` |
| Requester | `student` / `Student@123` |
| Faculty | `faculty` / `Faculty@123` |
| Non-teaching staff | `staff` / `Staff@123` |
| Technician | `tech.electrical` / `Tech@123` |
| Technician | `tech.plumbing` / `Tech@123` |

Change demo passwords before any non-demo use. Admin can reset technician passwords; the app stores BCrypt hashes and never reveals plaintext credentials.

## Local demo database fallback

If MySQL is unavailable, set `SPRING_PROFILES_ACTIVE=demo`. This selects an embedded H2 database in MySQL compatibility mode. H2 is only a local demonstration fallback; MySQL remains the target database. The fallback is in-memory and resets when the app stops.

## Architecture and engines

- Browser UI is served by Spring Boot from `src/main/resources/static`; REST API is under `/api`.
- Spring Boot, Java 21, Spring Data JPA/Hibernate, MySQL, Java request threads, BCrypt password hashing, and JUnit 5.
- E1 validates requests and keeps a stable priority/FIFO ordering (HIGH, MEDIUM, LOW).
- E2 filters technicians by required skill, service location, active/available state, and capacity.
- E3 uses a fair JVM lock plus a transactional pessimistic row lock and a unique active-assignment constraint to prevent duplicate concurrent allocation.
- E4 records assignment and status history in the same transaction; only valid lifecycle transitions are accepted.

### Data model

`app_user` stores requester/admin/technician identities and password hashes. `technician_profile` stores skills, service locations, availability and capacity. `service_category` and `service_subcategory` define admin-managed intake options and default priorities. `service_request` stores requester, category, location, priority, queue sequence and current status. `service_assignment` links a request to a technician and lifecycle status. `status_event` provides an append-only trace of changes.

### State flow

`SUBMITTED → ASSIGNED → ACCEPTED → IN_PROGRESS → COMPLETED`; an administrator may cancel a non-completed request. Requests without a matching technician remain `QUEUED`; admin can retry assignment after technician availability changes. Invalid transitions are rejected without modifying prior state.

### API overview

`POST /api/auth/login`; `GET /api/me`; `GET/POST /api/requests`; `GET /api/requests/{id}`; `GET /api/requests/{id}/events`; `POST /api/requests/{id}/assign`; `PATCH /api/requests/{id}/status`; `GET /api/queue`; `GET /api/admin/accounts`; `GET/POST/PATCH /api/admin/technicians`; `POST /api/admin/technicians/{id}/reset-password`; `GET/POST/PATCH /api/admin/categories`; `GET /api/admin/overview`. Role checks are enforced server-side. Requesters can view only their own requests; technicians can view/update only their assignments; admins can manage the whole workflow.

## Verification

Run `./mvnw test`. Tests cover priority/FIFO order, eligibility filters, legal state changes, and concurrent allocation safety. The UI has requester, technician, and admin modes with a shared login, live request timeline, queue, technician management, categories, and reset-password flow. It auto-refreshes periodically; this is a local workflow tracker, not GPS tracking.
