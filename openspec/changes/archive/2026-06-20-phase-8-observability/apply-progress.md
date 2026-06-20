# Apply Progress: Phase 8 — Observability

## Status: COMPLETE (Corrective Rerun #3 — 2026-06-20)

All 14 original + 6 corrective (Phase C) + 3 corrective (Phase D) tasks completed. 173 tests passing (baseline: 146, +17 Phase 8 original, +9 corrective C, +1 corrective D).

## TDD Cycle Evidence

### Original Tasks (batch 1 — 14 tasks)

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1 | N/A | N/A | N/A | ➖ Confirm-only | ✅ Present | ➖ N/A | ➖ N/A |
| 1.2 | N/A | N/A | N/A | ➖ Confirm-only | ✅ Present | ➖ N/A | ➖ N/A |
| 1.3 | N/A | N/A | N/A | ➖ Structural | ✅ Created | ➖ Single | ➖ N/A |
| 2.1 | MetricsSchedulerTest.java | Unit | N/A (new) | ✅ Written | ✅ Passed | ✅ 2 cases | ✅ Clean |
| 2.2 | MetricsScheduler.java | Unit | N/A (impl) | N/A | ✅ Passed | N/A | ➖ None needed |
| 2.3 | MdcCorrelationFilterTest.java | Unit | N/A (new) | ✅ Written | ✅ Passed | ✅ 3 paths | ✅ Clean |
| 2.4 | MdcCorrelationFilter.java | Unit | N/A (impl) | N/A | ✅ Passed | N/A | ✅ MDC.remove() |
| 2.5 | SshMetricsCollectorMetricsTest.java | Unit | N/A (new) | ✅ Written | ✅ Passed | ✅ 3 cases | ✅ doCollect seam |
| 2.6 | SshMetricsCollector.java | Unit | N/A (impl) | N/A | ✅ Passed | N/A | ✅ Extracted seam |
| 3.1 | BeanConfiguration.java | Wiring | N/A | ➖ Structural | ✅ Built | ➖ Single | ➖ N/A |
| 3.2 | SecurityConfig.java | Wiring | N/A | ➖ Structural | ✅ Built | ➖ Single | ➖ N/A |
| 4.1 | ActuatorEndpointTest.java | Integration | N/A (new) | ✅ Written | ✅ Passed | ✅ 4 cases | ✅ Clean |
| 4.2 | ArchitectureBoundaryTest.java | Unit | N/A (new) | ✅ Written | ✅ Passed | ✅ 2 packages | ➖ N/A |
| 4.3 | Full suite | All | ✅ 146/146 | N/A | ✅ 163/163 | N/A | N/A |

### Corrective Tasks (batch 2 — 6 tasks)

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| C.1 | ActuatorEndpointTest.java | Integration | ✅ 163/163 | ✅ Written | ✅ Passed (8/8) | ✅ 3 cases (env 404, configprops 404, info 200) | ➖ N/A |
| C.2 | ActuatorSecurityDemoTest.java | Integration | ✅ 163/163 | ✅ Written | ✅ Passed (3/3) | ✅ 3 cases (health public, prometheus public, assets 401) | ➖ N/A |
| C.3 | MdcFilterRegistrationTest.java | Integration | ✅ 163/163 | ✅ Written | ✅ Passed (2/2) | ✅ 2 checks (count+chain absence) | ✅ Imports cleaned |
| C.4 | ActuatorEndpointTest.java | Integration | ✅ 163/163 | ✅ Written | ✅ Passed (8/8) | ✅ 1 case (sweep → prometheus contains metric) | ➖ N/A |
| C.5 | tasks.md / apply-progress.md | Doc | N/A | N/A | ✅ Documented | ➖ Single | ➖ N/A |
| C.6 | SshMetricsCollectorMetricsTest.java | Unit | ✅ 163/163 | ✅ Written | ✅ Passed (3/3) | ✅ 2 failure paths (counter inc + no crossover) | ✅ Deterministic seam |

### Corrective Tasks (batch 3 — Phase D: unchecked SSH failure counter)

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| D.1 | SshMetricsCollectorMetricsTest.java | Unit | ✅ 10/10 | ✅ Written (null counter) | ✅ Passed (11/11) | ✅ 2 failure types (checked IOException + unchecked IllegalArgumentException) | ➖ None needed |
| D.2 | SshMetricsCollector.java | Unit | N/A (impl) | N/A | ✅ Passed | N/A | ➖ Minimal — single catch block added |
| D.3 | Full suite | All | ✅ 173/173 | N/A | ✅ Passed | N/A | N/A |

### Test Summary
- **Total tests written**: 27 (17 original + 9 corrective C + 1 corrective D)
- **Total tests passing**: 173 (146 baseline + 17 Phase 8 + 9 corrective C + 1 corrective D)
- **Layers used**: Unit (15), Integration (10), Wiring (0 — structural), Doc (2)
- **Approval tests**: None — no refactoring tasks
- **Pure functions created**: 0 (infrastructure wiring)

## Files Changed

| File | Action | What Was Done |
|------|--------|---------------|
| `pom.xml` | Modified | `micrometer-registry-prometheus` scope changed from runtime to default (compile) |
| `src/main/resources/application.yml` | Pre-existing | Management endpoints expose health,info,prometheus |
| `src/main/resources/logback-spring.xml` | Created | Console pattern with `%mdc{correlationId}` |
| `src/main/java/.../adapter/input/MetricsScheduler.java` | Modified | Added MeterRegistry, Timer instrumentation via `Timer.Sample` |
| `src/main/java/.../adapter/output/SshMetricsCollector.java` | Modified | Added MeterRegistry; extracted `doCollect()` protected test seam; success/failure Counter in `collect()` |
| `src/main/java/.../security/MdcCorrelationFilter.java` | Modified | Implemented MDC + UUID + correlation header; `MDC.remove("correlationId")` in finally |
| `src/main/java/.../config/BeanConfiguration.java` | Modified | Inject MeterRegistry into `sshMetricsCollector(...)` factory method |
| `src/main/java/.../config/SecurityConfig.java` | Modified | Register MdcCorrelationFilter via `FilterRegistrationBean` with order -100 (applies to all profiles) |
| `.claude/settings.local.json` | Modified | Added `Bash(./mvnw test *)` execution permission |
| `.gitignore` | Modified | Added `.atl/` (Gentle AI local cache) |
| `docker-compose.yml` | Modified | Added Docker healthcheck using `/actuator/health` |
| `src/test/java/.../adapter/input/MetricsSchedulerTest.java` | Created | 2 tests: timer records on success + failure |
| `src/test/java/.../security/MdcCorrelationFilterTest.java` | Created | 6 tests: UUID generation, header preservation, MDC cleanup on success + exception, no cross-request leakage |
| `src/test/java/.../adapter/output/SshMetricsCollectorMetricsTest.java` | Created/Modified | 3 tests: success counter via doCollect() seam, failure counter via deterministic failure seam, no cross-contamination |
| `src/test/java/.../config/ActuatorEndpointTest.java` | Created/Modified | 8 tests: health 200 UP public, prometheus 200 + text/plain + non-HTML public, env 404, configprops 404, info 200, sweep metric in prometheus |
| `src/test/java/.../architecture/ArchitectureBoundaryTest.java` | Created | 2 tests: zero `io.micrometer` imports in `domain/` and `application/` |
| `src/test/java/.../config/ActuatorSecurityDemoTest.java` | Created | 3 tests: actuator health+prometheus public under demo secured chain, assets require auth |
| `src/test/java/.../security/MdcFilterRegistrationTest.java` | Created | 2 tests: single FilterRegistrationBean, filter not in SecurityFilterChain |

## Deviations from Design

- `micrometer-registry-prometheus` scope: changed from `runtime` to default (compile) to ensure `@SpringBootTest` auto-configuration has full visibility of the Prometheus classes.
- Prometheus endpoint auto-configuration: requires `management.prometheus.metrics.export.enabled=true` in the test property source for reliable `@SpringBootTest` context startup. At runtime (demo/prod) the default `true` applies.
- `MDC.clear()` → `MDC.remove("correlationId")`: targeted cleanup is safer and still satisfies the spec's cleanup requirement. A full `MDC.clear()` could interfere with other libraries' MDC keys.
- `SshMetricsCollector` now has a `protected doCollect(Asset)` test seam that encapsulates the full SSH lifecycle (connect, authenticate, execute commands, disconnect). The public `collect()` method wraps it with counter instrumentation only. This enables a clean automated success-path test without a real SSH server.
- `docker-compose.yml`: healthcheck was added (Phase 8 makes it functional). Pre-existing feature that becomes active now.

## Issues Found & Resolved

- **Original Gatekeeper CRITICAL #1 (Prometheus test)**: Original `ActuatorEndpointTest` only asserted `status != 401 && status != 403` while `/actuator/prometheus` returned 404. Fixed by restructuring test to flat class (removed `@Nested` context isolation issues) and adding `management.prometheus.metrics.export.enabled=true` property. New assertions: HTTP 200, `text/plain` content type, non-HTML body (valid Prometheus format).
- **Original Gatekeeper CRITICAL #2 (SSH success test)**: Added `protected doCollect()` test seam to `SshMetricsCollector` and a test subclass that overrides it to return a synthetic snapshot. New test `incrementsSuccessCounterOnSshSuccess` verifies the `infratrack.ssh.collection{outcome=success}` counter path.
- **Gatekeeper SUGGESTION #5 (MDC.clear → MDC.remove)**: Applied. `MdcCorrelationFilter` now uses `MDC.remove("correlationId")` instead of `MDC.clear()`. All 6 MDC tests pass.
- **Working tree triage**: `.claude/settings.local.json` (+test permission), `.gitignore` (+`.atl/`), and `docker-compose.yml` (+healthcheck) are Phase-8 related. `PHASE-8-BRIEFING.md` was deleted by orchestrator per user decision.
- **Verify CRITICAL #1 (unsafe endpoints untested)**: Added runtime tests proving `/actuator/env` → 404 and `/actuator/configprops` → 404. Also added `/actuator/info` → 200 for completeness.
- **Verify CRITICAL #2 (secured-chain actuator public access)**: Created `ActuatorSecurityDemoTest` — `@SpringBootTest` under demo profile with `@AutoConfigureTestDatabase`. Proves `/actuator/health` and `/actuator/prometheus` return 200 without auth while `/api/v1/assets` returns 401 under the same secured chain.
- **Verify CRITICAL #3 (MDC single-registration untested)**: Created `MdcFilterRegistrationTest` — Spring context proves exactly one `FilterRegistrationBean<MdcCorrelationFilter>` exists and the filter is NOT added to any `SecurityFilterChain`.
- **Verify CRITICAL #4 (custom sweep metric not in Prometheus)**: Added `prometheusExposesCustomSweepMetricAfterCollectAll` — triggers `MetricsScheduler.collectAll()`, then asserts `infratrack_monitoring_collection_duration` appears in `/actuator/prometheus` text output.
- **Verify WARNING #1 (PowerShell quoting)**: Documented in `tasks.md` and this `apply-progress.md`: `./mvnw test "-Dspring.profiles.active=dev"`.
- **Verify WARNING #2 (SSH localhost:22 flaky)**: Replaced environment-sensitive localhost:22 assumption with deterministic `doCollect()` failure seam — throws `IOException` directly, no real network call.
- `@SpringBootTest` requires `infratrack.encryption.key` system property for dev profile (H2 startup). Resolved via `@TestPropertySource`.
- MDC assertions must capture state from within the filter chain (AtomicReference in mock `doAnswer`), not after `doFilterInternal` returns — the finally block clears MDC before assertions.
- **Verify CRITICAL #5 (SSH failure counter misses unchecked failures)**: `SshMetricsCollector.collect()` only caught `IOException`, but parsing methods (`parseCpuUsage`, `parseMemoryUsage`, `parseDiskUsage`) throw unchecked `IllegalArgumentException` / `NumberFormatException`. These propagated without incrementing `infratrack.ssh.collection{outcome="failure"}`. Fixed by adding `catch (RuntimeException e)` block that increments the failure counter and rethrows; verified with a new deterministic test seam that throws `IllegalArgumentException`. Targeted tests: 11/11 pass; full suite: 173/173 pass.

## PowerShell Maven Quoting (Mandatory)

On Windows PowerShell, Maven `-D` properties MUST be quoted:

```powershell
# ✅ CORRECT
./mvnw test "-Dspring.profiles.active=dev"
./mvnw test "-Dspring.profiles.active=dev" "-Dtest=ActuatorEndpointTest"

# ❌ WRONG — Maven parses .profiles.active=dev as a lifecycle phase
./mvnw test -Dspring.profiles.active=dev
```

## Gatekeeper Resolution Status

### Original Gatekeeper (corrective rerun #1)

| Finding | Severity | Status |
|---------|----------|--------|
| #1 Prometheus test weak (only not-401/403, actually 404) | CRITICAL | ✅ RESOLVED — 4 tests, 200 + text/plain + non-HTML |
| #2 No SSH success-path test | CRITICAL | ✅ RESOLVED — doCollect() test seam + success counter test |
| #3 Refresh apply-progress | WARNING | ✅ RESOLVED — this document |
| #4 Triage untracked files | WARNING | ✅ RESOLVED — documented above |
| #5 MDC.clear() → MDC.remove() | SUGGESTION | ✅ RESOLVED — applied |

### Verify Report CRITICALs (corrective rerun #2)

| Finding | Severity | Status |
|---------|----------|--------|
| #1 Unsafe actuator endpoints untested (env, configprops) | CRITICAL | ✅ RESOLVED — env 404 + configprops 404 tests |
| #2 Secured-chain actuator permitAll untested | CRITICAL | ✅ RESOLVED — ActuatorSecurityDemoTest under demo profile |
| #3 MDC single-registration untested | CRITICAL | ✅ RESOLVED — MdcFilterRegistrationTest context test |
| #4 Custom sweep metric not in Prometheus | CRITICAL | ✅ RESOLVED — sweep + prometheus end-to-end test |
| W#1 PowerShell quoting undocumented | WARNING | ✅ RESOLVED — documented in apply-progress + tasks.md |
| W#2 SSH localhost:22 flaky assumption | WARNING | ✅ RESOLVED — deterministic doCollect() failure seam |

### Verify Report CRITICALs (corrective rerun #3)

| Finding | Severity | Status |
|---------|----------|--------|
| #5 SSH failure counter misses unchecked collection failures (RuntimeException bypasses counter) | CRITICAL | ✅ RESOLVED — `catch (RuntimeException e)` block added; new test `incrementsFailureCounterOnUncheckedFailure` |
