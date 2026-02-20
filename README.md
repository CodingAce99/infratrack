# Infratrack

IT infrastructure inventory and monitoring system built with hexagonal architecture.

Tracks physical and logical state of IT assets (servers, routers, IoT devices) and monitors
them via SSH in real time.

## Tech Stack

| Layer      | Technology              | Version |
|------------|-------------------------|---------|
| Runtime    | Java (Eclipse Temurin)  | 21 LTS  |
| Framework  | Spring Boot             | 3.5.0   |
| Database   | PostgreSQL              | 17      |
| SSH Client | SSHJ                    | 0.40.0  |
| Frontend   | React + Next.js         | 19 / 15 |
| Infra      | Docker, Docker Compose  | Latest  |

## Architecture

Hexagonal architecture (Ports & Adapters) with event-driven internal communication.
```
com.infratrack/
├── application/
│   ├── port/input/     # Use case interfaces (exposed by domain)
│   ├── port/output/    # Repository/collector interfaces (required by domain)
│   └── service/        # Use case implementations
├── domain/
│   ├── model/          # Entities and Value Objects
│   ├── event/          # Domain events
│   └── service/        # Pure domain logic
└── infrastructure/
    ├── adapter/input/  # REST controllers, schedulers
    ├── adapter/output/ # JPA repositories, SSH clients, mocks
    ├── config/         # Spring beans, async config
    └── security/       # Credential encryption
```

Key decisions:
- **Domain is framework-free.** No Spring, no JPA annotations inside `domain/`.
- **Virtual Threads** for all SSH I/O operations.
- **AES-256-GCM** encryption for credentials at rest.

## Development Profiles

| Profile | Database        | SSH         | Use case              |
|---------|-----------------|-------------|-----------------------|
| `dev`   | H2 in-memory    | Mock        | Fast local testing    |
| `demo`  | PostgreSQL      | Mock (realistic) | Recruiter demos  |
| `prod`  | PostgreSQL      | Real SSH    | Production            |

## Running Locally

**Prerequisites:** Docker Desktop, JDK 21
```bash
# Clone the repo
git clone https://github.com/CodingAce99/infratrack.git
cd infratrack

# Start PostgreSQL
docker-compose up -d postgres

# Run with dev profile (H2, no Docker needed)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Run with demo profile (PostgreSQL + realistic mock data)
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

## Development Environment

- **OS:** Windows 11 with Docker Desktop (WSL2 engine)
- **JDK:** Eclipse Temurin 21 (Windows native)
- **IDE:** IntelliJ IDEA Community Edition

> Migrated from WSL2 to Windows-native tooling for better IDE integration.
> `.gitattributes` handles line endings across platforms.

## Implementation Progress

- [x] Phase 1: Hexagonal scaffolding, Docker Compose, domain Value Objects
- [ ] Phase 2: Asset CRUD with encrypted credentials
- [ ] Phase 3: Domain events + mock metrics
- [ ] Phase 4: Real SSH monitoring with Virtual Threads
- [ ] Phase 5: React/Next.js frontend dashboard
- [ ] Phase 6: CI/CD (GitHub Actions) + final polish

## Security Notes

- SSH credentials encrypted at rest using AES-256-GCM
- Encryption key via environment variable (`INFRATRACK_ENCRYPTION_KEY`)
- Credentials never appear in application logs
