# Infratrack

IT infrastructure inventory and monitoring system.

## What is this?

My first real enterprise-scale project. Building a system to track IT assets (servers, routers, IoT devices) and monitor them via SSH.

**Current status:** Just set up the project structure. Working on basic CRUD next.

## Tech Stack

- Java 21 (trying out Virtual Threads)
- Spring Boot 3.5.0
- PostgreSQL 17
- Docker Compose

## Project Structure

Using Hexagonal Architecture (still learning this pattern):

```
com.infratrack/
├── application/    # Use cases and business logic
├── domain/         # Core domain models
└── infrastructure/ # Database, REST API, SSH clients
```

## Running locally

```bash
# Start database
docker-compose up -d postgres

# Run app (dev profile uses H2 for now)
./mvnw spring-boot:run
```

## Development Plan

- [x] Phase 1: Project setup and structure
- [ ] Phase 2: Asset CRUD with encrypted credentials
- [ ] Phase 3: Mock metrics and events
- [ ] Phase 4: Real SSH monitoring
- [ ] Phase 5: React frontend
- [ ] Phase 6: Polish and deployment

## Notes

- Learning hexagonal architecture as I build
- Using Docker to simulate servers for testing
- Credentials will be encrypted in the database (AES-256)

---

**This is a learning project. Feedback welcome!**
