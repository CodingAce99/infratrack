# Proposal: Angular Dashboard Migration — PR4 Docker/CI Cutover & Next.js Removal

## Intent
PR1–PR3 shipped the Angular dashboard (`frontend-angular/`) — scaffold, services, presentational components, and the smart dashboard wired to the REST API. The dashboard works locally via `ng serve` + `proxy.conf.json`, but the production build path still serves the legacy Next.js app: the root `Dockerfile` is Java-only, `docker-compose.yml` `frontend` service builds `./frontend` (Next.js), and CI runs only the Maven pipeline. PR4 cuts over Docker and CI to Angular and removes the now-dead Next.js codebase so the repo has a single frontend and one consistent build/test path.

## Scope

### In Scope
- Root `Dockerfile`: multi-stage build that produces the Angular static assets (`dist/frontend-angular/browser`) and serves them via nginx, alongside the existing Java app build
- `nginx.conf`: serve Angular static files at `/` and reverse-proxy `/api/` to the app container (same-origin, no CORS), with SPA fallback to `index.html`
- `docker-compose.yml`: repoint the `frontend` service build context to `frontend-angular/` and the new Dockerfile/nginx setup
- `.github/workflows/ci.yml`: add Node 20 setup, `npm ci` in `frontend-angular/`, run `ng test --watch=false --browsers=ChromeHeadless`, and `ng build` so the Angular pipeline is exercised on every push/PR
- Remove legacy Next.js files: `frontend/` directory (`app/`, `components/`, `hooks/`, `lib/`, `public/`, configs, `Dockerfile`, `package*.json`)

### Out of Scope
- Any Angular component/service/dashboard logic change (locked by PR1–PR3)
- Backend or REST contract changes
- Frontend auth (login page, JWT interceptor, route guards) — Sprint 7.4–7.5
- Lighthouse/perf budgets, dark/light toggle, animations — Phase 10 polish
- Re-enabling ESLint for Angular (no prior ESLint config exists; deferred)

## Capabilities

### New Capabilities
- None. PR4 implements against the existing `angular-dashboard` capability spec.

### Modified Capabilities
- `angular-dashboard`: adds the deployment/runtime capability — the dashboard MUST be buildable to static assets, served by nginx behind the same origin as the API, and verifiable via `docker-compose up -d`. No behavioral change to the running app.

## Approach
Single multi-stage Dockerfile at repo root builds the JAR (existing stage) and the Angular bundle (new stage), then assembles a runtime image with nginx serving `dist/frontend-angular/browser` and proxying `/api/` to `localhost:8080`. `docker-compose` keeps the four-service topology (postgres, ssh-target-1, app, frontend) but `frontend` now builds from `frontend-angular/`. CI gains a Node job (or step) that installs deps and runs `ng test` + `ng build` so a broken Angular build fails the pipeline before merge. Next.js removal is a pure deletion once the Angular build path is green; nothing imports `frontend/`.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `Dockerfile` | Modified | Add Angular build stage + nginx runtime |
| `nginx.conf` (new or in `frontend-angular/`) | New | Static serve + `/api/` proxy + SPA fallback |
| `docker-compose.yml` | Modified | `frontend` service context → `frontend-angular/` |
| `.github/workflows/ci.yml` | Modified | Node 20 setup, `npm ci`, `ng test`, `ng build` |
| `frontend/` | Removed | Legacy Next.js app, superseded by `frontend-angular/` |
| `frontend-angular/` | Unchanged (source) | No code changes; only consumed by new build path |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| nginx SPA fallback misroutes deep links | Med | `try_files $uri $uri/ /index.html` + verify `/api/` stays proxied, not rewritten |
| CI Angular test needs ChromeHeadless + sandbox flags in runner | Med | Use `--watch=false --browsers=ChromeHeadless --no-sandbox` if runner requires it |
| Removing `frontend/` breaks a stray reference (docs, scripts) | Low | Grep repo for `frontend/` paths before deletion; update only references, not code |
| Docker build context size / node_modules leakage | Low | Use `.dockerignore` in `frontend-angular/` to exclude `node_modules`, `dist`, `.angular` |

## Rollback Plan
PR4 lands on `feat/angular-docker-cutover`. On verification failure, revert the PR — PR1–PR3 Angular source remains intact on `main`, and `ng serve` continues to work locally. The Next.js `frontend/` removal is the only non-reversible step; if it lands before the Angular docker path is verified, restore `frontend/` from git history. No backend/database changes to revert.

## Dependencies
- PR1–PR3 merged on `main` (Angular dashboard functional)
- Existing Infratrack REST API (unchanged)
- `frontend-angular/` `npm test` already green locally (PR3 shipped passing specs)

## Success Criteria
- [ ] `docker-compose up -d` serves the Angular dashboard at `http://localhost:3000`
- [ ] `/api/` requests from the browser reach the app container (same-origin, no CORS)
- [ ] Deep-link/SPA refresh returns `index.html`, not 404
- [ ] CI pipeline runs `ng test` (ChromeHeadless) and `ng build`; failure blocks merge
- [ ] `frontend/` (Next.js) directory and its references are removed
- [ ] No behavioral regression in the running dashboard vs PR3
