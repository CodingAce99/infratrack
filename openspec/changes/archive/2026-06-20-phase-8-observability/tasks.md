# Tasks: Phase 8 — Observability

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~320 (70 prod, 250 test) |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Medium

## Phase 1: Foundation

- [x] 1.1 Verify `pom.xml` has `spring-boot-starter-actuator` + `micrometer-registry-prometheus` (present — confirm only)
- [x] 1.2 Verify `application.yml` exposes `health,info,prometheus` with metric tag `application=${spring.application.name}` (present — confirm only)
- [x] 1.3 Create `src/main/resources/logback-spring.xml` — console pattern includes `%mdc{correlationId}`

## Phase 2: Core Implementation (RED → GREEN per component)

- [x] 2.1 RED: Write `MetricsSchedulerTest` — `SimpleMeterRegistry` asserts timer records on success and failure
- [x] 2.2 GREEN: `MetricsScheduler.java` — add `MeterRegistry`; wrap `collectAllActive()` with `Timer.Sample` + `stop()` in `finally`
- [x] 2.3 RED: Write `MdcCorrelationFilterTest` — mock request/response; assert MDC put, response header, cleanup on success + exception
- [x] 2.4 GREEN: Implement `MdcCorrelationFilter.doFilterInternal()` — read `X-Request-ID` or UUID; MDC put, header; `MDC.clear()` in `finally`
- [x] 2.5 RED: Write `SshMetricsCollectorMetricsTest` — `SimpleMeterRegistry` asserts counter for success and failure paths
- [x] 2.6 GREEN: `SshMetricsCollector.collect()` — increment `infratrack.ssh.collection` with tag `outcome=success|failure`

## Phase 3: Wiring

- [x] 3.1 `BeanConfiguration.java` — inject `MeterRegistry` into `sshMetricsCollector(...)` method
- [x] 3.2 `SecurityConfig.java` — add `FilterRegistrationBean<MdcCorrelationFilter>` with explicit order; NOT in security chain

## Phase 4: Testing & Verification

- [x] 4.1 Actuator endpoint test — `/actuator/health` 200 without token; asset auth unchanged
- [x] 4.2 `ArchitectureBoundaryTest` — source-scan `domain/` + `application/` for zero `io.micrometer` imports
- [x] 4.3 `./mvnw test` — green, new metric/MDC/actuator tests pass, count > 146

## Phase C: Corrective Verification Coverage (post-verify)

- [x] C.1 RED→GREEN: `ActuatorEndpointTest` — add tests proving `/actuator/env` (404), `/actuator/configprops` (404), and `/actuator/info` (200)
- [x] C.2 RED→GREEN: `ActuatorSecurityDemoTest` — `@SpringBootTest` under demo profile proving `/actuator/**` permitAll while `/api/v1/assets` requires auth
- [x] C.3 RED→GREEN: `MdcFilterRegistrationTest` — Spring context test proving exactly one `FilterRegistrationBean<MdcCorrelationFilter>` exists and the filter is NOT in any `SecurityFilterChain`
- [x] C.4 RED→GREEN: `ActuatorEndpointTest` — trigger `MetricsScheduler.collectAll()` and assert `infratrack_monitoring_collection_duration` appears in `/actuator/prometheus` output
- [x] C.5 Document PowerShell Maven quoting in apply-progress
- [x] C.6 REFACTOR: `SshMetricsCollectorMetricsTest` — replace localhost:22 assumption with deterministic `doCollect()` failure seam

## Phase D: Unchecked SSH Failure Counter (post-verify corrective)

- [x] D.1 RED: `SshMetricsCollectorMetricsTest` — add `uncheckedFailureCollector()` seam (throws `IllegalArgumentException`) and test `incrementsFailureCounterOnUncheckedFailure` proving the counter is NULL for unchecked collection failures
- [x] D.2 GREEN: `SshMetricsCollector.collect()` — add `catch (RuntimeException e)` block that increments `infratrack.ssh.collection{outcome="failure"}` and rethrows
- [x] D.3 Run targeted SSH metrics tests (11/11) and full suite to confirm safety net

**Note**: PowerShell requires quoted Maven `-D` properties: `./mvnw test "-Dspring.profiles.active=dev"`
