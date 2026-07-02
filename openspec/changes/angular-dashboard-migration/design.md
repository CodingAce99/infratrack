# Design: Angular Dashboard Migration — PR3 Smart Dashboard Architecture

## Technical Approach

PR3 replaces the Angular placeholder route with a real operations dashboard while reusing PR1 services/models and PR2 presentational components. `DashboardComponent` is the composition root: it owns the shared asset-list subscription, 60-second refresh cadence, modal visibility, confirmation messages, connection status, and the single-edit-card id. Child components stay narrowly scoped: header emits intent, create/edit forms call existing service methods, and asset cards compose metrics plus presentational UI.

## Architecture Decisions

| Decision | Choice | Alternatives considered | Rationale |
|---|---|---|---|
| State root | Keep dashboard-level UI coordination in `DashboardComponent`; keep API state in `AssetService`/`MetricService` | Add a new global store | Existing PR1 services already expose replayed assets, mutation revalidation, and errors; a store would add review size and duplicate state. |
| Single edit invariant | Dashboard owns `editingAssetId`; cards receive whether they are active and emit edit/close requests | Fully local card state | Local-only state cannot guarantee “one editing card” across siblings. A single id is explicit and cheap. |
| Refresh safety | Dashboard runs the 60s asset-list timer but does not destroy modal/panel state or reset form-owned values | Put timer inside `AssetService` | PR1 intentionally kept service refresh explicit. Dashboard has the interaction context needed to avoid disrupting users. |
| Connection indicator | Header displays “connected” only after the latest asset-list check succeeds; latest failure means disconnected | Derive from browser online status | The requirement is API/backend reachability now, not network availability. |
| Future roles | Add input-ready affordance boundaries such as `canManage` defaults, but no auth service, JWT interceptor, guards, or role fetching | Implement real frontend auth now | Backend auth exists, but frontend auth is a later slice. PR3 should prepare UI seams without pulling auth scope forward. |

## Data Flow

```text
Route / ──→ DashboardComponent
              │
              ├─ subscribes assets$/loading$/error$ ──→ AssetService ──→ /api/v1/assets
              ├─ passes count + connection ───────────→ HeaderComponent
              ├─ opens create modal ──────────────────→ CreateAssetModalComponent
              └─ renders cards + editingAssetId ──────→ AssetCardComponent × N
                                                         ├─ MetricService.history$(id)
                                                         └─ EditAssetPanelComponent
```

Refresh sequence:

```text
timer(60s) → DashboardComponent → AssetService.refresh()
success → assets$ updates cards/header, connection=true
failure → last asset list remains, connection=false, visible error
open modal/panel → component instance remains mounted; form state is not rewritten
```

Successful create closes the modal, calls the existing `createAsset()` path, lets `AssetService` revalidate, and shows a dashboard-level confirmation. Failed mutations show form/section errors and preserve the previous list.

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `frontend-angular/src/app/dashboard/dashboard.component.ts` | Create | Composition root for assets, refresh, connection status, modal state, editing id, confirmation/error messages. |
| `frontend-angular/src/app/dashboard/header.component.ts` | Create | Present asset count, API connection indicator, and add trigger. |
| `frontend-angular/src/app/assets/create-asset-modal.component.ts` | Create | Reactive five-field create form using `ASSET_TYPES` and `AssetService.createAsset()`. |
| `frontend-angular/src/app/assets/asset-card.component.ts` | Create | Card container using `StatusBadgeComponent`, `MetricGaugeComponent`, `SparklineComponent`, and `MetricService.history$(id)`. |
| `frontend-angular/src/app/assets/edit-asset-panel.component.ts` | Create | Separate status/IP/credentials save sections and delete confirmation using `ConfirmDialogComponent`. |
| `frontend-angular/src/app/app.routes.ts` | Modify | Bind `DashboardComponent` eagerly via `component` (replacing the `loadComponent` placeholder) at `/`. |
| `frontend-angular/src/app/core/asset.service.ts` | Reuse | Keep existing `assets$`, `refresh()`, CRUD methods, and failure-preserves-list behavior. |
| `frontend-angular/src/app/core/metric.service.ts` | Reuse | Keep per-asset polling streams and empty-history fallback. |

## Interfaces / Contracts

- `HeaderComponent`: inputs `assetCount`, `isConnected`, optional `canManage`; output `addAsset`.
- `CreateAssetModalComponent`: input `isOpen`; outputs `close`, `created(message)`; owns its `FormGroup` and API error display.
- `AssetCardComponent`: inputs `asset`, `isEditing`, optional `canManage`; outputs `requestEdit`, `closeEdit`; owns only metrics subscription and visual card behavior.
- `EditAssetPanelComponent`: input `asset`; outputs `updated`, `deleted`, `cancel`; owns independent form groups for status, IP, credentials.
- `DashboardComponent`: owns `editingAssetId: string | null` and ensures opening one card clears the previous one.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | 60s refresh, connection true/false, single-edit id transitions | Angular tests with mocked services and fake timers where useful. |
| Component | 409 create error, success confirmation, empty metrics history, edit-panel section saves | Standalone component tests with mocked `AssetService`/`MetricService`. |
| Integration | `/` renders dashboard instead of placeholder | Route/component test. |
| E2E | Browser auth/roles | Out of scope. |

## Migration / Rollout

No data migration required. PR3 stays on `feat/angular-smart-dashboard` and does not touch Docker/CI cutover, Next.js removal, backend contracts, or `frontend-angular/angular.json` analytics.

## Open Questions

- [ ] None.
