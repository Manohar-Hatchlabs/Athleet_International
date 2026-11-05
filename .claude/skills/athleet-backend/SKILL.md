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

```
backend/
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

## When adding a feature

1. Identify the program (core / ajsdp / uksc) and the feature package; create the five sub-packages if missing.
2. Add a Flyway migration in that module's schema folder.
3. Entity → repository → service (with scope check + audit) → DTOs → controller.
4. Document the endpoint with OpenAPI annotations so web/mobile clients can be generated.
