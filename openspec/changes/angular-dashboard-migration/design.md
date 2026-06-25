# Design: Angular Dashboard Migration

## Technical Approach

Replace the current `frontend/` Next.js static export with an Angular SPA using standalone components, RxJS services, and Reactive Forms. The backend API remains unchanged: Angular calls the existing `/api/v1/assets` and `/api/v1/assets/{id}/metrics/history?limit=20` endpoints through relative URLs. The design preserves the existing functional decomposition while making state ownership explicit in services and keeping presentational components API-free.

## Architecture Decisions

| Decision | Choice | Alternatives considered | Rationale |
|---|---|---|---|
| Angular structure | Standalone Angular app under `frontend/src/app` with feature folders for dashboard, assets, metrics, and shared UI | NgModules-first layout | Standalone is current Angular direction and keeps the portfolio signal modern without adding module boilerplate. |
| Asset state | `AssetService` exposes `assets$`, `loading$`, `error$`, mutation methods, and `refresh()` backed by `BehaviorSubject`/`Subject`, `switchMap`, `shareReplay(1)` | Component-local fetching | Replaces SWR with one shared replayed source and explicit mutation revalidation. |
| Metrics state | `MetricService.history$(assetId)` returns independent 60s polling streams per card | Single dashboard-wide metrics request | Existing UI isolates metrics per asset; per-card streams preserve fault isolation and avoid blocking unrelated cards. |
| Sparkline | Custom SVG path component | `ngx-charts` or another chart library | Recharts is React-only; SVG keeps the dependency graph small for a simple 32px sparkline. |
| Tests | Official Angular testing foundation with at least one service test and one presentational component test | Defer frontend tests or introduce third-party testing utilities in the first slice | The spec requires non-zero tests and CI confidence during the framework swap, and the first migration slice should minimize tooling risk by staying on Angular's standard testing stack. |

## Data Flow

```text
DashboardComponent ── subscribes ──> AssetService.assets$
       │                                │
       │ create/edit/delete             ├── HttpClient ──> /api/v1/assets
       │                                └── refresh$ + shareReplay(1)
       └── AssetCardComponent ──> MetricService.history$(id)
                                      └── timer(0, 60000) ──> /metrics/history?limit=20
```

Forms emit commands to services. Successful mutations call `AssetService.refresh()`. Failed mutations expose the API error and do not clear the last valid asset list.

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `frontend/src/app/app.component.*` | Create | Angular shell for `/`. |
| `frontend/src/app/dashboard/*` | Create | State-owning dashboard composition. |
| `frontend/src/app/assets/*` | Create | Asset card, create modal, edit panel, status badge. |
| `frontend/src/app/metrics/*` | Create | Metric gauge and custom SVG sparkline. |
| `frontend/src/app/core/api/*.ts` | Create | Models, `ApiError`, `AssetService`, `MetricService`. |
| `frontend/src/styles.css` | Create | Dark theme CSS variables and shared layout styles. |
| `frontend/proxy.conf.json` | Create | Dev proxy from `/api` to Spring Boot. |
| `frontend/Dockerfile` | Modify | Build Angular and copy `dist/frontend/browser` to nginx. |
| `frontend/docker/nginx.conf` | Modify | Keep `/api/` proxy and SPA `try_files` fallback. |
| `.github/workflows/ci.yml` | Modify | Add Node setup plus Angular install/build/test steps. |
| `frontend/app`, `frontend/components`, `frontend/hooks`, `frontend/lib` | Delete | Remove Next.js/React implementation. |

## Interfaces / Contracts

```ts
export interface Asset { id: string; name: string; type: 'SERVER' | 'ROUTER' | 'IOT_DEVICE'; ipAddress: string; status: 'ACTIVE' | 'INACTIVE' | 'MAINTENANCE'; username: string; }
export interface MetricSnapshot { assetId: string; cpuUsage: number; memoryUsage: number; diskUsage: number; collectedAt: string; }
```

`AssetService` contract: `assets$`, `loading$`, `error$`, `refresh()`, `createAsset()`, `updateStatus()`, `updateIp()`, `updateCredentials()`, `deleteAsset()`. Status, IP, and credentials remain separate saves.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | `AssetService` refresh/error behavior; threshold color helper; SVG path generation | Angular `TestBed` plus official `HttpClient` testing utilities (`provideHttpClientTesting`, `HttpTestingController`) |
| Component | `StatusBadge`, `MetricGauge`, empty sparkline state, form error rendering | Angular component tests with inputs and mocked services using the official Angular testing stack |
| Integration | Docker/nginx API proxy and Angular production build | CI build plus manual `docker-compose up -d` verification |
| E2E | Browser auth flow | Out of scope for this slice |

This migration slice intentionally stays on Angular's official testing foundation. Third-party testing utilities are not excluded forever, but they are deferred until the Angular frontend is stable and there is a demonstrated need that the default Angular stack cannot satisfy cleanly.

## Migration / Rollout

No data migration required. Roll out on a feature branch by replacing `frontend/` in one bounded slice. Rollback is restoring the previous Next.js frontend and Dockerfile because backend and database are untouched.

## Open Questions

- [ ] None blocking.
