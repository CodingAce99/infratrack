# Proposal: Angular Dashboard Migration (First Parity Slice)

## Intent
Replace the Next.js dashboard with an Angular SPA that preserves all functional value (asset CRUD, metrics polling, sparklines, edit flows) while establishing a clearly structured Angular architecture and a notable visual upgrade. Signals that the candidate can structure a serious frontend, not just use Angular syntax.

## Scope

### In Scope
- Angular app skeleton (standalone components, routing at `/`, `proxy.conf.json`)
- `AssetService` + `MetricService` (RxJS, `shareReplay`, imperative `refresh()`)
- Full dashboard CRUD: `Dashboard`, `Header`, `AssetCard`, `CreateAssetModal`, `EditAssetPanel`, `ConfirmDialog`
- `MetricGauge`, custom-SVG `Sparkline` (no charting library), `StatusBadge`
- Dark-theme default design system (CSS custom properties, threshold color logic)
- Updated `frontend/Dockerfile` (`dist/frontend/browser`) + nginx config
- Frontend testing foundation using Angular's official testing stack for the first slice

### Out of Scope
- Login page, JWT interceptor, route guards (Sprint 7.4–7.5)
- Dark/light toggle, animations, advanced skeletons, responsive grid refinement
- Backend changes; the Angular app consumes the existing REST API unchanged

## Capabilities

### New Capabilities
- `angular-dashboard`: Angular SPA delivering asset inventory CRUD, live metrics polling, sparklines, and inline edit flows with smart/dumb component boundaries and RxJS service state.

### Modified Capabilities
- None. No spec-level backend behavior changes; the REST API contract is consumed as-is.

## Approach
Standalone Angular components with explicit smart (service-injecting) vs dumb (input/output-only) boundaries. `AssetService` owns the asset list stream + mutations + `refresh$` trigger; `MetricService` provides per-asset history observables. `ReactiveFormsModule` for forms. Custom SVG sparkline (~30 LOC). Dark theme via CSS custom properties. Dev proxy + nginx reverse proxy keep `API_BASE_URL` relative, unchanged from current model.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `frontend/` | Removed | Next.js app replaced |
| `frontend/` (Angular) | New | Services, components, models, styles, routing |
| `frontend/Dockerfile` | Modified | Build/copy paths to `dist/frontend/browser` |
| `frontend/proxy.conf.json` | New | Dev API proxy |
| `.github/workflows/ci.yml` | Modified | Angular build/test steps |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| SWR→RxJS mental-model shift | Med | Service-encapsulated `refresh()`; documented contract |
| Charting gap (Recharts→SVG) | Low | Custom SVG is ~30 LOC; no library |
| Build path churn breaks Docker | Med | Update Dockerfile + CI in same slice |
| No frontend testing precedent | Med | Establish minimal test setup in this slice |

## Rollback Plan
Keep the Next.js `frontend/` on a branch; the Angular work lands on a feature branch. If the slice fails verification, abandon the Angular branch and restore the Next.js frontend + original Dockerfile/CI. No backend or DB changes to revert.

## Dependencies
- Angular CLI, standalone components, RxJS, ReactiveFormsModule
- Existing Infratrack REST API (unchanged)

## Success Criteria
- [ ] All current dashboard functional value present: list, create, edit (status/IP/credentials), delete, metrics polling, sparklines
- [ ] Smart/dumb component boundaries enforced and documented
- [ ] Custom SVG sparkline renders with no charting dependency
- [ ] Dark-theme default ships with notable visual improvement over current
- [ ] `docker-compose up -d` serves the Angular dashboard end-to-end
- [ ] Frontend has a runnable test suite (non-zero)
