# Infratrack

**IT infrastructure inventory and monitoring system** built with Java 21, Spring Boot 3.5, and Hexagonal Architecture.

Infratrack bridges the gap between physical inventory and the logical state of an IT infrastructure — providing a single source of truth for assets, credentials, and real-time metrics.

---

## Features

- **Asset Management** — Full CRUD for servers, routers, and IoT devices
- **Secure Credentials** — AES-256-GCM encryption at rest for SSH credentials
- **Active Monitoring** — SSH connections to extract real-time metrics *(in development)*
- **Demo Mode** — Realistic simulated data with no real infrastructure required *(in development)*
- **Zero-friction setup** — Fully Dockerized, runs with a single command

---

## Tech Stack

| Layer | Technology | Version |
|-------|------------|---------|
| Runtime | Java | 21 LTS |
| Framework | Spring Boot | 3.5.x |
| Database | PostgreSQL | 17 |
| Security | AES-256-GCM | — |
| Containers | Docker + Docker Compose | — |
| Testing | JUnit 5 + Mockito | — |

---

## Architecture

Hexagonal Architecture (Ports & Adapters) with strict layer separation:

```
com.infratrack/
├── domain/           # Pure Java — entities, value objects, business logic
├── application/      # Use cases and ports — zero framework dependencies
└── infrastructure/   # Spring Boot, JPA, REST — input and output adapters
```

Key architectural decisions:
- The domain has no knowledge of Spring, JPA, or HTTP
- Credentials never appear in logs or API responses
- `AssetResponse` has no `password` field — security by construction, not convention

---

## Quick Start

### Prerequisites

- Java 21 ([Eclipse Temurin](https://adoptium.net/))
- Docker Desktop

### Demo mode (PostgreSQL + mock data)

```bash
# 1. Clone the repository
git clone https://github.com/your-username/infratrack.git
cd infratrack

# 2. Set the encryption key (generate one with: openssl rand -base64 32)
export INFRATRACK_ENCRYPTION_KEY=<your-32-byte-base64-key>

# 3. Start PostgreSQL
docker-compose up -d postgres

# 4. Run the application
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

### Dev mode (H2 in-memory, no Docker needed)

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Run the test suite

```bash
./mvnw test
```

---

## API Reference

Base URL: `http://localhost:8080/api/v1/assets`

| Method | Endpoint | Description | Status |
|--------|----------|-------------|--------|
| `GET` | `/` | List all assets | 200 |
| `GET` | `/{id}` | Get asset by ID | 200 |
| `POST` | `/` | Create a new asset | 201 |
| `PUT` | `/{id}/status` | Update asset status | 200 |
| `PUT` | `/{id}/credentials` | Update SSH credentials | 200 |
| `PUT` | `/{id}/ip` | Update IP address | 200 |
| `DELETE` | `/{id}` | Delete asset | 204 |

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

### Asset types

| Value | Description |
|-------|-------------|
| `SERVER` | Physical or virtual server |
| `ROUTER` | Network router or switch |
| `IOT_DEVICE` | IoT or embedded device |

### Asset statuses

| Value | Description |
|-------|-------------|
| `ACTIVE` | Asset is online and operational |
| `INACTIVE` | Asset is offline |
| `MAINTENANCE` | Asset is under maintenance |

---

## Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `INFRATRACK_ENCRYPTION_KEY` | demo + prod | AES-256 key, 32 bytes, Base64-encoded |
| `DATABASE_URL` | prod only | PostgreSQL connection URL |
| `DATABASE_USER` | prod only | Database username |
| `DATABASE_PASSWORD` | prod only | Database password |

Generate a valid encryption key:

```bash
openssl rand -base64 32
```

---

## Project Roadmap

| Phase | Status | Description |
|-------|--------|-------------|
| 1 — Scaffolding | ✅ Complete | Hexagonal structure + Docker Compose |
| 2 — CRUD + Encryption | ✅ Complete | Asset management + AES-256-GCM |
| 3.1 — DTO Layer | ✅ Complete | Request/Response DTOs + Bean Validation |
| 3.2 — Domain Events | 🔄 In progress | Event bus + mock metrics |
| 4 — Real SSH | ⏳ Planned | Live connections to Alpine containers |
| 5 — React Frontend | ⏳ Planned | Metrics dashboard |
| 6 — CI/CD | ⏳ Planned | GitHub Actions + final polish |

---

## Testing

**57 tests passing** across domain, application, and REST layers.

```
Domain tests       → pure unit tests (no Spring context)
Service tests      → Mockito only
Controller tests   → @WebMvcTest (isolated HTTP layer)
Security tests     → assert password never leaks into any response
```
