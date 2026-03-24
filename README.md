[![CI](https://github.com/CodingAce99/infratrack/actions/workflows/ci.yml/badge.svg)](https://github.com/CodingAce99/infratrack/actions/workflows/ci.yml)

# Infratrack

**IT infrastructure inventory and monitoring system** built with Java 21, Spring Boot 3.5, and PostgreSQL 17.

Infratrack bridges the gap between physical inventory and the logical state of an IT infrastructure — providing a single source of truth for assets, credentials, and real-time health metrics.

> **Why this project?** Managing IT assets typically means scattered spreadsheets, stale CMDBs, and credentials stored in plaintext. Infratrack solves this with a clean REST API backed by encrypted storage and proactive SSH monitoring — built on enterprise-grade architecture patterns.

---

## Highlights

- **Hexagonal Architecture** — Domain layer with zero framework dependencies. Swappable adapters for REST, JPA, SSH, and in-memory storage.
- **SSH Monitoring** — Proactive metric collection via SSHJ. A scheduler connects to assets over SSH, extracts CPU/memory/disk metrics, and persists historical data — all parallelized with Virtual Threads.
- **React Dashboard** — Real-time monitoring UI built with Next.js 15 and TypeScript. Auto-refreshing metrics with SWR polling, sparkline charts via Recharts, and a dark ops-themed interface. Served as a static export through nginx with reverse proxy to the backend — zero CORS configuration needed.
- **Domain Events** — Async event bus decouples asset lifecycle from downstream reactions. Events are pure Java records; adding new listeners requires zero changes to existing code.
- **Security by construction** — SSH credentials encrypted with AES-256-GCM at rest. API responses structurally cannot contain passwords (`AssetResponse` has no password field — not hidden, *absent*).
- **CI/CD** — GitHub Actions pipeline validates every push. Multi-stage Docker build produces a minimal JRE image. `docker-compose up` starts the entire ecosystem in one command.
- **Three execution profiles** — `dev` (H2, instant feedback), `demo` (PostgreSQL + real SSH to Alpine containers), `prod` (real infrastructure).
- **Virtual Threads** — Java 21 Virtual Threads for non-blocking parallel SSH collection with per-asset fault isolation.
- **91 tests** across domain, service, and REST layers — including dedicated security tests that verify credentials never leak.

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Runtime | Java 21 LTS (Virtual Threads) |
| Framework | Spring Boot 3.5 |
| Database | PostgreSQL 17 (Docker) |
| Encryption | AES-256-GCM via JPA AttributeConverter |
| SSH | SSHJ 0.40.0 |
| Frontend | Next.js 15, React 19, TypeScript, Tailwind CSS v4, Recharts, SWR |
| CI/CD | GitHub Actions |
| Containerization | Docker, Docker Compose, multi-stage builds |
| Testing | JUnit 5, Mockito |

---

## Architecture

Infratrack follows **Hexagonal Architecture** (Ports & Adapters) with two distinct data flows: asset management (HTTP-driven) and monitoring (time-driven).

The domain knows nothing about Spring, JPA, or HTTP. Value objects (`IpAddress`, `Credentials`, `AssetId`) are self-validating and immutable. Domain entities use factory methods (`Asset.create()`, `Asset.reconstitute()`) instead of public constructors.

Two dedicated mapper layers keep concerns separated: `AssetDtoMapper` translates between HTTP and the domain, while `AssetMapper` translates between the domain and JPA entities.

**Virtual Threads**

The monitoring scheduler collects metrics from all active assets in parallel using Java 21 Virtual Threads. Each SSH connection runs in its own Virtual Thread via `Thread.startVirtualThread()` — lightweight, non-blocking, and with per-asset fault isolation. A single failed connection never blocks the collection of other assets.

### Domain Events

Asset lifecycle changes publish domain events through a port interface (`DomainEventPublisher`). The domain defines the events as plain Java records; infrastructure adapters implement publishing (via Spring `ApplicationEventPublisher`) and listening. This decouples the service from any downstream reactions — adding a new listener requires zero changes to existing code.

| Event | Trigger | Data |
|-------|---------|------|
| `AssetCreatedEvent` | Asset successfully persisted | AssetId, AssetType, timestamp |
| `AssetStatusChangedEvent` | Status updated | AssetId, new AssetStatus, timestamp |
| `AssetDeletedEvent` | Asset removed | AssetId, timestamp |

### Frontend

The React dashboard (`frontend/`) is a Next.js 15 static export served by nginx. It connects to the backend API through nginx reverse proxy — both frontend and API are same-origin from the browser's perspective, eliminating CORS entirely. SWR handles data fetching with automatic 60-second polling. Each `AssetCard` component owns its own metrics SWR call, following the single-responsibility principle.

---

## Quick Start

### Prerequisites

- **Java 21** — [Eclipse Temurin](https://adoptium.net/)
- **Docker Desktop** — for PostgreSQL and SSH targets in demo/prod profiles

### Option 1: Full demo (recommended for evaluation)

```bash
git clone https://github.com/CodingAce99/infratrack.git
cd infratrack

# Start everything: PostgreSQL + SSH target + API + Dashboard
export INFRATRACK_ENCRYPTION_KEY=$(openssl rand -base64 32)
docker-compose up -d

# Create a demo asset (IP is the Docker hostname, not 127.0.0.1)
curl -X POST http://localhost:8080/api/v1/assets \
  -H "Content-Type: application/json" \
  -d '{"name":"web-server-01","type":"SERVER","ipAddress":"web-server-01","username":"sshuser","password":"sshpass"}'

# Open the dashboard
# http://localhost:3000

# Or query the API directly — wait 60s for the scheduler, then:
curl http://localhost:8080/api/v1/assets/{id}/metrics/history
```

### Option 2: Dev mode (no Docker needed)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Run tests

```bash
./mvnw test
```

### Access

| Service | URL | Description |
|---------|-----|-------------|
| Dashboard | http://localhost:3000 | Real-time monitoring UI |
| REST API | http://localhost:8080/api/v1/assets | Backend API |

---

## API Reference

### Asset endpoints

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

### Metrics endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/{id}/metrics` | Latest snapshot (404 if none) |
| `GET` | `/{id}/metrics/history` | Last N snapshots (default 20, `?limit=N`) |

### Example: Create an asset

```bash
curl -X POST http://localhost:8080/api/v1/assets \
  -H "Content-Type: application/json" \
  -d '{
    "name": "web-server-01",
    "type": "SERVER",
    "ipAddress": "web-server-01",
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
  "ipAddress": "web-server-01",
  "status": "ACTIVE",
  "username": "admin"
}
```

> **Note:** When running in Full Docker mode, use the Docker hostname (`web-server-01`).
> When running the app on the host with `./mvnw spring-boot:run`, use `127.0.0.1`.

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

**91 tests** passing across four layers:

| Layer | Strategy | Spring context |
|-------|----------|----------------|
| Domain | Pure unit tests — entities, value objects, validation | None |
| Application | Mockito-based service tests with mocked ports | None |
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
| `demo` | PostgreSQL (Docker) | Real SSH to Alpine containers | Recruiter demos, integration testing |
| `prod` | PostgreSQL (external) | Real SSHJ | Production deployment |

---

## Roadmap

| Phase | Status | Description |
|-------|--------|-------------|
| 1 — Scaffolding | ✅ Done | Hexagonal package structure, Docker Compose, profile system |
| 2 — Asset CRUD + Encryption | ✅ Done | Full CRUD with AES-256-GCM encrypted credentials |
| 3 — DTO Layer + Domain Events | ✅ Done | Request/Response DTOs, Bean Validation, event bus |
| 4 — SSH Monitoring | ✅ Done | Metrics collection, persistence, SSH connections, REST API |
| 5 — React Dashboard | ✅ Done | Next.js 15 + TypeScript dashboard with SWR polling and Recharts sparklines |
| 6 — CI/CD | ✅ Done | GitHub Actions pipeline, multi-stage Docker build |

---

## Project Structure

```
infratrack/
├── .github/workflows/     CI pipeline (ci.yml)
├── docker/alpine-ssh/     SSH target Dockerfile
├── Dockerfile             Multi-stage app build (JDK → JRE)
├── docker-compose.yml     Full ecosystem: postgres + ssh-target + app + frontend
│
├── src/main/java/com.infratrack/
│   ├── domain/
│   │   ├── model/             Asset, AssetId, IpAddress, Credentials, MetricSnapshot, enums
│   │   └── event/             AssetCreatedEvent, AssetStatusChangedEvent, AssetDeletedEvent
│   │
│   ├── application/
│   │   ├── port/input/        ManageAssetUseCase, MonitorAssetUseCase
│   │   ├── port/output/       AssetRepository, MetricsCollector, MetricSnapshotRepository
│   │   └── service/           AssetService, MonitoringService
│   │
│   └── infrastructure/
│       ├── adapter/input/     AssetRestController, MetricsRestController, MetricsScheduler
│       ├── adapter/input/dto/ Request/Response DTOs + mappers
│       ├── adapter/output/    JPA repos, SSH collector, mocks, event publisher
│       ├── config/            BeanConfiguration, SchedulingConfiguration
│       ├── persistence/       JPA entities, mappers, schema.sql
│       └── security/          EncryptedStringConverter (AES-256-GCM)
│
└── frontend/
    ├── app/                   Next.js App Router (layout, page, globals.css)
    ├── components/            Dashboard, AssetCard, MetricGauge, Sparkline, Header, StatusBadge
    ├── hooks/                 useAssets (SWR hook for asset list)
    ├── lib/                   API client, TypeScript interfaces
    ├── docker/nginx.conf      Static serving + reverse proxy to backend
    └── Dockerfile             Multi-stage: Node build → nginx serve (~25MB)
```
