# Tasks: Angular Dashboard Migration

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 2,000–2,500 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 → PR 2 → PR 3 → PR 4 |
| Delivery strategy | ask-on-risk |
| Chain strategy | stacked-to-main |

Decision needed before apply: No — resolved as stacked-to-main
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Angular scaffold, models, services, theme | PR 1 | Foundation for all other units; tests included |
| 2 | Presentational components | PR 2 | StatusBadge, MetricGauge, Sparkline, ConfirmDialog; tests included |
| 3 | Smart dashboard components | PR 3 | Dashboard, Header, AssetCard, modals, edit panel; tests included |
| 4 | Docker/CI + Next.js removal | PR 4 | Dockerfile, nginx, CI, delete old frontend |

## Phase 1: Angular Foundation & Services

- [x] 1.1 Angular CLI scaffold: `package.json`, `angular.json`, `tsconfig.json`
- [x] 1.2 Models: `Asset`, `MetricSnapshot`, enums in `core/models.ts`
- [x] 1.3 `ApiError` class in `core/api-error.ts`
- [x] 1.4 `AssetService`: `assets$`, `loading$`, `error$`, `refresh()`, CRUD via HttpClient
- [x] 1.5 `MetricService`: `history$(assetId)` with timer(0,60000) polling
- [x] 1.6 `proxy.conf.json`: dev proxy /api → backend
- [x] 1.7 Dark theme in `styles.css` (CSS custom properties)
- [x] 1.8 App shell: `main.ts`, `index.html`, `app.config.ts`, `app.routes.ts`, `app.component.ts`
- [x] 1.9 Test: AssetService mutates list, errors preserve previous state
- [x] 1.10 Test: MetricService per-asset polling isolation

### PR1 Correction Batch (post-verify cleanup, no new tasks)

- [x] Add runtime coverage for `AssetService.refresh()` load-error branch (error$ emission + loading reset + last-list preservation) — completes task 1.9 branch coverage
- [x] Remove unused `RouterOutlet` import from `DashboardPlaceholderComponent` (resolves `TS-998113`)
- [x] Remove unused `provideHttpClientTesting` import from `app.config.ts` production source
- [x] Delete empty, unintentional root-level `package-lock.json` (no root `package.json` exists)
- Deferred to Phase 3 (task 3.5): spec-level 60-second asset-list refresh. Per design, `AssetService` owns only explicit `refresh()`; the automatic 60s cadence is a `DashboardComponent` subscription/wiring concern (mirrors the Next.js `useAssets` hook `refreshInterval`). Adding a timer inside the service now would deviate from the design and cross slice boundaries.

## Phase 2: Presentational Components

- [ ] 2.1 `StatusBadgeComponent`: input status, pure presentational
- [ ] 2.2 `MetricGaugeComponent`: input value, threshold color (green/amber/red)
- [ ] 2.3 `SparklineComponent`: custom SVG path, empty state, no chart library
- [ ] 2.4 `ConfirmDialogComponent`: input message, output confirm/cancel
- [ ] 2.5 Test: all presentational components render from inputs correctly

## Phase 3: Smart Dashboard Components

- [ ] 3.1 `HeaderComponent`: asset count, connection indicator, add button
- [ ] 3.2 `CreateAssetModalComponent`: ReactiveForm, submit→AssetService, error display
- [ ] 3.3 `AssetCardComponent`: wires MetricService, owns isEditing state
- [ ] 3.4 `EditAssetPanelComponent`: status/IP/credentials save sections + delete via ConfirmDialog
- [ ] 3.5 `DashboardComponent`: composition root, subscribes AssetService
- [ ] 3.6 Wire AppComponent: render Dashboard at `/`
- [ ] 3.7 Test: modal shows error on 409 duplicate IP
- [ ] 3.8 Test: AssetCard handles empty metrics history

## Phase 4: Docker, CI & Cleanup

- [ ] 4.1 Update `Dockerfile`: Angular build → `dist/frontend/browser` → nginx
- [ ] 4.2 Update `nginx.conf`: keep `/api/` proxy + SPA fallback
- [ ] 4.3 Update `.github/workflows/ci.yml`: Node setup, Angular install/build/test
- [ ] 4.4 Remove Next.js files: `app/`, `components/`, `hooks/`, `lib/`, configs
- [ ] 4.5 Verify: `docker-compose up -d` serves Angular dashboard
- [ ] 4.6 Verify: `ng test` passes with non-zero tests
