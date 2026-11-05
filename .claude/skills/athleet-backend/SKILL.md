---
name: athleet-backend
description: Conventions and architecture for the Athleet backend (Spring Boot modular monolith serving the Parent app, Staff app and Web admin for the AJSDP and UKSC programs). Use whenever adding, changing or reviewing anything under backend/ — new features, entities, endpoints, database migrations, or deciding which module code belongs in.
---

# Athleet Backend

One Spring Boot backend shared by the Web admin, Parent app and Staff app.
It serves two programs that are **kept strictly separate**:

- **AJSDP** – Athleet Junior Sports Development Program (school-based; zone → cluster → school).
- **UKSC** – United Kolkata Sports Club football academy (academy locations, admissions, players).

Source of truth for requirements: the Athleet Platform Functionality Document
(https://claude.ai/artifact/DQLuehzhzvhBoUcZkJsb3g). Feature IDs (P1…, S1…, W1…) come from it.

## Stack

- Java 21, Spring Boot 3.x, Maven multi-module build
- PostgreSQL (installed locally for development; no Docker), Flyway migrations
- Spring Security with our own JWT (short-lived access token + refresh token)
- S3-compatible file storage (local folder in dev, AWS S3 Mumbai in prod)
- OpenAPI/Swagger for the API contract consumed by web and mobile
- No unit tests and no CI/CD for now — do not add test folders, test dependencies or pipelines unless asked.

## Module layout

Maven multi-module: `backend/pom.xml` is the parent (Spring Boot parent, shared versions);
each module has its own `pom.xml`. Build from `backend/` with `mvn clean package`.

```
backend/
├── pom.xml          parent POM
├── athleet-core/    shared, program-agnostic platform code
├── athleet-ajsdp/   everything specific to AJSDP
├── athleet-uksc/    everything specific to UKSC
└── athleet-app/     Spring Boot entry point + configuration; assembles the modules
```

Packages: `com.athleet.core.*`, `com.athleet.ajsdp.*`, `com.athleet.uksc.*`, `com.athleet.app`.

### athleet-core (shared)
| Package | Responsibility |
|---|---|
| `auth` | Parent mobile + OTP login, staff email/phone + password login, JWT/refresh tokens, password reset |
| `iam` | Staff users, roles, scope assignments (multiple per user), scope enforcement on every request |
| `program` | Program registry (AJSDP, UKSC), per-program module toggles, global settings (escalation hours, default geofence radius) |
| `notification` | Push (FCM), SMS (OTP, absence, fee alerts only), notification inbox |
| `storage` | File storage abstraction (local / S3) |
| `audit` | Audit log of who changed what and when (attendance, scores, fees, roles/scopes) |
| `consent` | Parent consents, withdrawal, data export / deletion requests (DPDP Act) |
| `support` | Parent support tickets |
| `common/*` | `error`, `paging`, `excel`, `pdf`, `security`, `config` helpers |

### athleet-ajsdp
`org` (zones, clusters, schools, GPS points) · `student` · `registration` (self-registration approval, bulk Excel upload) ·
`schedule` (batches, sessions, holidays, coach assignment) · `attendance` (coach check-in, student attendance, session logs) ·
`approval` (Cluster Manager per session → Zonal Manager weekly → Program Lead weekly, 48h escalation) · `sync` (offline queue from Staff app) ·
`assessment` · `reportcard` · `fees` · `announcement` · `dashboard`

### athleet-uksc
`academy` (locations, GPS points) · `player` · `admission` · `batch` · `attendance` ·
`approval` (Academy Admin per session, 48h escalation to Super Admin, no weekly step) · `sync` ·
`assessment` (football skills) · `observation` (coach notes/ratings) · `development` (player development reports) ·
`reportcard` · `fees` · `announcement` · `dashboard`

### Inside each feature package
```
<feature>/
├── web/         REST controllers (thin: validate, call service, map to DTO)
├── service/     business logic and transactions
├── domain/      JPA entities, enums, value objects
├── repository/  Spring Data repositories
└── dto/         request/response records
```

## Hard rules

1. **Never mix programs.** `athleet-ajsdp` must not depend on `athleet-uksc` or vice versa. Both may depend on `athleet-core`; core never depends on either program module. If something looks shared, it goes into core only when it is genuinely program-agnostic — otherwise duplicate it in each program.
2. **Separate database schemas**: `core`, `ajsdp`, `uksc`. Migrations live in
   `athleet-core/src/main/resources/db/migration/core`, `athleet-ajsdp/.../db/migration/ajsdp`, `athleet-uksc/.../db/migration/uksc`.
   No foreign keys across `ajsdp` ↔ `uksc`. Program tables may reference `core` (users, parents).
   Name files `V<yyyyMMddHHmm>__<description>.sql`; never edit an applied migration.
3. **API paths are program-scoped**: `/api/v1/core/...`, `/api/v1/ajsdp/...`, `/api/v1/uksc/...`. Within a program, split by client where useful: `/admin/`, `/staff/`, `/parent/`.
4. **Scope enforcement everywhere**: every query is filtered by the caller's role + scope; parents only see their linked children. Never return data without the scope check.
5. **Audit**: any change to attendance, scores, fees or role/scope assignments writes an audit record.
6. **Offline sync**: Staff app records carry a client-generated UUID; sync endpoints are idempotent on that ID. Server re-validates geofence and resolves conflicts, logging them to the audit log.
7. **Text externalised**: user-facing messages and report card labels come from message bundles (English now, Bengali/Hindi later).
8. **Data in India**: no service or storage outside Indian regions.

## Key product decisions (from discussion)

- Parents: one mobile number per student; children linked automatically by that number.
- Absence alerts (push + SMS) go out **only after** the session's attendance is approved.
- Geofence default 200 m, configurable per school/academy; checked on device and server.
- Fitness grades 1–5 per category from admin-configured age/gender benchmark tables.
- Report cards are published manually per school/batch.
- Payments: Razorpay online + manual cash/UPI/bank entry; GST configurable per school/academy; invoice numbers per financial year.
- OTP SMS provider not chosen yet — in dev, log OTPs instead of sending.

## Security: write SAST-clean code

Code must pass static security analysis (SonarQube, Semgrep, SpotBugs/FindSecBugs, Snyk Code, Checkmarx-style rules) without suppressions. Write it right the first time:

**Injection**
- Use Spring Data derived queries, JPQL/Criteria with bind parameters (`:name`), or `JdbcTemplate` with `?` placeholders. **Never** build SQL/JPQL by string concatenation or `String.format`, including `ORDER BY` — map sort fields through an allow-list enum.
- No `Runtime.exec`/`ProcessBuilder` with user input. No user input in file paths: generate storage keys server-side (UUID) and never use the uploaded filename as a path.
- No user input in log format strings or LDAP/XPath/regex built at runtime; escape or reject.

**Authentication & access control**
- Every endpoint is authenticated unless explicitly listed as public in the security config (OTP request/verify, login, refresh, Razorpay webhook). Deny by default.
- Check role **and** scope in the service layer (not only `@PreAuthorize`), and check object ownership on every by-ID lookup (IDOR): load by `id AND scope`, return 404 if outside scope.
- Passwords hashed with `BCryptPasswordEncoder`/`DelegatingPasswordEncoder` only. OTPs: 6 digits from `SecureRandom`, stored hashed, 5-minute expiry, max 5 attempts, rate-limited per number and IP.
- JWT: sign with a strong key from config (HS256 ≥ 256-bit or RS256), always verify signature, `exp`, `iss`; never accept `alg=none`. Refresh tokens stored hashed, rotated on use, revocable.
- Razorpay webhooks: verify the HMAC signature before trusting any payload; amounts always recomputed server-side from the invoice.

**Secrets & config**
- No secrets, keys, passwords or tokens in code, `application.yml` or Git. Read from environment variables (`${DB_PASSWORD}`); commit only `application-example.yml` placeholders.
- No hard-coded credentials in seed data or migrations.

**Data exposure**
- Never return JPA entities from controllers — always DTOs. Never serialise password hashes, OTPs, tokens or internal IDs that aren't needed.
- Don't log PII (child names, phone numbers, medical notes), tokens or OTPs. Mask phone numbers in logs (`98xxxxxx21`).
- Global `@RestControllerAdvice` returns a generic error body; no stack traces or SQL messages to clients (`server.error.include-stacktrace=never`).

**Input & files**
- Validate every request DTO with `@Valid` + Bean Validation (`@NotNull`, `@Size`, `@Pattern`, `@Min/@Max`). Bound sizes of strings, lists and page size (max 100).
- File uploads: allow-list content type **and** extension, enforce size limits, check magic bytes, store outside web root, serve via pre-signed URLs.
- Excel uploads (Apache POI): protect against zip bombs/XXE — use POI defaults with `ZipSecureFile` limits; never enable external entities in any XML parser.
- No Java native deserialisation (`ObjectInputStream`); Jackson without default typing.

**Web**
- CORS restricted to known web-admin origins (no `*` with credentials). CSRF disabled only because the API is stateless JWT with no cookies.
- Security headers on (HSTS, X-Content-Type-Options, frame-options deny). HTTPS only in prod.
- Use `SecureRandom` for anything security-related; never `Math.random`/`Random`.
- Keep dependencies on the Spring Boot BOM versions; don't pin old versions of transitive libraries.

## Design patterns and code structure

- **Layered per feature**: `web → service → repository`. Controllers are thin (validate, call one service method, return DTO). Business rules live only in services/domain.
- **DTOs as Java `record`s**; mapping via small static mapper methods or MapStruct — no entity leaks.
- **Constructor injection only** (`final` fields, no `@Autowired` on fields).
- **Strategy** for swappable providers behind core interfaces: `SmsSender` (log-only in dev, MSG91 etc. in prod), `PushSender`, `FileStorage` (local / S3), `PaymentGateway` (Razorpay). Select implementation via `@ConditionalOnProperty`.
- **State machine** for workflows with explicit enums and allowed transitions in one place: session attendance (`DRAFT → SUBMITTED → APPROVED | REJECTED → RESUBMITTED`), registration/admission, invoice (`DUE → PARTIALLY_PAID → PAID | REFUNDED | WAIVED`). Reject illegal transitions with a domain exception.
- **Domain events** (`ApplicationEventPublisher` + `@TransactionalEventListener(phase = AFTER_COMMIT)`) for side effects: attendance approved → absence alerts; report card published → parent notification; any audited change → audit record. Keeps modules decoupled and notifications out of the main transaction.
- **Specification/Criteria** for dynamic list filters (scope + search + filters) instead of many repository methods.
- **Template method** for shared flows that differ per program (e.g. report card generation, bulk upload validation) — the base lives in `core/common`, each program extends it in its own module.
- **Scheduler** (`@Scheduled` + ShedLock if more than one instance) for 48h escalation, fee invoice generation and reminders. Jobs must be idempotent.
- **Idempotency key** (client UUID) on sync and payment endpoints.
- Custom exceptions (`NotFoundException`, `ForbiddenException`, `BusinessRuleException`) mapped centrally to HTTP status — no `try/catch` returning error strings in controllers.
- Use `java.time` (`Instant` for timestamps in UTC, `LocalDate` for dates, zone `Asia/Kolkata` only at display/reporting). Money as `BigDecimal` with scale 2, never `double`.
- Avoid: god services, static utility classes holding state, circular module dependencies, business logic in entities' getters, `Optional` as fields/parameters.

## Performance

**Database & JPA**
- All associations `FetchType.LAZY` (including `@ManyToOne`). Load what a screen needs with `JOIN FETCH`, `@EntityGraph` or DTO projections — **avoid N+1** queries. Keep `spring.jpa.open-in-view=false`.
- List/report endpoints use projections (interface/record DTOs) and return only needed columns.
- **Always paginate** lists (`Pageable`, max page size 100). For large/infinite lists (attendance history, audit log) prefer keyset pagination on `(created_at, id)`.
- Index every foreign key and every column used in `WHERE`/`ORDER BY` of hot queries (scope columns: `zone_id`, `cluster_id`, `school_id`, `academy_id`, `batch_id`, `session_date`). Add indexes in the same migration as the table.
- Batch writes: `hibernate.jdbc.batch_size=50`, `order_inserts/order_updates=true`; use sequence-based IDs (or UUID generated in app), not `IDENTITY`, so batching works. Bulk Excel upload and sync use `saveAll` in chunks, not row-by-row transactions.
- Keep transactions short: `@Transactional(readOnly = true)` on read services; no remote calls (SMS, push, S3, Razorpay) inside a DB transaction — publish an event and do it after commit.
- Dashboards/aggregates computed with SQL `GROUP BY` (or pre-aggregated summary tables refreshed by a job), never by loading rows into Java and looping.
- Use HikariCP defaults sized to the DB (start with pool size 10–20); watch slow-query logs (`log_min_duration_statement`).

**API & app**
- Heavy work runs async (`@Async` with a bounded `ThreadPoolTaskExecutor`): bulk uploads, report card PDF generation, bulk notifications, Excel/PDF exports. Return a job ID and let the client poll / get notified.
- Stream large exports (Excel via SXSSF streaming workbook, CSV streaming) instead of building them in memory.
- Cache rarely changing reference data (programs, benchmark tables, org structure, fee plans) with Spring Cache (Caffeine); evict on update.
- Files go through pre-signed S3 URLs — the backend never proxies large file bytes.
- Sync endpoint accepts batched payloads (e.g. up to 500 records) and processes them in one transaction per batch.
- Enable response compression; keep JSON responses lean (no nested object graphs).
- Use `Instant`-based filters with indexed date columns instead of functions on columns in `WHERE` (e.g. avoid `date(created_at) = ?`).

## When adding a feature

1. Identify the program (core / ajsdp / uksc) and the feature package; create the five sub-packages if missing.
2. Add a Flyway migration in that module's schema folder.
3. Entity → repository → service (with scope check + audit) → DTOs → controller.
4. Document the endpoint with OpenAPI annotations so web/mobile clients can be generated.
