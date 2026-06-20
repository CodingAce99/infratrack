# Design: Phase 8 Observability

## Technical Approach

Implement observability entirely in `infrastructure/`, preserving the hexagonal boundary: Actuator and Micrometer are Spring concerns, not domain/application concerns. The current repo already exposes `/actuator/**` in `SecurityConfig`, has Actuator/Prometheus dependencies and management YAML present, and contains an empty `MdcCorrelationFilter` skeleton. The remaining design completes instrumentation, filter registration, log formatting, and focused tests for the three specs: endpoints, metrics, and MDC.

## Architecture Decisions

| Decision | Choice | Alternatives considered | Rationale |
|---|---|---|---|
| Metrics placement | Inject `MeterRegistry` into `MetricsScheduler` and `SshMetricsCollector` only. | Put metrics in `MonitoringService` or use AOP. | Keeps `domain/` and `application/` free of `io.micrometer` imports and follows explicit infrastructure wiring. |
| Scheduler duration metric | Wrap `monitorUseCase.collectAllActive()` with `Timer.Sample` and stop it in `finally`. | Time inside `MonitoringService`. | Records duration even on failure while keeping application service clean. |
| SSH outcome metric | Increment `infratrack.ssh.collection` with `outcome=success|failure` inside `SshMetricsCollector.collect()`. | Count in `MonitoringService`. | The adapter owns SSH success/failure knowledge; per-asset failures remain isolated. |
| MDC registration | Create `MdcCorrelationFilter` bean plus `FilterRegistrationBean` with explicit servlet order. Do not add it to `SecurityFilterChain`. | Security-chain registration or component scanning. | Avoids Phase 7.3 double-registration trap while applying to all requests, including Actuator. |
| Log pattern | Create `src/main/resources/logback-spring.xml` with correlation id in the console pattern. | Configure logging pattern in YAML only. | Logback file is explicit, reviewable, and matches the proposal’s requested file. |

## Data Flow

```text
HTTP request ──> MdcCorrelationFilter ──> Spring MVC / Actuator / Security
      │                 │                           │
      │                 ├─ MDC[correlationId]       └─ logs include correlation id
      └─ X-Request-ID? ─┘
                        finally: MDC.remove()

@Scheduled MetricsScheduler ──Timer──> MonitorAssetUseCase.collectAllActive()
                                      └─ virtual threads ──> SshMetricsCollector
                                                            └─ Counter outcome tag
```

## File Changes

| File | Action | Description |
|---|---|---|
| `pom.xml` | Verify/modify | Ensure `spring-boot-starter-actuator` and runtime `micrometer-registry-prometheus` are present. |
| `src/main/resources/application.yml` | Verify/modify | Expose only `health,info,prometheus`; add common metric tag `application=infratrack`. |
| `src/main/resources/application-demo.yml` | Verify | Demo profile inherits management exposure; no sensitive endpoint expansion. |
| `src/main/resources/logback-spring.xml` | Create | Console pattern includes MDC correlation id. |
| `src/main/java/com/infratrack/infrastructure/config/BeanConfiguration.java` | Modify | Inject `MeterRegistry` into `sshMetricsCollector(...)`. |
| `src/main/java/com/infratrack/infrastructure/adapter/input/MetricsScheduler.java` | Modify | Add `MeterRegistry` constructor dependency and timer instrumentation. |
| `src/main/java/com/infratrack/infrastructure/adapter/output/SshMetricsCollector.java` | Modify | Add `MeterRegistry` dependency and success/failure counter increments. |
| `src/main/java/com/infratrack/infrastructure/security/MdcCorrelationFilter.java` | Modify | Populate `MDC` from `X-Request-ID` or UUID; set response header; clear in `finally`. |
| `src/main/java/com/infratrack/infrastructure/config/SecurityConfig.java` | Modify | Register MDC filter once via `FilterRegistrationBean`; keep JWT filter duplicate-registration disabled. |
| `src/test/java/...` | Create/modify | Add actuator, metrics, MDC, and architecture-boundary tests. |

## Interfaces / Contracts

```java
// MDC key/header contract
header: "X-Request-ID"
mdcKey: "correlationId"

// Metrics contract
Timer:   "infratrack.monitoring.collection.duration"
Counter: "infratrack.ssh.collection", tag("outcome", "success" | "failure")
```

Actuator contract: expose only `/actuator/health`, `/actuator/info`, and `/actuator/prometheus`; `/actuator/env` and `/actuator/configprops` remain unavailable.

## Testing Strategy

| Layer | What to Test | Approach |
|---|---|---|
| Unit | Scheduler timer records on success/failure. | `SimpleMeterRegistry` + mocked `MonitorAssetUseCase`. |
| Unit | SSH counter increments success/failure. | `SimpleMeterRegistry`; prefer a test seam around collection execution if needed. |
| Web slice | Actuator security compatibility and asset auth unchanged. | Extend `SecurityAuthorizationTest` or add focused `@WebMvcTest`/context test. |
| Filter | Header preservation/generation and MDC cleanup. | Mock servlet request/response/filter chain; assert `MDC` cleared after success/failure. |
| Architecture | No `io.micrometer` imports in `domain/` or `application/`. | Source scan test. |

## Migration / Rollout

No data migration required. Roll out with normal Maven build and demo Docker startup. Validate `/actuator/health`, `/actuator/prometheus`, custom `infratrack_*` metrics after a sweep, and log correlation ids.

## Open Questions

- [ ] None.
