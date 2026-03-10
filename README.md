# Infratrack

**IT infrastructure inventory and monitoring system** built with Java 21, Spring Boot 3.5, and PostgreSQL 17.

Infratrack bridges the gap between physical inventory and the logical state of an IT infrastructure — providing a single source of truth for assets, credentials, and real-time health metrics.

> **Why this project?** Managing IT assets typically means scattered spreadsheets, stale CMDBs, and credentials stored in plaintext. Infratrack solves this with a clean REST API backed by encrypted storage and proactive SSH monitoring — built on enterprise-grade architecture patterns.

---

## Highlights

- **Hexagonal Architecture** — Domain layer with zero framework dependencies. Swappable adapters for REST, JPA, SSH, and in-memory storage.
- **Domain Events** — Async event bus decouples asset lifecycle from reactions (metrics, auditing, notifications). Events are pure Java records; listeners are infrastructure adapters. Adding new reactions requires zero changes to the domain or service layer.
- **Security by construction** — SSH credentials encrypted with AES-256-GCM at rest. API responses structurally cannot contain passwords (`AssetResponse` has no password field — not hidden, *absent*).
- **Three execution profiles** — `dev` (H2, instant feedback), `demo` (PostgreSQL + simulated data), `prod` (real infrastructure).
- **Virtual Threads** — Java 21 Virtual Threads enabled for non-blocking I/O across SSH connections and async event processing.
- **58 tests** across domain, service, and REST layers — including dedicated security tests that verify credentials never leak.

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Runtime | Java 21 LTS (Virtual Threads) |
| Framework | Spring Boot 3.5 |
| Database | PostgreSQL 17 (Docker) |
| Encryption | AES-256-GCM via JPA AttributeConverter |
| SSH | SSHJ 0.40 *(upcoming)* |
| Frontend | React 19 + Next.js 15 *(upcoming)* |
| Infrastructure | Docker Compose, GitHub Actions |
| Testing | JUnit 5, Mockito |

---

## Architecture

```
                    ┌───────────────────────────────────────┐
                    │           INFRASTRUCTURE              │
                    │                                       │
   HTTP Request ──▶ │  REST Controller                      │
                    │       │                               │
                    │       ▼                               │
                    │  ┌─────────┐    ┌────────────────┐    │
                    │  │DTO Layer│    │  JPA Adapter   │    │
                    │  │ Mapper  │    │  (PostgreSQL)  │    │
                    │  └────┬────┘    └───────▲────────┘    │
                    │       │                 │             │
                    ├───────┼─────────────────┼─────────────┤
                    │       ▼                 │             │
                    │  ┌─────────┐    ┌───────┴────────┐    │
                    │  │ UseCase │───▶│  Repository    │    │
                    │  │ (Port)  │    │  (Port)        │    │
                    │  └────┬────┘    └────────────────┘    │
                    │       │                               │
                    │       ├───▶ DomainEventPublisher (Port)│
                    │       │          APPLICATION          │
                    ├───────┼───────────────────────────────┤
                    │       ▼                               │
                    │   Asset  ·  IpAddress  ·  Credentials │
                    │   AssetId ·  AssetType  · AssetStatus │
                    │   AssetCreatedEvent · AssetDeletedEvent│
                    │   AssetStatusChangedEvent              │
                    │              DOMAIN                   │
                    └───────────────────────────────────────┘
```

The **Dependency Rule** is strictly enforced: all dependencies point inward. The domain knows nothing about Spring, JPA, or HTTP. Value objects (`IpAddress`, `Credentials`, `AssetId`) are self-validating and immutable. Domain entities use factory methods (`Asset.create()`, `Asset.reconstitute()`) instead of public constructors.

Two dedicated mapper layers keep concerns separated: `AssetDtoMapper` translates between HTTP and the domain, while `AssetMapper` translates between the domain and JPA entities.

### Domain Events

Asset lifecycle changes publish domain events through a port interface (`DomainEventPublisher`). The domain defines the events as plain Java records; infrastructure adapters implement publishing (via Spring `ApplicationEventPublisher`) and listening. This decouples the service from any downstream reactions — adding a new listener requires zero changes to existing code.

| Event | Trigger | Data |
|-------|---------|------|
| `AssetCreatedEvent` | Asset successfully persisted | AssetId, AssetType, timestamp |
| `AssetStatusChangedEvent` | Status updated | AssetId, new AssetStatus, timestamp |
| `AssetDeletedEvent` | Asset removed | AssetId, timestamp |

---

## Quick Start

### Prerequisites

- **Java 21** — [Eclipse Temurin](https://adoptium.net/)
- **Docker Desktop** — for PostgreSQL in demo/prod profiles

### Option 1: Demo mode (recommended for evaluation)

```bash
git clone https://github.com/CodingAce99/infratrack.git
cd infratrack

# Generate an encryption key
export INFRATRACK_ENCRYPTION_KEY=$(openssl rand -base64 32)

# Start PostgreSQL and run the application
docker-compose up -d postgres
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

### Option 2: Dev mode (no Docker needed)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Run tests

```bash
./mvnw test
```

The API is available at `http://localhost:8080/api/v1/assets`.

---

## API Reference

All endpoints under `/api/v1/assets`:

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/` | List all assets |
| `GET` | `/{id}` | Get asset by ID |
| `POST` | `/` | Create a new asset |
| `PUT` | `/{id}/status` | Update asset status |
| `PUT` | `/{id}/credentials` | Update SSH credentials |
| `PUT` | `/{id}/ip` | Update IP address |
| `DELETE` | `/{id}` | Delete asset |

### Example: Create an asset

```bash
curl -X POST http://localhost:8080/api/v1/assets \
  -H "Content-Type: application/json" \
  -d '{
    "name": "web-server-01",
    "type": "SERVER",
    "ipAddress": "192.168.1.10",
    "username": "admin",
    "password": "s3cr3t"
  }'
```

Response — note that `password` is never returned:

```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "name": "web-server-01",
  "type": "SERVER",
  "ipAddress": "192.168.1.10",
  "status": "ACTIVE",
  "username": "admin"
}
```

### Asset types and statuses

**Types:** `SERVER` · `ROUTER` · `IOT_DEVICE`

**Statuses:** `ACTIVE` · `INACTIVE` · `MAINTENANCE`

---

## Security Model

| Concern | Approach |
|---------|----------|
| Credentials at rest | AES-256-GCM encryption via JPA `AttributeConverter` |
| Credentials in transit | HTTPS / TLS 1.3 |
| Credentials in API responses | `AssetResponse` structurally has no password field |
| Credentials in logs | `Credentials.toString()` omits sensitive data |
| Key management | Environment variable (`INFRATRACK_ENCRYPTION_KEY`) |

The encryption converter is transparent to the domain — it operates at the JPA layer, so business logic works with plain `Credentials` objects while persistence handles encryption automatically.

---

## Testing

58 tests passing across four layers:

| Layer | Strategy | Spring context |
|-------|----------|----------------|
| Domain | Pure unit tests — entities, value objects, validation | None |
| Application | Mockito-based service tests with mock publisher | None |
| REST | `@WebMvcTest` with mocked use cases | Slice |
| Security | Verify passwords never appear in responses or `toString()` | Varies |

```bash
./mvnw test                          # All tests
./mvnw test -Dtest=AssetServiceTest  # Specific class
```

---

## Configuration

### Environment variables

| Variable | Required | Description |
|----------|----------|-------------|
| `INFRATRACK_ENCRYPTION_KEY` | demo + prod | AES-256 key, 32 bytes, Base64-encoded |
| `DATABASE_URL` | prod | PostgreSQL JDBC URL |
| `DATABASE_USER` | prod | Database username |
| `DATABASE_PASSWORD` | prod | Database password |

### Profiles

| Profile | Database | SSH | Best for |
|---------|----------|-----|----------|
| `dev` | H2 in-memory | Mock | Fast local iteration, no Docker |
| `demo` | PostgreSQL (Docker) | Realistic mock | Integration testing, demonstrations |
| `prod` | PostgreSQL (external) | Real SSHJ | Production deployment |

---

## Roadmap

| Phase | Status | Description |
|-------|--------|-------------|
| 1 — Scaffolding | ✅ | Hexagonal package structure, Docker Compose, profile system |
| 2 — Asset CRUD + Encryption | ✅ | Full CRUD with AES-256-GCM encrypted credentials |
| 3.1 — DTO Layer | ✅ | Request/Response DTOs with Bean Validation |
| 3.2 — Domain Events | ✅ | Domain event bus with mock CPU/memory/disk metrics |
| 4 — SSH Monitoring | 🔜 | Live SSH connections to containerized Alpine targets |
| 5 — React Dashboard | ⏳ | Next.js 15 frontend with real-time metrics visualization |
| 6 — CI/CD | ⏳ | GitHub Actions pipeline, Docker multi-stage builds |

---

## Project Structure

```
com.infratrack/
├── domain/
│   ├── model/             Asset, AssetId, IpAddress, Credentials, enums
│   └── event/             AssetCreatedEvent, AssetStatusChangedEvent, AssetDeletedEvent
│
├── application/
│   ├── port/input/        ManageAssetUseCase
│   ├── port/output/       AssetRepository, DomainEventPublisher
│   └── service/           AssetService
│
└── infrastructure/
    ├── adapter/input/     AssetRestController
    ├── adapter/input/dto/ CreateAssetRequest, AssetResponse, AssetDtoMapper
    ├── adapter/output/    JpaAssetRepository, InMemoryAssetRepository,
    │                      SpringEventPublisher, MockMetricsListener
    ├── config/            BeanConfiguration (explicit wiring, no @Service)
    ├── persistence/       AssetJpaEntity, AssetMapper, schema.sql
    └── security/          EncryptedStringConverter
```

---

## License

This project is for demonstration and educational purposes.
