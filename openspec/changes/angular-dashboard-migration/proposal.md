# Proposal: Angular Dashboard Migration — PR3 Smart Dashboard Components

## Intent
PR3 turns the scaffold + presentational components shipped in PR1/PR2 into a usable monitoring/operations dashboard. Today `/` renders a placeholder and the API services are unused, so the migration delivers zero user value. PR3 wires `Dashboard`, `Header`, `AssetCard`, `CreateAssetModal`, and `EditAssetPanel` to `AssetService`/`MetricService`, adds the single-edit-card invariant, and makes the 60s refresh interaction-safe. After PR3 the dashboard is a credible ops surface; PR4 only does Docker/CI cutover and Next.js removal.

## Scope

### In Scope
- `HeaderComponent`: asset count, API connection indicator, "+ Add Asset" trigger
- `CreateAssetModalComponent`: `ReactiveFormsModule` 5-field form, submit→`AssetService`, 409/400 error display
- `AssetCardComponent`: wires `MetricService.history$`, owns `isEditing`
- `EditAssetPanelComponent`: separate status/IP/credentials saves + delete via `ConfirmDialog`
- `DashboardComponent`: composition root, subscribes `assets$`, owns 60s list refresh that never disrupts in-progress interaction
- Single-edit-card invariant (one card editing at a time)
- Post-create affordance: close modal + revalidate list + visible confirmation
- Route wiring: replace placeholder with real `DashboardComponent` at `/`
- Tests: 409 duplicate IP, empty metrics history, single-edit invariant, connection indicator

### Out of Scope
- Login page, JWT interceptor, route guards — Sprint 7.4–7.5 (structural placeholders only, no real frontend auth this PR)
- Docker/CI cutover and Next.js removal (PR4)
- Unrelated local change `frontend-angular/angular.json` (`cli.analytics=false`) — explicitly excluded
- Backend or REST contract changes

## Capabilities

### New Capabilities
- None. PR3 implements against the existing `angular-dashboard` capability spec.

### Modified Capabilities
- `angular-dashboard`: adds interaction-safety requirements (60s refresh must not close modal/panel or overwrite in-progress forms), single-edit-card invariant, header connection-indicator semantics (= API responding now), and post-create confirmation affordance.

## Approach
Smart components inject services; dumb components (already shipped) take `@Input`/emit `@Output` only. `DashboardComponent` owns the 60s refresh timer plus an interaction-active gate that suppresses disruptive refresh during open modal/panel or dirty forms. `AssetCardComponent` coordinates a shared editing-id stream for the single-edit invariant. Per-card metrics streams stay isolated. Successful create: close modal → `AssetService.refresh()` → visible confirmation.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `frontend-angular/src/app/dashboard/dashboard.*` | New | Composition root, interaction-safe 60s refresh |
| `frontend-angular/src/app/dashboard/header.*` | New | Count, connection indicator, add trigger |
| `frontend-angular/src/app/assets/asset-card.*` | New | Card shell, owns `isEditing`, wires `MetricService` |
| `frontend-angular/src/app/assets/create-asset-modal.*` | New | Reactive form + submit |
| `frontend-angular/src/app/assets/edit-asset-panel.*` | New | Status/IP/credentials saves + delete confirm |
| `frontend-angular/src/app/app.routes.ts` | Modified | Render real `DashboardComponent` at `/` |
| `frontend-angular/angular.json` | Unchanged | `cli.analytics=false` change is NOT PR3 |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Instance-template + behavioral logic exceeds 400-line budget | High | Slice PR3 into a chained sub-PR if forecast confirms; keep templates lean |
| Single-edit invariant structurally couples cards via shared state | Med | One editing-id stream in `DashboardComponent`; cards observe/emit only |
| 60s refresh disrupts open forms | Med | Interaction-active gate; suppress while modal/panel open or form dirty |
| Route guard/auth affordances accidentally pulled in | Low | Explicit out-of-scope; placeholders only, no interceptor/guard |

## Rollback Plan
PR3 lands on `feat/angular-smart-dashboard`. On verification failure, revert the PR without affecting PR1/PR2 already on `main`. Placeholder route remains the safe default until PR4 cutover. No backend/database changes to revert.

## Dependencies
- PR1 services + PR2 presentational helpers
- Existing Infratrack REST API (unchanged)

## Success Criteria
- [ ] `/` renders the real dashboard (no placeholder), Header, and asset cards
- [ ] Create closes modal, revalidates list, shows visible confirmation
- [ ] 409 duplicate IP shows form-level error, previous list preserved
- [ ] Only one asset card editable at a time
- [ ] 60s refresh never closes modal/panel or overwrites in-progress forms
- [ ] Empty metrics history renders stable empty state
- [ ] Header connection indicator reflects current API reachability
- [ ] No real frontend auth/JWT/route guards introduced
- [ ] `npm test` from `frontend-angular/` passes with the new PR3 tests