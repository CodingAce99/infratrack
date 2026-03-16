# CLAUDE.md

Project guidance for [Claude Code](https://claude.ai/code) and contributor onboarding.

## Quick Start

```bash
# Dev profile — H2 in-memory, no Docker required
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Demo profile — PostgreSQL + mock SSH (recruiter-friendly)
docker-compose up -d postgres
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo

# Build & test
./mvnw clean package          # Build JAR
./mvnw test                   # Run all tests (58 passing)
./mvnw test -Dtest=<Class>    # Run a specific test class
```

## Architecture — Hexagonal (Ports & Adapters)

Strict layer boundaries enforced by package structure. The domain has **zero framework dependencies**.

```
com.infratrack/
├── domain/                → Pure Java. Asset entity, value objects (AssetId, IpAddress,
│   ├── model/               Credentials), domain logic. No Spring, no JPA.
│   └── event/             → Domain events as plain Java records (AssetCreatedEvent,
│                            AssetStatusChangedEvent, AssetDeletedEvent).
│
├── application/           → Use cases and port interfaces. Framework-agnostic.
│   ├── port/input/          ManageAssetUseCase (what the app exposes)
│   ├── port/output/         AssetRepository, DomainEventPublisher (what the app requires)
│   └── service/             AssetService (orchestrates use cases, publishes events)
│
└── infrastructure/        → Spring Boot, JPA, REST — all framework code lives here.
    ├── adapter/input/       AssetRestController (REST API)
    ├── adapter/input/dto/   Request/Response DTOs + AssetDtoMapper (HTTP ↔ Domain)
    ├── adapter/output/      JpaAssetRepository, InMemoryAssetRepository,
    │                        SpringEventPublisher, MockMetricsListener
    ├── config/              BeanConfiguration (profile-based wiring, no @Service)
    ├── persistence/         AssetJpaEntity, AssetMapper (Domain ↔ JPA), schema.sql
    └── security/            EncryptedStringConverter (AES-256-GCM)
```

### Design Principles

- **Dependency Rule:** dependencies always point inward toward the domain.
- **Factory methods over constructors:** `Asset.create()` for new entities, `Asset.reconstitute()` for persistence rehydration. Events use `EventClass.of()` factory methods.
- **Self-validating Value Objects:** `IpAddress`, `AssetId`, `Credentials` reject invalid state at construction.
- **Security by construction:** `AssetResponse` has no `password` field — it is impossible to accidentally leak what does not exist.
- **Explicit wiring:** beans are assembled in `BeanConfiguration`, not auto-detected via `@Service`. This makes the dependency graph visible and testable.
- **Two mapper layers with distinct responsibilities:**
  - `AssetDtoMapper` — HTTP ↔ Domain (lives in `adapter/input/dto/`)
  - `AssetMapper` — Domain ↔ JPA (lives in `persistence/`)
- **Domain events after persistence:** events are published only after the repository operation succeeds — never announce what hasn't happened.

## DTO Layer

Request DTOs (inbound) use Bean Validation. Response DTOs enforce security invariants structurally.

## Domain Events

Three domain events published by `AssetService` through the `DomainEventPublisher` port:

| Event | Published after | Payload |
|-------|----------------|---------|
| `AssetCreatedEvent` | `createAsset()` | AssetId, AssetType, timestamp |
| `AssetStatusChangedEvent` | `updateAssetStatus()` | AssetId, AssetStatus, timestamp |
| `AssetDeletedEvent` | `deleteAsset()` | AssetId, timestamp |

Events are plain Java records — no Spring, no JPA. `SpringEventPublisher` implements the port using `ApplicationEventPublisher`. `MockMetricsListener` reacts to `AssetCreatedEvent` generating simulated CPU/memory/disk metrics.

## Security

| Layer | Mechanism |
|-------|-----------|
| At rest (DB) | AES-256-GCM via JPA `AttributeConverter` |
| In transit | HTTPS / TLS 1.3 |
| In responses | `AssetResponse` structurally excludes credentials |
| In logs | `Credentials.toString()` omits password by design |
| Key management | `INFRATRACK_ENCRYPTION_KEY` env var (32 bytes, Base64) |

## Testing Strategy

67 tests passing · JUnit 5 + Mockito · `@Nested` classes with `@DisplayName`

| Layer | Approach | Spring context? |
|-------|----------|-----------------|
| Domain | Pure unit tests on entities and value objects | No |
| Service | Mockito (`@ExtendWith(MockitoExtension.class)`) with mock publisher | No |
| Controller | `@WebMvcTest` + `@MockitoBean` | Slice only |
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
| 3.2 — Domain Events | ✅ Done | Event publishing, mock metrics, `DomainEventPublisher` port |
| 4.1 — Metrics Domain | ✅ Done | MetricSnapshot VO, MonitoringService, JPA layer, mock collector, 67 tests |
| 4.2 — SSH Real | Planned | SSHJ + Alpine containers |
| 4.3 — Scheduling + REST | Planned | Virtual Threads, GET /metrics endpoints |
| 5 — React Dashboard | Planned | Next.js 15 frontend with live metrics |
| 6 — CI/CD | Planned | GitHub Actions pipeline, final polish |

---

<!--
CLAUDE CODE INTERNAL NOTES — operational details for AI-assisted development.
These do not affect the public documentation above.

• PostgreSQL is mapped to port 5433 (not 5432) to avoid conflicts with Docker Desktop internals.
• Encryption key property path: `infratrack.encryption.key` in application.yml.
  Do NOT use `infratrack.security.encryption.key` (legacy path, no longer valid).
• `defer-datasource-initialization=true` must NOT be used — it inverts SQL init / validate order.
• `ddl-auto: update` was discarded because Hibernate 6.x silently aborts DDL generation when
  EncryptedStringConverter lacks a no-arg constructor. Manual schema.sql is the solution.
• JpaAssetRepository has @Repository (so Spring autoconfigures SpringDataAssetRepository)
  but is NOT registered as a @Bean — it's instantiated manually via
  `new JpaAssetRepository(springRepo)` to avoid duplicate bean conflicts.
• MapStruct adoption is deferred to Phase 4+ — manual mappers are intentional for now.
• SpringEventPublisher uses @Component (auto-detected by Spring), not explicit wiring in
  BeanConfiguration. This is intentional — it's a simple infrastructure adapter with no
  profile-specific behavior, unlike repositories.
• MockMetricsListener removed in Sprint 4.1 — replaced by MonitoringService + MockMetricsCollector.
• MockMetricsCollector uses @Component (auto-detected). Will be profile-restricted in Sprint 4.2
  when SshMetricsCollector is added for demo+prod.
• InMemoryMetricSnapshotRepository registered as
  MetricSnapshotRepository bean for dev profile in BeanConfiguration.
• MonitoringService constructor: (AssetRepository, MetricsCollector, MetricSnapshotRepository).
• AssetService constructor now takes two params: (AssetRepository, DomainEventPublisher).
  BeanConfiguration wires both. Tests mock both.
-->
