# CLAUDE.md

Project guidance for [Claude Code](https://claude.ai/code) and contributor onboarding.

## Quick Start

```bash
# Dev profile — H2 in-memory, no Docker required
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Demo profile — PostgreSQL + real SSH to Alpine containers (recruiter-friendly)
docker-compose up -d
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo

# Build & test
./mvnw clean package          # Build JAR
./mvnw test                   # Run all tests (81 passing)
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
    ├── persistence/         JPA entities, mappers (Domain ↔ JPA), schema.sql
    └── security/            EncryptedStringConverter (AES-256-GCM)
```

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

## Security

| Layer | Mechanism |
|-------|-----------|
| At rest (DB) | AES-256-GCM via JPA `AttributeConverter` |
| In transit | HTTPS / TLS 1.3 |
| In responses | `AssetResponse` structurally excludes credentials |
| In logs | `Credentials.toString()` omits password by design |
| Key management | `INFRATRACK_ENCRYPTION_KEY` env var (32 bytes, Base64) |

## Testing Strategy

81 tests passing · JUnit 5 + Mockito · `@Nested` classes with `@DisplayName`

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
| 5 — React Dashboard | Planned | Next.js 15 frontend with live metrics |
| 6 — CI/CD | Planned | GitHub Actions pipeline, final polish |

---

<!--
CLAUDE CODE INTERNAL NOTES — operational details for AI-assisted development.
These do not affect the public documentation above.

• PostgreSQL is mapped to port 5433 (not 5432) to avoid conflicts with Docker Desktop internals.
• SSH target container maps port 2222 (host) → 22 (container). App connects to 127.0.0.1:2222
  when running on host. infratrack.ssh.port=2222 in application-demo.yml, default 22 for prod.
• Encryption key property path: `infratrack.encryption.key` in application.yml.
  Do NOT use `infratrack.security.encryption.key` (legacy path, no longer valid).
• `defer-datasource-initialization=true` must NOT be used — it inverts SQL init / validate order.
• `ddl-auto: update` was discarded because Hibernate 6.x silently aborts DDL generation when
  EncryptedStringConverter lacks a no-arg constructor. Manual schema.sql is the solution.
• JpaAssetRepository has @Repository (so Spring autoconfigures SpringDataAssetRepository)
  but is NOT registered as a @Bean — it's instantiated manually via
  `new JpaAssetRepository(springRepo)` to avoid duplicate bean conflicts.
• MapStruct adoption is deferred to Phase 5+ — manual mappers are intentional for now.
• SpringEventPublisher uses @Component (auto-detected by Spring), not explicit wiring in
  BeanConfiguration. This is intentional — it's a simple infrastructure adapter with no
  profile-specific behavior, unlike repositories.
• MockMetricsListener removed in Sprint 4.1 — replaced by MonitoringService + MockMetricsCollector.
• MockMetricsCollector: no @Component. Wired explicitly in BeanConfiguration with @Profile("dev").
• SshMetricsCollector: no @Component. Wired explicitly in BeanConfiguration with
  @Profile({"demo", "prod"}) and @Value("${infratrack.ssh.port:22}").
• SshMetricsCollector parsing: done in Java with regex, not shell. Pattern: ([\d.]+)\s*id for CPU.
  Static parse methods are independently testable without SSH connections.
• InMemoryMetricsSnapshotRepository (note: class name has plural 'Metrics') registered as
  MetricSnapshotRepository bean for dev profile in BeanConfiguration.
• MonitoringService constructor: (AssetRepository, MetricsCollector, MetricSnapshotRepository).
• AssetService constructor: (AssetRepository, DomainEventPublisher).
  BeanConfiguration wires both services. Tests mock all collaborators.
• MetricsScheduler uses @Component (auto-detected). Reads infratrack.monitoring.interval-seconds
  from application.yml (default 60). Virtual Thread spawning happens in MonitoringService.collectAllActive(),
  not in the scheduler — scheduler is a thin trigger only.
• SchedulingConfiguration is a dedicated @Configuration class with @EnableScheduling.
  @EnableScheduling is NOT in BeanConfiguration.
• GET /{id}/metrics reuses getHistory(id, 1) — no getLatest() method on the use case (YAGNI).
• GET /{id}/metrics/history returns 200 + empty list when no data. Never 404.
• Docker Alpine SSH target: user sshuser/sshpass, Alpine 3.19, procps + coreutils installed.
  Dockerfile at docker/alpine-ssh/Dockerfile.
-->
