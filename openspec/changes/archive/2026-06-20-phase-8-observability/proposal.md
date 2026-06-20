# Proposal: Phase 8 — Observability (Actuator + Micrometer + MDC)

## Intent

Infratrack monitors external infrastructure today (SSH-collected assets) but exposes
nothing about its own runtime health. An operator or developer has no way to answer:
"Is the app alive?", "How long do monitoring sweeps take?", "Which request produced
this log line?" without SSH-ing into the container. This phase closes that gap with
Spring Boot Actuator, Micrometer metrics, and MDC-based structured logging.

## Scope

### In Scope
- Actuator endpoints: `/actuator/health`, `/actuator/info`, `/actuator/prometheus`
- Custom Micrometer metrics in infrastructure adapters (Timer in `MetricsScheduler`,
  Counter in `SshMetricsCollector`) under `infratrack.*` prefix
- MDC correlation-id filter: assign per-request id, log it, clear in `finally`
- Logback pattern update to include the MDC correlation id
- `MeterRegistry` wired in `BeanConfiguration`; no Micrometer imports in `domain/` or `application/`
- Metric-assertion tests using `SimpleMeterRegistry`

### Out of Scope
- AOP/`@Aspect` instrumentation
- Separate management port
- Distributed tracing (OpenTelemetry, Zipkin)
- Prometheus/Grafana services (optional; timeboxed to half-day if core finishes early)
- Alerting rules or polished Grafana dashboards (post-1.0)
- `restart` policy on Docker healthcheck failure (informational only)
- Changes to domain model or application services

## Capabilities

### New Capabilities
- `observability-endpoints`: Actuator health, info, prometheus exposure + security
- `observability-metrics`: Custom Micrometer metrics in infrastructure adapters
  (`infratrack.monitoring.collection.duration`, `infratrack.ssh.collection`)
- `observability-mdc`: MDC correlation-id filter, log pattern, and cleanup contract

### Modified Capabilities

None — no existing spec-level behavior changes.

## Approach

Add `spring-boot-starter-actuator` + `micrometer-registry-prometheus` (runtime).
Configure `management.endpoints.web.exposure.include` in YAML. Wire `MeterRegistry`
into `MetricsScheduler` and `SshMetricsCollector` via constructor (mirroring existing
BeanConfiguration pattern). Complete the `MdcCorrelationFilter` body with MDC
put/remove guarded by `finally`, then register it once via `FilterRegistrationBean`
to avoid the double-registration trap from Phase 7.3.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `pom.xml` | Modified | Add actuator + micrometer-registry-prometheus |
| `application.yml`, `application-demo.yml` | Modified | Management endpoint exposure config |
| `infrastructure/config/BeanConfiguration.java` | Modified | Wire MeterRegistry bean, inject into adapters |
| `infrastructure/adapter/input/MetricsScheduler.java` | Modified | Wrap `collectAllActive()` with Micrometer Timer |
| `infrastructure/adapter/output/SshMetricsCollector.java` | Modified | Increment success/failure Counter |
| `infrastructure/security/MdcCorrelationFilter.java` | Modified | Implement `doFilterInternal` with MDC + cleanup |
| `infrastructure/config/SecurityConfig.java` | Modified | Register MDC filter (once) |
| `src/main/resources/logback-spring.xml` | Modified | Add `%mdc` to log pattern |
| `docker-compose.yml` | None | Healthcheck already present; becomes functional |
| `domain/`, `application/` | None | Zero Micrometer/Spring imports |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Actuator security rule already present but untested — could break existing auth | Low | Rule is ordered correctly (after login, before assets); verify with existing @WebMvcTest slices |
| MDC leak on Virtual Threads if cleanup not in `finally` | Medium | Cleanup in `finally` block; test with two sequential requests verifying no stale id |
| Filter double-registration (same trap as Phase 7.3) | Medium | Register via `FilterRegistrationBean` with explicit order; do NOT add to security chain |
| Actuator dependency may pull in unwanted auto-configuration | Low | Explicit `management.endpoints.web.exposure.include`; do not expose `env`/`configprops` |

## Rollback Plan

Remove `spring-boot-starter-actuator` and `micrometer-registry-prometheus` from
`pom.xml`. Delete `management.*` config from YAML files. Revert `MetricsScheduler`
and `SshMetricsCollector` to pre-instrumentation state (remove MeterRegistry fields).
Remove MDC filter registration (class stays; empty `doFilterInternal` is harmless).
Existing 146 tests remain unchanged and continue passing under dev profile.

## Dependencies

- Phase 7 (auth + login) fully closed; 146 tests green on `main`
- `MdcCorrelationFilter` skeleton already exists; `SecurityConfig` already permits `/actuator/**`

## Success Criteria

- [ ] `./mvnw test` green with new metric-assertion tests (count > 146)
- [ ] `/actuator/health` returns 200 UP in demo profile
- [ ] `/actuator/prometheus` emits `infratrack_*` metrics after a collection cycle
- [ ] Log lines contain correlation id; second request does not inherit first request's id
- [ ] Grep: zero `io.micrometer` imports in `domain/` or `application/`
- [ ] `docker-compose up`: app healthcheck passes; existing asset CRUD + dashboard unchanged
