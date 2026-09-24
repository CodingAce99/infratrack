# Infratrack Review Rules

Infratrack is a Java 21 + Spring Boot backend with an Angular 19 frontend.

The backend follows strict hexagonal architecture. Reviews must preserve existing architecture, security invariants, testing standards, and project conventions.

## Architecture

- Dependencies must always point inward toward the domain.
- `domain/` must remain pure Java:
  - no Spring
  - no JPA
  - no infrastructure dependencies
- `application/` contains use cases, services, and port interfaces and must remain framework-agnostic.
- Infrastructure concerns belong under `infrastructure/`.
- Do not bypass ports by coupling application/domain code directly to infrastructure implementations.
- Prefer the project's existing explicit bean wiring in `BeanConfiguration`.
- Do not introduce `@Service`, `@Repository`, or other component scanning merely for convenience when the existing adapter is intentionally wired explicitly.
- Preserve the distinction between:
  - HTTP ↔ Domain mappers in `adapter/input/dto/`
  - Domain ↔ JPA mappers in `persistence/`

## Domain Design

- Preserve self-validating value objects.
- Invalid domain state should be rejected at construction.
- Follow existing factory conventions rather than introducing public constructors where factories already exist.
- Domain events must only be published after successful persistence.
- Do not move framework-specific concepts into domain models.

## Persistence and Flyway

- Flyway is the sole owner of database schema evolution.
- `ddl-auto` must remain `validate`.
- Never modify an already-applied Flyway migration.
- Schema changes require a new `V<version>__<description>.sql`.
- Do not add `IF NOT EXISTS` to migrations to hide schema inconsistencies.
- Do not introduce Hibernate-managed schema creation.

## Security

Treat security regressions as high severity.

- Asset credentials must never appear in API responses or logs.
- User passwords must remain BCrypt one-way hashes.
- Do not encrypt user passwords with AES.
- Asset credentials use AES-256-GCM and may only be decrypted where required for SSH.
- JWT secrets and encryption keys must come from configuration/environment, never source code.
- Never commit secrets, credentials, tokens, or private keys.
- Preserve uniform authentication failures; do not reveal whether a username exists or why authentication failed.
- JWT infrastructure must remain behind application ports.
- Preserve `ROLE_` authority handling.
- Preserve authorization rule ordering.
- Do not accidentally expose additional actuator endpoints.
- Public actuator access is currently intentional only for the explicitly exposed endpoints.

## REST and Error Handling

- Global API exception handling belongs in `GlobalExceptionHandler`.
- Do not add controller-local `@ExceptionHandler` methods unless there is a demonstrated architectural reason.
- Preserve existing HTTP semantics:
  - not found → 404
  - duplicate IP → 409
  - authentication failure → 401
  - authorization failure → 403
- Response DTOs must not expose internal credentials or sensitive persistence details.

## Monitoring and Concurrency

- One asset failing during SSH collection must not prevent monitoring of other assets.
- Preserve per-asset fault isolation.
- SSH sessions are single-use; do not reuse a consumed session for multiple commands.
- Parsing SSH command output should remain testable independently of real SSH connections.
- Do not move Virtual Thread orchestration into the scheduler; the scheduler should remain a thin trigger.

## Angular Frontend

- Use Angular standalone components; do not introduce NgModules.
- Preserve `OnPush` change detection.
- Use the existing RxJS/service-based state architecture; do not introduce a global state library without a demonstrated need.
- Preserve relative `/api` URLs.
- Do not add Spring CORS configuration merely to support the frontend; production uses same-origin nginx proxying.
- Do not reintroduce the removed legacy frontend.
- Preserve the existing per-asset polling isolation.
- Do not read required signal inputs from field initializers or constructors.
- Keep the edit workflow section-based; do not replace the separate backend mutations with an artificial "Save all" transaction.
- Asset passwords are never returned by the backend; frontend code must not assume they are available.

## Testing

Changes should include or update tests when behavior changes.

Backend:
- JUnit 5
- Mockito for application services
- Spring MVC slice tests for controllers where appropriate
- Prefer pure unit tests for domain logic

Frontend:
- Karma + Jasmine
- Keep tests compatible with ChromeHeadless
- CI uses `ChromeHeadlessNoSandbox`

Do not weaken, remove, or skip tests merely to make a change pass.

Bug fixes should normally include a regression test when practical.

## CI and Build

The project must remain compatible with:

```bash
./mvnw clean verify -Dspring.profiles.active=dev
cd frontend-angular
npm ci
npm test -- --browsers=ChromeHeadlessNoSandbox
npm run build
