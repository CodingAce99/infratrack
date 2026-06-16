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
./mvnw test                   # Run all tests (146 passing)
./mvnw test -Dtest=<Class>    # Run a specific test class
```

## Architecture — Hexagonal (Ports & Adapters)

Strict layer boundaries enforced by package structure. The domain has **zero framework dependencies**.

```
com.infratrack/
├── domain/                → Pure Java. Entities, value objects, events.
│   ├── model/               No Spring, no JPA. Asset + User aggregates.
│   └── event/             → Domain events as plain Java records.
│
├── application/           → Use cases and port interfaces. Framework-agnostic.
│   ├── port/input/          ManageAssetUseCase, MonitorAssetUseCase, AuthenticateUserUseCase
│   ├── port/output/         AssetRepository, DomainEventPublisher,
│   │                        MetricsCollector, MetricSnapshotRepository,
│   │                        UserRepository, PasswordEncoder, TokenGenerator, TokenValidator (+ TokenClaims record)
│   └── service/             AssetService, MonitoringService, AuthenticationService
│
└── infrastructure/        → Spring Boot, JPA, REST, SSH — all framework code lives here.
    ├── adapter/input/       AssetRestController, MetricsRestController, MetricsScheduler,
    │                        AuthRestController
    ├── adapter/input/dto/   Request/Response DTOs + mappers (HTTP ↔ Domain)
    ├── adapter/output/      JpaAssetRepository, InMemoryAssetRepository,
    │                        SpringEventPublisher, MockMetricsCollector,
    │                        SshMetricsCollector, JpaMetricSnapshotRepository,
    │                        InMemoryMetricSnapshotRepository, JpaUserRepository,
    │                        BCryptPasswordEncoderAdapter, JjwtTokenGenerator, JjwtTokenValidator
    ├── config/              BeanConfiguration, SchedulingConfiguration
    │                        SecurityConfig
    ├── persistence/         JPA entities, mappers (Domain ↔ JPA)
    └── security/            EncryptedStringConverter (AES-256-GCM)
                             JwtAuthenticationFilter (OncePerRequestFilter, validates Bearer, populates SecurityContext)
                             RestAuthenticationEntryPoint (401 JSON)
                             RestAccessDeniedHandler (403 JSON)
```

Schema is managed by Flyway migrations under `src/main/resources/db/migration/` (V1__initial_schema.sql is the baseline). See the Database section in internal notes for operational details.

### Design Principles

- **Dependency Rule:** dependencies always point inward toward the domain.
- **Factory methods over constructors:** `Asset.create()` / `Asset.reconstitute()` for entities. `MetricSnapshot.of()` / `MetricSnapshot.reconstruct()` for value objects. `EventClass.of()` for events.
- **Self-validating Value Objects:** `IpAddress`, `AssetId`, `Credentials`, `MetricSnapshot`, `Username`, `EncodedPassword`, `UserId` reject invalid state at construction.
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
| User passwords | BCrypt one-way hash via the `PasswordEncoder` port — never reversible, never AES-encrypted |
| Authentication | Stateless JWT (HS256), issued at login, 1h expiry, no server-side session |
| Authorization | URL-based role rules in the SecurityFilterChain: reads (`GET /api/v1/assets/**`) allowed to any authenticated role, writes (POST/PUT/DELETE) require ADMIN, login is public |
| In transit | HTTPS / TLS 1.3 |
| In responses | `AssetResponse` structurally excludes credentials |
| In logs | `Credentials.toString()` omits password by design |
| Key management | `INFRATRACK_ENCRYPTION_KEY` env var (32 bytes, Base64) |

## Testing Strategy

146 tests passing · JUnit 5 + Mockito · `@Nested` classes with `@DisplayName`

| Layer | Approach | Spring context? |
|-------|----------|-----------------|
| Domain | Pure unit tests on entities and value objects | No |
| Service | Mockito (`@ExtendWith(MockitoExtension.class)`) with mocked ports | No |
| Controller | `@WebMvcTest` + `@MockitoBean` | Slice only |
| SSH parsing | Static method unit tests, no SSH connection | No |
| Security | Assert password never appears in responses or `toString()` | Varies |

## Environment Variables

**Demo / Prod:**
**Demo / Prod:**
- `INFRATRACK_ENCRYPTION_KEY` — AES key, 32 bytes, Base64-encoded
- `INFRATRACK_JWT_SECRET` — HMAC-SHA256 signing key, >= 32 bytes. Demo has a default; prod has none (fail-fast on missing key).

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
| 7.1 — User persistence | ✅ Done | User domain (Username, EncodedPassword, UserRole), JPA + BCrypt, seed admin/viewer via Flyway V2/V3 |
| 7.2 — JWT + login | ✅ Done | Login use case + `POST /api/v1/auth/login` returning a signed JWT (HS256, 1h) |
| 7.3 — Security filter + roles | ✅ Done | Real `SecurityFilterChain`, JWT validation filter, role enforcement (ADMIN write / VIEWER read) |
| 7.4 — Login UI + token storage | ⏳ Next | Frontend login page, token in React context |
| 7.5 — Protected routes + 401/403 | Pending | End-to-end auth flow from the browser |
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
• JpaAssetRepository has NO @Repository annotation. It is the output adapter that implements
  the AssetRepository port and wraps SpringDataAssetRepository (the actual Spring Data JPA
  interface). It IS registered as a @Bean explicitly in BeanConfiguration via
  `new JpaAssetRepository(springRepo)`, gated by @Profile({"demo","prod"}). The dev profile
  uses InMemoryAssetRepository instead. Spring Data autoconfigures the
  SpringDataAssetRepository interface; the adapter class itself is never component-scanned.
• JpaUserRepository follows the same pattern: no @Repository, implements the UserRepository
  port, wraps SpringDataUserRepository, registered as a @Bean in BeanConfiguration with
  @Profile({"demo","prod"}). There is currently NO dev-profile UserRepository bean — nothing
  in dev depends on it yet. Sprint 7.2 decides whether the login service needs a dev wiring
  (InMemoryUserRepository) or whether Mockito mocks suffice for its tests.
• BCryptPasswordEncoderAdapter implements the PasswordEncoder port and lives in adapter/output
  (NOT in security/, despite the name). Registered as the `passwordEncoder` @Bean with NO
  @Profile — available in every profile, since hashing is needed everywhere.
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

AUTHENTICATION & USERS (added Sprint 7.1 — foundation only, no auth flow yet)
• Spring Security is on the classpath ONLY to use BCryptPasswordEncoder as a hashing utility.
  There is NO real authentication yet. Adding spring-boot-starter-security auto-configures
  HTTP Basic on every endpoint, which would 401 everything and break all tests. The
  placeholder SecurityConfig (anyRequest permitAll; csrf/httpBasic/formLogin disabled)
  neutralizes that. It carries a TEMPORARY comment — Sprint 7.3 rewrites it entirely.
• SecurityConfig must be `public` (not package-private) so @WebMvcTest slices can
  @Import(SecurityConfig.class). The security auto-config is on the classpath in the slice,
  but the permitAll config is not loaded unless imported — so without the import, controller
  slice tests 401. Every @WebMvcTest class needs @Import(SecurityConfig.class).
• password_hash does NOT use EncryptedStringConverter. BCrypt is a one-way hash; layering AES
  on top is pointless and would break matches() (it would compare a hash against an
  encrypted-then-decrypted value). AES is for Asset credentials, which must be decrypted to
  open SSH sessions; BCrypt is for user passwords, which are only ever verified, never
  recovered. Two different problems, two different tools.
• PasswordEncoder is an Infratrack port (application/port/output), NOT Spring Security's
  org.springframework.security.crypto.password.PasswordEncoder. The domain depends on our
  port; the Spring class is an implementation detail hidden inside the adapter.
• UserRepository.save() returns User (asymmetric with AssetRepository.save(), which is void).
  Intentional — save-returns-entity is the safer default for entities that may gain state on
  persist; Asset's void save predates the convention and is left as-is.
• Schema: users table created by V2__add_users_table.sql. id VARCHAR(36) (consistent with
  assets), password_hash VARCHAR(72) (BCrypt is 60 chars; margin without waste), CHECK
  constraint chk_user_role IN ('ADMIN','VIEWER'). Two demo users (admin/viewer) seeded by
  V3__seed_default_users.sql with real BCrypt hashes. Demo credentials only.

JWT LOGIN (Sprint 7.2)
• jjwt 0.13 needs THREE artifacts: jjwt-api (compile), jjwt-impl (runtime), jjwt-jackson
  (runtime). Missing a runtime artifact compiles cleanly and then crashes on the FIRST login
  request (not at startup) with "no implementation found". Confusing because the build is green.
• Use the modern API only: Jwts.builder().subject().claim().issuedAt().expiration().signWith(key)
  .compact(); and Jwts.parser().verifyWith(key).build().parseSignedClaims(). The old setX /
  signWith(SignatureAlgorithm, secret) / parseClaimsJws API is gone in 0.12+.
• JjwtTokenGenerator builds the SecretKey ONCE in its constructor via Keys.hmacShaKeyFor().
  Fail-fast: a weak key (< 32 bytes for HS256) throws WeakKeyException at STARTUP, not on first
  login. INFRATRACK_JWT_SECRET must be >= 32 bytes.
• TokenGenerator is an output port; jjwt never appears in application/ or domain/. Same isolation
  pattern as PasswordEncoder.
• Uniform-failure login: AuthenticationService throws the SAME InvalidCredentialsException for
  user-not-found, wrong-password, AND malformed username. The malformed case is caught
  (InvalidUsernameException) inside login() and rethrown before the repository is queried, so the
  username format rules are never leaked. GlobalExceptionHandler maps it to 401.
• type:"Bearer" is assigned in AuthDtoMapper (REST layer), NOT in AuthenticationResult. The
  application result knows nothing about HTTP; "Bearer" is an Authorization-header protocol const.
• The whole auth stack (AuthenticationService, JjwtTokenGenerator, AuthRestController) is
  @Profile({"demo","prod"}) because it depends on UserRepository, which has no dev bean. No auth
  in dev, by design. The dev context still starts cleanly (none of these beans are created).
• @WebMvcTest GOTCHA: a controller annotated @Profile returns 404 for every request in a slice,
  because the slice runs under the default profile and Spring never registers the controller. Fix:
  @ActiveProfiles("demo") on the test class (plus @Import(SecurityConfig.class) as before). A 404
  in a @WebMvcTest almost always means the controller is absent from the slice context.

SECURITY FILTER + AUTHORIZATION (Sprint 7.3)
• Two SecurityFilterChain beans, profile-split like repositories/collectors:
  @Profile("dev") permits everything (dev has no TokenValidator/filter beans, by design);
  @Profile({"demo","prod"}) is the real secured chain. SecurityConfig stays public so
  @WebMvcTest slices can @Import it.
• TokenValidator is a NEW output port (mirror of TokenGenerator); JjwtTokenValidator is its
  adapter. The filter depends on the port, never on jjwt directly. validate() throws
  InvalidTokenException (domain.exception, mirrors InvalidCredentialsException) on
  bad/expired/malformed tokens.
• ROLE_ PREFIX GOTCHA: hasRole("ADMIN") checks for the authority ROLE_ADMIN. The JWT claim
  stores "ADMIN" (no prefix). JwtAuthenticationFilter must build the authority as
  new SimpleGrantedAuthority("ROLE_" + claims.role()). Get this wrong → every authenticated
  request 403s even with a valid ADMIN token.
• Filter-level rejections (401/403) NEVER reach @RestControllerAdvice — they happen before the
  request hits the controller. That is why RestAuthenticationEntryPoint (401) and
  RestAccessDeniedHandler (403) exist; they write {"error":"..."} directly to match
  GlobalExceptionHandler's body shape (Map.of("error", msg)).
• DOUBLE-REGISTRATION GOTCHA: Spring Boot auto-registers any Filter bean as a servlet filter,
  so a JWT filter added via addFilterBefore would run twice per request. Fix: a
  FilterRegistrationBean<JwtAuthenticationFilter> with setEnabled(false) keeps the bean available
  to the security chain while disabling the duplicate servlet registration.
• requestMatchers ORDER matters (first match wins): login permitAll FIRST, then GET assets/**
  for any role, then assets/** (non-GET) ADMIN only, then anyRequest authenticated. Inverting the
  GET rule and the write rule would let a VIEWER write.
• Uniform rejection: missing token and invalid/expired token both yield the same generic 401
  (no leak of why), same spirit as 7.2's uniform login failure. The filter does not short-circuit
  on a bad token; it just doesn't authenticate, and the authorization layer produces the 401.
• @WebMvcTest IMPACT: controller slices that @Import(SecurityConfig.class) now need a matching
  profile, because both chains are profile-gated. AssetRestControllerTest and
  MetricsRestControllerTest use @ActiveProfiles("dev") (permitAll chain, no TokenValidator needed).
  AuthRestControllerTest stays @ActiveProfiles("demo") and adds @MockitoBean TokenValidator so the
  secured chain's filter dependency is satisfied; login stays permitAll so its assertions hold.

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
