# CLAUDE.md

Project guidance for [Claude Code](https://claude.ai/code) and contributor onboarding.

## Quick Start

```bash
# Full demo — PostgreSQL + SSH target + app + dashboard, all containerized
export INFRATRACK_ENCRYPTION_KEY=$(openssl rand -base64 32)
docker-compose up -d
# Dashboard: http://localhost:3000   API: http://localhost:8080

# Dev profile — H2 in-memory, no Docker required
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Demo on host — app runs locally, Docker for services only
docker-compose up -d postgres ssh-target-1
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo

# Frontend dev — requires backend running (any mode)
cd frontend && npm run dev    # http://localhost:3000

# Build & test
./mvnw clean package          # Build JAR
./mvnw test                   # Run all tests (91 passing)
./mvnw test -Dtest=<Class>    # Run a specific test class
```

## Architecture — Hexagonal (Ports & Adapters)

Strict layer boundaries enforced by package structure. The domain has **zero framework dependencies**.

```
com.infratrack/
├── domain/                → Pure Java. Entities, value objects, events.
│   ├── model/               No Spring, no JPA.
│   └── event/             → Domain events as plain Java records.
│
├── application/           → Use cases and port interfaces. Framework-agnostic.
│   ├── port/input/          ManageAssetUseCase, MonitorAssetUseCase
│   ├── port/output/         AssetRepository, DomainEventPublisher,
│   │                        MetricsCollector, MetricSnapshotRepository
│   └── service/             AssetService, MonitoringService
│
└── infrastructure/        → Spring Boot, JPA, REST, SSH — all framework code lives here.
    ├── adapter/input/       AssetRestController, MetricsRestController, MetricsScheduler
    ├── adapter/input/dto/   Request/Response DTOs + mappers (HTTP ↔ Domain)
    ├── adapter/output/      JpaAssetRepository, InMemoryAssetRepository,
    │                        SpringEventPublisher, MockMetricsCollector,
    │                        SshMetricsCollector, JpaMetricSnapshotRepository,
    │                        InMemoryMetricSnapshotRepository
    ├── config/              BeanConfiguration, SchedulingConfiguration
    ├── persistence/         JPA entities, mappers (Domain ↔ JPA)
    └── security/            EncryptedStringConverter (AES-256-GCM)
```

Schema is managed by Flyway migrations under `src/main/resources/db/migration/` (V1__initial_schema.sql is the baseline). See the Database section in internal notes for operational details.

### Design Principles

- **Dependency Rule:** dependencies always point inward toward the domain.
- **Factory methods over constructors:** `Asset.create()` / `Asset.reconstitute()` for entities. `MetricSnapshot.of()` / `MetricSnapshot.reconstruct()` for value objects. `EventClass.of()` for events.
- **Self-validating Value Objects:** `IpAddress`, `AssetId`, `Credentials`, `MetricSnapshot` reject invalid state at construction.
- **Security by construction:** `AssetResponse` has no `password` field — it is impossible to accidentally leak what does not exist.
- **Explicit wiring:** beans are assembled in `BeanConfiguration`, not auto-detected via `@Service`. This makes the dependency graph visible and testable.
- **Two mapper layers with distinct responsibilities:**
  - `AssetDtoMapper` / `MetricSnapshotResponse.from()` — HTTP ↔ Domain (lives in `adapter/input/dto/`)
  - `AssetMapper` / `MetricSnapshotMapper` — Domain ↔ JPA (lives in `persistence/`)
- **Domain events after persistence:** events are published only after the repository operation succeeds — never announce what hasn't happened.
- **Per-asset fault isolation:** Virtual Threads with individual try-catch ensure one SSH failure never blocks collection from other assets.

## DTO Layer

Request DTOs (inbound) use Bean Validation. Response DTOs enforce security invariants structurally.

## Domain Events

Three domain events published by `AssetService` through the `DomainEventPublisher` port:

| Event | Published after | Payload |
|-------|----------------|---------|
| `AssetCreatedEvent` | `createAsset()` | AssetId, AssetType, timestamp |
| `AssetStatusChangedEvent` | `updateAssetStatus()` | AssetId, AssetStatus, timestamp |
| `AssetDeletedEvent` | `deleteAsset()` | AssetId, timestamp |

Events are plain Java records — no Spring, no JPA. `SpringEventPublisher` implements the port using `ApplicationEventPublisher`.

## Monitoring

`MonitoringService` orchestrates the metrics pipeline through three output ports:

| Port | dev | demo / prod |
|------|-----|-------------|
| `MetricsCollector` | `MockMetricsCollector` | `SshMetricsCollector` (SSHJ 0.40.0) |
| `MetricSnapshotRepository` | `InMemoryMetricSnapshotRepository` | `JpaMetricSnapshotRepository` (PostgreSQL) |
| `AssetRepository` | `InMemoryAssetRepository` | `JpaAssetRepository` (PostgreSQL) |

`MetricsScheduler` triggers `collectAllActive()` on a configurable interval (default 60s). Each active asset gets its own Virtual Thread for parallel SSH collection. `MetricsRestController` exposes latest snapshot and historical data via REST.

## Frontend

React dashboard in `frontend/`. Next.js 15 with App Router, TypeScript, Tailwind CSS v4, Recharts, SWR.

### Architecture

- `layout.tsx` (server component) → `page.tsx` (server) → `Dashboard.tsx` (client, `"use client"`)
- Each `AssetCard` owns its SWR call for metrics — avoids Rules of Hooks violation
- `useAssets.ts` hook manages asset list only (single responsibility)
- Static export (`output: 'export'`) — no Node.js server in production, nginx serves HTML

### Component tree

```
Dashboard (client component, useAssets hook)
├── Header (asset count, connection indicator, "+ Add Asset" button)
│   └── CreateAssetModal (modal with 5-field form, POST /api/v1/assets)
└── AssetCard × N (each owns useSWR for metrics, manages its own isEditing state)
    ├── StatusBadge (ACTIVE/MAINTENANCE/INACTIVE pill)
    ├── MetricGauge × 3 (CPU, Memory, Disk)
    │   └── Sparkline (Recharts LineChart, last 20 data points)
    └── EditAssetPanel (inline, opens on Edit click)
        ├── Status section → PUT /{id}/status
        ├── IP section → PUT /{id}/ip
        ├── Credentials section → PUT /{id}/credentials
        └── ConfirmDialog (reusable: message, onConfirm, onCancel) → DELETE /{id}
```

### Data fetching

- SWR with `refreshInterval: 60000` for automatic 60s polling
- `API_BASE_URL = ''` (empty) — relative URLs work in both dev (Next.js rewrites) and Docker (nginx proxy)
- History endpoint (`/metrics/history?limit=20`) provides both latest value and sparkline data
- **Mutations:** `CreateAssetModal` and `EditAssetPanel` call mutation functions in `lib/api.ts` (createAsset, updateStatus, updateIp, updateCredentials, deleteAsset). Each throws `ApiError` with HTTP status on failure (409 duplicate IP, 400 validation, 404 not found). After success, components call `useSWRConfig().mutate('/api/v1/assets')` to revalidate the list — no prop drilling, no manual state management. After delete, no extra cleanup is needed: React unmounts the card and SWR garbage-collects the metrics cache automatically.

### Frontend Docker

- Multi-stage: `node:20-alpine` build → `nginx:alpine` serve (~25MB final image)
- nginx serves static files at `/` and reverse proxies `/api/` to `app:8080`
- No CORS config on Spring Boot — same-origin via nginx

### Design system — Style C (Hybrid Ops/Modern)

- Dark theme: `#0c0f14` (background), `#111621` (cards)
- Sans-serif base + monospace for technical data (hostnames, IPs, metrics)
- Border-left 3px encodes asset status: green (active), amber (maintenance), gray (inactive)
- Metric colors by threshold: green (0-60%), amber (60-80%), red (80-100%)
- Sparklines with smooth curves, area fill gradient, dot on last point

## CI/CD

GitHub Actions workflow (`.github/workflows/ci.yml`) triggers on push and PR to `main`. Runs `./mvnw clean verify -Dspring.profiles.active=dev` — H2 + MockMetricsCollector, zero external services needed. Badge at top of README.

Multi-stage `Dockerfile` at project root: Stage 1 builds with JDK Alpine, Stage 2 runs with JRE Alpine (~200MB final image). Layer caching via separated `pom.xml` copy + `dependency:resolve`.

`docker-compose up -d` starts the full ecosystem: PostgreSQL + SSH target + Infratrack app + frontend dashboard (4 services). The app service overrides datasource URL and SSH port via environment variables for Docker-internal networking.

## Security

| Layer | Mechanism |
|-------|-----------|
| At rest (DB) | AES-256-GCM via JPA `AttributeConverter` |
| In transit | HTTPS / TLS 1.3 |
| In responses | `AssetResponse` structurally excludes credentials |
| In logs | `Credentials.toString()` omits password by design |
| Key management | `INFRATRACK_ENCRYPTION_KEY` env var (32 bytes, Base64) |

## Testing Strategy

91 tests passing · JUnit 5 + Mockito · `@Nested` classes with `@DisplayName`

| Layer | Approach | Spring context? |
|-------|----------|-----------------|
| Domain | Pure unit tests on entities and value objects | No |
| Service | Mockito (`@ExtendWith(MockitoExtension.class)`) with mocked ports | No |
| Controller | `@WebMvcTest` + `@MockitoBean` | Slice only |
| SSH parsing | Static method unit tests, no SSH connection | No |
| Security | Assert password never appears in responses or `toString()` | Varies |

## Environment Variables

**Demo / Prod:**
- `INFRATRACK_ENCRYPTION_KEY` — AES key, 32 bytes, Base64-encoded

**Prod only:**
- `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`

## Roadmap

| Phase | Status | Deliverable |
|-------|--------|-------------|
| 1 — Scaffolding | ✅ Done | Hexagonal structure, Docker Compose, three profiles |
| 2 — Asset CRUD | ✅ Done | Full CRUD, AES-256-GCM encryption end-to-end |
| 3.1 — DTO Layer | ✅ Done | Request/Response DTOs, Bean Validation, security-by-construction |
| 3.2 — Domain Events | ✅ Done | Event publishing, `DomainEventPublisher` port |
| 4.1 — Metrics Domain | ✅ Done | MetricSnapshot VO, MonitoringService, JPA layer, mock collector |
| 4.2 — SSH Real | ✅ Done | SshMetricsCollector via SSHJ 0.40.0, Alpine containers in Docker Compose |
| 4.3 — Scheduling + REST | ✅ Done | collectAllActive(), Virtual Threads, MetricsScheduler, MetricsRestController |
| 5 — React Dashboard | ✅ Done | Next.js 15 + TypeScript dashboard with full CRUD from UI (modal create, inline edit, delete with confirmation), SWR polling and Recharts sparklines |
| 6 — CI/CD | ✅ Done | GitHub Actions pipeline, multi-stage Docker build, full ecosystem containerized |
| 6.5 — Flyway | ✅ Done | Schema versioning via Flyway 11; `schema.sql` replaced by `V1__initial_schema.sql`; `ddl-auto: validate` everywhere |
| 7 — Authentication & Authorization | ⏳ Next | Spring Security + JWT stateless, ADMIN/VIEWER roles, BCrypt, login UI |
| 8 — Observability | Pending | Spring Actuator, Micrometer metrics, structured logging with MDC |
| 9 — Event Streaming | Pending | Apache Kafka pipeline for metrics + alerts (KRaft mode) |
| 10 — Frontend Polish | Pending | Animations, loading skeletons, responsive design, dark/light mode |

---

<!--
CLAUDE CODE INTERNAL NOTES — operational details for AI-assisted development.
These do not affect the public documentation above.

DATABASE & SCHEMA
• PostgreSQL is mapped to port 5433 (host) → 5432 (container) to avoid conflicts with Docker Desktop.
• Encryption key property path: `infratrack.encryption.key` in application.yml.
  Do NOT use `infratrack.security.encryption.key` (legacy path, no longer valid).
• `defer-datasource-initialization=true` must NOT be used — it inverts SQL init / validate order.
• `ddl-auto: validate` in every profile. Hibernate never writes schema — Flyway is the sole owner.
  Dev profile was changed from `create-drop` to `validate` in Sprint 6.5 (create-drop is
  incompatible with Flyway: Hibernate would drop the tables Flyway just created).

FLYWAY (added Sprint 6.5)
• Migrations live in `src/main/resources/db/migration/` and follow the strict naming
  `V<version>__<description>.sql`. Flyway parses the version from the filename and
  applies migrations in order, recording each in `flyway_schema_history`.
• Two dependencies required since Flyway 10+: `flyway-core` (engine) and
  `flyway-database-postgresql` (PostgreSQL adapter). H2 (dev profile) is supported by
  core directly — no extra artifact needed. Do NOT specify versions in pom.xml;
  Spring Boot 3.5 BOM resolves to Flyway 11.x.
• `baseline-on-migrate: true` in application.yml — essential for environments that had
  pre-Flyway schema. Without it, Flyway fails with "table already exists" on first run
  against a populated database.
• Migrations are immutable once applied. Flyway computes a checksum and refuses to run
  if a previously-applied migration has been modified. Schema changes always require a
  NEW V file, never editing an old one.
• `IF NOT EXISTS` clauses are anti-Flyway and must NOT appear in migration files.
  Flyway guarantees exactly-once execution; defensive clauses would mask real errors.
• `flyway_schema_history` row types:
  - `BASELINE` — DB was pre-populated when Flyway first ran (baseline-on-migrate kicked in)
  - `SQL` — Flyway executed the migration from scratch
  Distinction matters for debugging "why is my migration not running?" questions.
• When evolving schema (e.g., adding a table in Sprint 7.1), the workflow is:
  1. Create `V<n>__<description>.sql` with the DDL/DML
  2. Run `./mvnw test` → Flyway picks it up automatically
  3. Run `docker-compose up -d --build` → Flyway applies to PostgreSQL
  4. Verify with `SELECT * FROM flyway_schema_history;`

BEAN WIRING
• JpaAssetRepository has @Repository (so Spring autoconfigures SpringDataAssetRepository)
  but is NOT registered as a @Bean — it's instantiated manually via
  `new JpaAssetRepository(springRepo)` to avoid duplicate bean conflicts.
• MapStruct adoption is deferred — manual mappers are intentional for now.
• SpringEventPublisher uses @Component (auto-detected by Spring), not explicit wiring in
  BeanConfiguration. This is intentional — it's a simple infrastructure adapter with no
  profile-specific behavior, unlike repositories.
• MockMetricsListener removed in Sprint 4.1 — replaced by MonitoringService + MockMetricsCollector.
• MockMetricsCollector: no @Component. Wired explicitly in BeanConfiguration with @Profile("dev").
• SshMetricsCollector: no @Component. Wired explicitly in BeanConfiguration with
  @Profile({"demo", "prod"}) and @Value("${infratrack.ssh.port:22}").
• SshMetricsCollector parsing: done in Java with regex, not shell. Pattern: ([\d.]+)\s*id for CPU.
  Static parse methods are independently testable without SSH connections.
• SshMetricsCollector session handling: one Session per command — SSHJ channels are
  single-use. Reusing the same Session for multiple exec() calls throws
  "This session channel is all used up". The SSHClient connection is reused; only Sessions are not.
• InMemoryMetricsSnapshotRepository (note: class name has plural 'Metrics') registered as
  MetricSnapshotRepository bean for dev profile in BeanConfiguration.
• MonitoringService constructor: (AssetRepository, MetricsCollector, MetricSnapshotRepository).
• AssetService constructor: (AssetRepository, DomainEventPublisher).
  BeanConfiguration wires both services. Tests mock all collaborators.
• IpAddress accepts IPv4 (e.g. 192.168.1.1) and RFC 1123 hostnames (e.g. web-server-01).
  Both CreateAssetRequest and UpdateIpAddressRequest @Pattern updated to match.
  When app runs in Docker, asset IP must be the Docker hostname (web-server-01), not 127.0.0.1.

EXCEPTION HANDLING
• GlobalExceptionHandler (@RestControllerAdvice) handles all exceptions globally.
  Do NOT add local @ExceptionHandler methods in controllers — local handlers take priority
  over @RestControllerAdvice and will intercept exceptions before the global handler.
• DuplicateIpAddressException → 409 Conflict (thrown by createAsset and updateAssetIpAddress).
• AssetNotFoundException → 404 Not Found (thrown by findAsset and all orElseThrow calls).
• updateAssetIpAddress checks existsByIpAddress but skips the check when newIp equals the
  asset's current IP (no-op case).

SCHEDULING
• MetricsScheduler uses @Component (auto-detected). Reads infratrack.monitoring.interval-seconds
  from application.yml (default 60). Virtual Thread spawning happens in MonitoringService.collectAllActive(),
  not in the scheduler — scheduler is a thin trigger only.
• SchedulingConfiguration is a dedicated @Configuration class with @EnableScheduling.
  @EnableScheduling is NOT in BeanConfiguration.

REST ENDPOINTS
• GET /{id}/metrics reuses getHistory(id, 1) — no getLatest() method on the use case (YAGNI).
• GET /{id}/metrics/history returns 200 + empty list when no data. Never 404.

DOCKER & NETWORKING
• SSH target container: user sshuser/sshpass, Alpine 3.19, procps + coreutils.
  Dockerfile at docker/alpine-ssh/Dockerfile. Port 2222 (host) → 22 (container).
• application-demo.yml SSH port: ${INFRATRACK_SSH_PORT:2222}. Running on host → 2222 (mapped).
  Running in Docker → 22 (env override from docker-compose).
• App Dockerfile: multi-stage, eclipse-temurin:21-jdk-alpine (build) → eclipse-temurin:21-jre-alpine (run).
  Layer caching: pom.xml + dependency:resolve first, then source copy.
  chmod +x ./mvnw included for Windows Git execute bit stripping.
• docker-compose: 4 services (postgres, ssh-target-1, app, frontend) on infratrack-network.
  - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/infratrack (Docker-internal, not localhost:5433)
  - INFRATRACK_SSH_PORT=22 (Docker-internal, not 2222)
  - INFRATRACK_ENCRYPTION_KEY=${INFRATRACK_ENCRYPTION_KEY:-yt1+CDm1+...} (host env var with fallback)
  - depends_on postgres with service_healthy condition. No depends_on for ssh-target-1.
• When app runs in Docker, asset IP must be `web-server-01` (Docker hostname), not `127.0.0.1`.
  When app runs on host, asset IP is `127.0.0.1`.

FRONTEND
• Next.js 15 static export (output: 'export') — no Node.js server in production.
• nginx serves static files + reverse proxies /api/ to app:8080. Zero CORS config on Spring Boot.
• SWR per-component pattern: each AssetCard owns its useSWR call for metrics.
  Never call useSWR inside .map() or conditionals — Rules of Hooks violation.
• API_BASE_URL = '' (empty string). Relative URLs work in dev (Next.js rewrites) and Docker (nginx).
• next.config.ts has both `output: 'export'` and `rewrites` — generates harmless build warning. Ignore it.
• Recharts does not support server components — all chart components must be in "use client" files.
• Frontend Dockerfile: node:20-alpine build → nginx:alpine serve (~25MB final image).
• Tailwind v4 uses CSS-first config (@import "tailwindcss" + @theme {} in globals.css).
• React 19: `React.FormEvent` is deprecated. Use `React.SyntheticEvent<HTMLFormElement>` for
  onSubmit handler types. VS Code flags the deprecated form correctly.
• Edit panel state ownership: `isEditing` lives inside each AssetCard via useState. Do NOT
  lift to Dashboard or track a global editingAssetId — each card's panel is fully independent.
• Save-per-section, not Save-all: the backend exposes three separate endpoints for status, IP
  and credentials. Each section in EditAssetPanel has its own Save button mapped 1:1. Do NOT
  attempt a single "Save all" — implementing it would require three sequential calls with
  partial failure handling, effectively a distributed transaction without atomicity guarantees.
• Credentials UX: password is never returned by AssetResponse (security by construction), so
  the password field in EditAssetPanel is always empty when the panel opens. Both username
  and password are required for submission — there is no "username-only" path. If user wants
  to change only username, they must re-enter the current password.
• Mutations use useSWRConfig().mutate('/api/v1/assets') — exact key match required. SWR
  deduplicates by string identity, so '/api/v1/assets/' (trailing slash) silently fails to
  revalidate. Verify the key in hooks/useAssets.ts before calling mutate elsewhere.

CI/CD
• GitHub Actions: .github/workflows/ci.yml. Temurin 21, Maven cache, chmod +x ./mvnw.
• CI uses dev profile (H2 + mock). No Docker services in CI pipeline.
• Dummy INFRATRACK_ENCRYPTION_KEY env var set in CI to satisfy Spring placeholder resolution
  at startup (dev profile doesn't use encryption but base application.yml resolves the variable).
• Node.js 20 deprecation warning in CI — deadline June 2026. Non-blocking.
-->
