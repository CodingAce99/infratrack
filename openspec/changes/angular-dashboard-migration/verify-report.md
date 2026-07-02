# Verification Report

**Change**: angular-dashboard-migration
**Slice**: PR4 Docker/CI cutover runtime
**Mode**: Strict TDD
**Artifact store**: OpenSpec + Engram
**Verify date**: 2026-07-02
**Verdict**: PASS WITH WARNINGS

PR4 runtime cutover is verified. `docker compose up -d --build` built the app, Angular/nginx frontend, and SSH target images; started the four-service topology; and served the Angular dashboard at `http://localhost:3000`. nginx successfully proxied both public actuator traffic and secured API traffic to the Spring Boot app. The Angular CI launcher test command also passed with 96/96 specs.

## Scope Boundary

| Dimension | Verification stance |
|---|---|
| Current slice | PR4 only: Docker/CI cutover runtime, nginx static serving/proxy, Next.js removal verification. |
| Completed tasks judged | Phase 4 tasks 4.1-4.6 from `tasks.md`. |
| Backend changes | None required for PR4; backend was exercised through the existing Docker image/runtime path. |
| Frontend auth | Still out of scope; `GET /api/v1/assets` returning 401 is expected because backend auth is enforced. |

## Completeness

| Metric | Value |
|---|---:|
| Phase 4 tasks in scope | 6 |
| Phase 4 tasks complete | 6 |
| Phase 4 tasks incomplete | 0 |
| Full change task count | 35/35 complete |
| Archive readiness | Yes, from a PR4 runtime verification perspective. |

## Build, Runtime, and Test Evidence

| Command / Probe | Result | Evidence |
|---|---|---|
| `docker compose config --quiet` | ✅ Passed | Exit 0; no output. |
| `docker compose up -d --build` | ✅ Passed | Built `infratrack-app`, `infratrack-frontend`, `infratrack-ssh-target-1`; started `postgres`, `ssh-target-1`, `app`, `frontend`. |
| `docker compose ps` | ✅ Passed | `infratrack-app` up and healthy on `8080`; `infratrack-postgres` healthy; `infratrack-frontend` up on `3000`; `ssh-target-1` up on `2222`. |
| `GET http://localhost:3000/` | ✅ Passed | HTTP 200 `text/html`; body contains `<app-root` and Angular `main-*.js` bundle script. |
| `GET http://localhost:3000/actuator/health` | ✅ Passed | HTTP 200 via nginx; body `{"status":"UP",...}`; `Server: nginx/1.31.2` plus Spring actuator headers. |
| `GET http://localhost:3000/api/v1/assets` | ✅ Passed | HTTP 401 via nginx; body `{"error":"Unauthorized"}`. This matches secured backend behavior and proves proxy reachability. |
| `GET http://localhost:8080/actuator/health` | ✅ Passed | HTTP 200 direct backend; body `{"status":"UP",...}`. |
| `GET http://localhost:8080/api/v1/assets` | ✅ Passed | HTTP 401 direct backend; body `{"error":"Unauthorized"}`; matches the proxied response. |
| `GET http://localhost:3000/dashboard/deep-link-check` | ✅ Passed | HTTP 200 `text/html`; body contains `<app-root` and Angular bundle script, confirming SPA fallback. |
| `npm test -- --browsers=ChromeHeadlessNoSandbox` from `frontend-angular/` | ✅ Passed | `Chrome Headless 149.0.0.0 (Windows 10): Executed 96 of 96 SUCCESS`; `TOTAL: 96 SUCCESS`. |

## Spec Compliance Matrix

| Requirement / Success criterion | Runtime evidence | Result |
|---|---|---|
| Angular dashboard is served by Docker Compose | `http://localhost:3000/` returns Angular `index.html` with `<app-root` and bundle script. | ✅ COMPLIANT |
| `/api/` requests reach the app container through same-origin nginx | `http://localhost:3000/api/v1/assets` returns Spring Security 401 JSON matching direct backend response. | ✅ COMPLIANT |
| Actuator proxy reaches backend | `http://localhost:3000/actuator/health` returns Spring Boot actuator health JSON with `status: UP`. | ✅ COMPLIANT |
| Direct backend remains healthy | `http://localhost:8080/actuator/health` returns `status: UP`. | ✅ COMPLIANT |
| SPA fallback works for deep links | `/dashboard/deep-link-check` returns Angular HTML instead of 404. | ✅ COMPLIANT |
| Angular CI launcher remains green | NoSandbox Karma command passes 96/96 specs. | ✅ COMPLIANT |

## TDD Compliance

| Check | Result | Details |
|---|---|---|
| TDD evidence reported | ✅ | Engram apply-progress topic `sdd/angular-dashboard-migration/apply-progress` contains a PR4 TDD Cycle Evidence table. |
| All PR4 tasks have verification evidence | ✅ | 4.1-4.4 and 4.6 had prior green build/config/test evidence; 4.5 now has live Docker Compose runtime evidence. |
| RED/GREEN evidence cross-check | ✅ | Green evidence was re-run for 4.5 (`docker compose up -d --build`) and 4.6 (`npm test -- --browsers=ChromeHeadlessNoSandbox`). |
| Triangulation adequate | ✅ | PR4 is config/runtime work; verification covers static compose config, image build, service health, static Angular serve, nginx proxy, direct backend comparison, SPA fallback, and frontend tests. |
| Assertion quality | ✅ | PR4 did not create or modify production test assertions; existing 96 Angular specs passed at runtime. |

## Design Coherence

| Design / proposal point | Followed? | Notes |
|---|---|---|
| Four-service Docker Compose topology | ✅ Yes | Runtime has `postgres`, `ssh-target-1`, `app`, and `frontend`. |
| Angular static assets served by nginx | ✅ Yes | Frontend image serves Angular HTML from nginx on host port 3000. |
| Same-origin `/api/` proxy, no CORS dependency | ✅ Yes | `/api/v1/assets` through port 3000 reaches backend and returns the same secured response as port 8080. |
| `/actuator/` proxy for health checks/observability | ✅ Yes | Proxied actuator health returns 200 UP. |
| Next.js no longer used as runtime frontend | ✅ Yes | Runtime frontend image is `infratrack-frontend` built from the Angular/nginx path. |

## Issues Found

### CRITICAL

None.

### WARNING

1. `npm ci` inside the frontend Docker build reported `29 vulnerabilities (2 low, 10 moderate, 17 high)`. This did not fail the build and is outside the PR4 runtime objective, but dependency audit follow-up is recommended.
2. The app log contains one scheduled metrics collection failure for an existing asset while the app remains healthy. This appears unrelated to the Angular/nginx cutover and did not affect dashboard serving or proxy verification.
3. The frontend test command emits an Angular CLI warning because `browsers` is set both in the npm script and the CLI override; the override wins and tests pass.

### SUGGESTION

1. Before archiving, optionally run the full Maven verify path once outside Docker if you want a final backend regression signal, even though PR4 did not change Java code.
2. Consider a follow-up dependency audit issue for the frontend npm advisory count instead of mixing it into this Docker/CI cutover.

## Final Verdict

**PASS WITH WARNINGS**

Task 4.5 is complete. PR4 satisfies the runtime cutover objective: Docker Compose builds and starts the Angular/nginx frontend, serves the dashboard at port 3000, proxies backend health/API traffic correctly, preserves direct backend health, and keeps the Angular CI launcher test suite green.
