# Tasks: Angular Dashboard Migration

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines (PR3) | 1,000–1,200 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes (overall migration: PR1→PR4) |
| Subdivide PR3 further? | No — tightly coupled via DashboardComponent |
| Suggested split | PR 1 → PR 2 → PR 3 → PR 4 |
| Delivery strategy | ask-on-risk |
| Chain strategy | stacked-to-main |

Decision needed before apply: Yes — PR3 exceeds 400-line budget; user must accept size:exception
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

**PR3 subdivision rationale**: `DashboardComponent` is the composition root wiring Header, AssetCard, CreateAssetModal, and EditAssetPanel. All smart components depend on it for the single-edit invariant, interaction-safe refresh, and connection state. Splitting creates non-functional intermediates. Keep PR3 as one slice with `size:exception`.

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Angular scaffold, models, services, theme | PR 1 ✅ | Foundation; tests included |
| 2 | Presentational components | PR 2 ✅ | StatusBadge, MetricGauge, Sparkline, ConfirmDialog |
| 3 | Smart dashboard components | PR 3 (this) | Dashboard, Header, cards, modal, edit panel, tests |
| 4 | Docker/CI + Next.js removal | PR 4 | Dockerfile, nginx, CI, remove Next.js |

## Phase 1: Angular Foundation & Services

- [x] 1.1 Angular CLI scaffold
- [x] 1.2 Models: `Asset`, `MetricSnapshot`, enums in `core/models.ts`
- [x] 1.3 `ApiError` class in `core/api-error.ts`
- [x] 1.4 `AssetService`: `assets$`, `loading$`, `error$`, `refresh()`, CRUD
- [x] 1.5 `MetricService`: `history$(assetId)` with timer polling
- [x] 1.6 `proxy.conf.json`: dev proxy /api → backend
- [x] 1.7 Dark theme in `styles.css`
- [x] 1.8 App shell: `main.ts`, `index.html`, `app.config.ts`, `app.routes.ts`, `app.component.ts`
- [x] 1.9 Test: AssetService mutates list, errors preserve previous state
- [x] 1.10 Test: MetricService per-asset polling isolation

### PR1 Correction Batch

- [x] Coverage for `AssetService.refresh()` load-error branch
- [x] Remove unused `RouterOutlet` from `DashboardPlaceholderComponent`
- [x] Remove unused `provideHttpClientTesting` from production source
- [x] Delete root-level accidental `package-lock.json`

## Phase 2: Presentational Components

- [x] 2.1 `StatusBadgeComponent`: input status, pure presentational
- [x] 2.2 `MetricGaugeComponent`: input value, threshold color
- [x] 2.3 `SparklineComponent`: custom SVG path, empty state, no chart library
- [x] 2.4 `ConfirmDialogComponent`: input message, output confirm/cancel
- [x] 2.5 Test: all presentational components render from inputs correctly

## Phase 3: Smart Dashboard Components

- [x] 3.1 `HeaderComponent` — `@Input({required:true}) assetCount`, `@Input() isConnected=false`, `@Input() canManage=true`, `@Output() addAsset`. Template: count badge + connection dot (green/gray) + "+ Add Asset" button.
  Verify: renders count, toggles connection dot, emits `addAsset` on click.

- [x] 3.2 `CreateAssetModalComponent` — `@Input({required:true}) isOpen=false`, `@Output() close`, `@Output() created`. Reactive form with `ReactiveFormsModule`: name (required), type (select via `ASSET_TYPES`), IP (required, pattern), username (required), password (required). Submit → `AssetService.createAsset()`. On success: emit `created('Asset added')`. On 409: show inline field-level error. On 4xx: show general error.
  Verify: submits valid form, shows 409 inline error, preserves list on failure.

- [x] 3.3 `AssetCardComponent` — `@Input({required:true}) asset`, `@Input() isEditing=false`, `@Input() canManage=true`, `@Output() requestEdit`, `@Output() closeEdit`. Subscribes `MetricService.history$(asset.id)` per card isolation. Composes `StatusBadge`, `MetricGauge`×3 (CPU/Memory/Disk), `Sparkline`. Owns `isEditing` toggle locally; emits to parent for single-edit coordination.
  Verify: renders metrics from observable, toggle edit emits, empty metrics renders stable empty state.

- [x] 3.4 `EditAssetPanelComponent` — `@Input({required:true}) asset`, `@Output() updated`, `@Output() deleted`, `@Output() cancel`. Three independent sections (status dropdown, IP input, credentials pair). Each section calls its own `AssetService.update*()` method. Delete section `AssetService.deleteAsset()` with `ConfirmDialog` cancel guard.
  Verify: each section saves independently, delete opens confirm dialog, cancel does nothing.

- [x] 3.5 `DashboardComponent` — composition root. Subscribes `AssetService.assets$`/`loading$`/`error$`. Owns `editingAssetId: string | null` for single-edit invariant. 60s `setInterval` → `AssetService.refresh()` with interaction-active gate (`!isModalOpen && !editingAssetId`). Owns modal state `isCreateModalOpen`, confirmation message, connection status from latest API check. Passes `canManage: true` so PR3 management actions stay usable (future seam — real frontend auth is a later slice).
  Verify: renders Header + cards list, single edit invariant, safe refresh preserves modal/panel, connection reflects latest API success/failure.

- [x] 3.6 Modify `app.routes.ts` — replace the `loadComponent` placeholder with `DashboardComponent` eager import at path `''`. Remove `DashboardPlaceholderComponent` import. Bind `DashboardComponent` via the route's eager `component` property (not `loadComponent`).
  Verify: `/` renders DashboardComponent, placeholder no longer referenced.

- [x] 3.7 Test: 409 duplicate IP shows form-level error in `CreateAssetModal`. Mock `AssetService.createAsset()` to return `ApiError(409,"IP address already in use")`. Assert error text visible in modal form. Verify `assets$` list unchanged.
  Verify: spec scenario passes.

- [x] 3.8 Test: `AssetCard` handles empty metrics history. Mock `MetricService.history$` to return `of([])`. Assert stable empty state (no gauge errors, sparkline shows "No data").
  Verify: spec scenario passes.

- [x] 3.9 Test: interaction-safe 60s refresh. Open `CreateAssetModal`, advance timer. Modal stays open, form input unchanged. Same with `editingAssetId` set — refresh suppressed.
  Verify: spec scenario passes.

- [x] 3.10 Test: connection indicator API reachability. Make `AssetService.error$` emit on load failure → header shows disconnected. Next successful `refresh()` → header shows connected.
  Verify: spec scenario passes.

## Phase 4: Docker, CI & Cleanup

- [ ] 4.1 Update `Dockerfile`: Angular build → `dist/frontend/browser` → nginx
- [ ] 4.2 Update `nginx.conf`: keep `/api/` proxy + SPA fallback
- [ ] 4.3 Update `.github/workflows/ci.yml`: Node setup, Angular install/build/test
- [ ] 4.4 Remove Next.js files: `app/`, `components/`, `hooks/`, `lib/`, configs
- [ ] 4.5 Verify: `docker-compose up -d` serves Angular dashboard
- [ ] 4.6 Verify: `ng test` passes with non-zero tests
