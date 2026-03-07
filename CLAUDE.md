# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with this repository.

## Build & Run Commands

```bash
# Run with dev profile (H2 in-memory, no Docker required)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Run with demo profile (PostgreSQL + mock SSH)
docker-compose up -d postgres
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo

# Build JAR
./mvnw clean package

# Run all tests
./mvnw test

# Run a specific test class
./mvnw test -Dtest=AssetRestControllerTest

# Skip tests during build
./mvnw clean package -DskipTests
```

## Architecture

**Hexagonal Architecture (Ports & Adapters)** — strict boundaries between layers:

```
domain/           → Pure Java, no frameworks. Contains Asset entity, value objects
                    (AssetId, IpAddress, Credentials) and domain logic.

application/      → Use cases and ports. No Spring. Contains:
                    - port/input/: ManageAssetUseCase (what the app exposes)
                    - port/output/: AssetRepository (what the app requires)
                    - service/: AssetService (use case implementation)

infrastructure/   → Spring Boot, JPA, REST. Contains:
                    - adapter/input/: AssetRestController
                    - adapter/input/dto/: Request/Response DTOs + AssetDtoMapper
                    - adapter/output/: JpaAssetRepository, InMemoryAssetRepository
                    - config/: BeanConfiguration (profile-based bean wiring)
                    - persistence/: AssetJpaEntity, AssetMapper (Domain↔JPA), schema.sql
                    - security/: EncryptedStringConverter (AES-256-GCM)
```

**Key Rules:**
- The domain layer has zero framework dependencies (enforced)
- All Spring/JPA annotations stay in the infrastructure layer
- Domain objects use factory methods: `Asset.create()`, `Asset.reconstitute()`
- Value objects are immutable and self-validating (throw on invalid input)
- `AssetService` and domain objects are instantiated via `BeanConfiguration` (not `@Service`)
- `JpaAssetRepository` has `@Repository` but is NOT registered with `@Bean` — instantiated
  manually via `new JpaAssetRepository(springRepo)` to avoid duplicate bean conflicts
- `InMemoryAssetRepository` for `dev` profile; `JpaAssetRepository` for `demo`/`prod`

## DTO Layer

DTOs live in `infrastructure/adapter/input/dto/` and are split by responsibility:

**Request DTOs** (inbound) — with Bean Validation:
- `CreateAssetRequest` — name, type, IP address, username, password
- `UpdateStatusRequest` — new status (`ACTIVE` | `INACTIVE` | `MAINTENANCE`)
- `UpdateCredentialsRequest` — new username and password
- `UpdateIpAddressRequest` — new IP address

**Response DTO** (outbound):
- `AssetResponse` — never contains a `password` field (security by construction)

**HTTP ↔ Domain Mapper:**
- `AssetDtoMapper` — converts Request DTOs to domain objects, and `Asset` to `AssetResponse`
- Distinct from `AssetMapper` (Domain ↔ JPA), which lives in `infrastructure/persistence/`

## Profiles

| Profile | Database | SSH | DDL | When to use |
|---------|----------|-----|-----|-------------|
| `dev` | H2 in-memory | Mock | `create-drop` | Fast iteration, no Docker |
| `demo` | PostgreSQL:5433 | Realistic mock | `validate` | Integration tests, demos |
| `prod` | PostgreSQL (env vars) | Real | `validate` | Production deployment |

The `assets` table schema is defined in `src/main/resources/schema.sql` (managed manually).
`ddl-auto: validate` (base) + `ddl-auto: create-drop` (dev profile override).

## API Endpoints

All endpoints under `/api/v1/assets`. All write operations use `@RequestBody` with typed DTOs:

| Method | Path | Request Body | Response |
|--------|------|--------------|----------|
| `GET` | `/` | — | `List<AssetResponse>` |
| `GET` | `/{id}` | — | `AssetResponse` |
| `POST` | `/` | `CreateAssetRequest` | `AssetResponse` (201) |
| `PUT` | `/{id}/status` | `UpdateStatusRequest` | `AssetResponse` |
| `PUT` | `/{id}/credentials` | `UpdateCredentialsRequest` | `AssetResponse` |
| `PUT` | `/{id}/ip` | `UpdateIpAddressRequest` | `AssetResponse` |
| `DELETE` | `/{id}` | — | 204 No Content |

## Testing

**57 tests passing.** Uses JUnit 5 with Mockito. Test conventions:
- Nested test classes with `@Nested` and `@DisplayName`
- Domain tests: pure unit tests (no Spring, no mocks)
- Service tests: plain Mockito (`@ExtendWith(MockitoExtension.class)`)
- Controller tests: `@WebMvcTest` + `@MockitoBean` for the use case
- Security tests: assert that `password` never appears in any response or `toString()`

## Environment Variables

Required for demo/prod:
- `INFRATRACK_ENCRYPTION_KEY` — AES key, 32 bytes, Base64-encoded

Required for prod only:
- `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`

## Key Design Decisions

- **`JpaAssetRepository` bean wiring:** Has `@Repository` so Spring Boot autoconfigures the
  underlying `SpringDataAssetRepository`, but is NOT registered with `@Bean` in
  `BeanConfiguration` — instantiated manually to avoid duplicate bean conflicts.
- **Encryption key path:** `infratrack.encryption.key` in `application.yml`.
  Do NOT use `infratrack.security.encryption.key` (incorrect path from earlier versions).
- **`AssetResponse` has no `password` field by design** — security by construction, not by
  annotations. It is impossible to accidentally leak something that does not exist.
- **`ManageAssetUseCase.createAsset(Asset)`** accepts a fully-built `Asset` object. Object
  construction is the mapper's responsibility, not the use case or controller.
- **`Credentials.toString()`** omits the password deliberately.
- **PostgreSQL port:** 5433 (5432 is occupied by Docker Desktop's internal PostgreSQL).
- **Manual schema management:** `schema.sql` + `spring.sql.init.mode=always`. `ddl-auto: update`
  was discarded — Hibernate silently skipped DDL generation on empty PostgreSQL databases.
- **`defer-datasource-initialization=true`** must NOT be used — it inverts the SQL/validate order.
- **MapStruct** deferred to Phase 4+ — manual mappers used for pedagogical clarity.

## Project Status

### Completed

**Phase 1 — Scaffolding:**
- Full hexagonal package structure
- Docker Compose with PostgreSQL 17 (port 5433)
- dev / demo / prod profiles

**Phase 2 — Asset CRUD + Encryption:**
- Full Asset CRUD end-to-end with demo profile
- AES-256-GCM encryption via `EncryptedStringConverter` with manual schema
- `AssetMapper` (Domain ↔ JPA) in persistence layer
- 51 tests passing at phase close

**Sprint 3.1 — DTO Layer:**
- 4 Request DTOs with Bean Validation
- `AssetResponse` without `password` field (security by construction)
- `AssetDtoMapper` in `infrastructure/adapter/input/dto/`
- `AssetRestController` refactored to `@RequestBody` + typed DTOs
- `ManageAssetUseCase.createAsset()` updated to `(Asset asset)` signature
- New tests: `AssetDtoMapperTest`, `AssetResponseTest`, `AssetRestControllerTest`
- **57 tests passing**

### Next: Sprint 3.2 — Domain Events + Mock Metrics

- Domain event publishing with `ApplicationEventPublisher`
- Mock metrics (CPU, memory, disk) for demo mode
- Async event listeners on Virtual Threads
