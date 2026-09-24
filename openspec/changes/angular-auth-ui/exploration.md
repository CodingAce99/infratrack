## Exploration: Angular Auth UI (`angular-auth-ui`)

### Current State

The Infratrack backend (Sprints 7.1–7.3) has a fully working JWT auth stack:
- **Login endpoint**: `POST /api/v1/auth/login` → `200 OK` with body `{token, type:"Bearer", username, role}`
- **Auth rules**: `GET /api/v1/assets/**` → any authenticated role (`ADMIN` or `VIEWER`); `POST/PUT/DELETE` → `ADMIN` only
- **Token format**: Bearer JWT (HS256), 1h expiry, no refresh token
- **Filter rejections**: 401 → `{"error":"Unauthorized"}`; 403 → `{"error":"Forbidden"}` (written directly by Spring Security handlers, not `@RestControllerAdvice`)
- **Dev profile**: No auth beans at all (`permitAll` chain); backend tests run without tokens.

The Angular frontend (PR4, `frontend-angular/`) currently has **zero auth infrastructure**:
- `AssetService` calls `/api/v1/assets` with plain `HttpClient` — no `Authorization` header
- `DashboardComponent.canManage` is hardcoded `true` — management controls are always visible
- App routes only map `/` → `DashboardComponent`; no login route or auth guard
- `provideHttpClient()` in `app.config.ts` has no interceptors
- No token storage, no auth state, no login/logout UI
- Testing uses `AssetServiceMock` and `MetricServiceMock` with Jasmine spies

Running the Docker Compose stack in demo mode results in `Unauthorized` on every API call because the secured filter chain is active and Angular sends no `Authorization` header.

### Affected Areas

| File | Why affected |
|------|-------------|
| `frontend-angular/src/app/app.routes.ts` | Needs a `/login` route and an auth guard for `/` |
| `frontend-angular/src/app/app.config.ts` | Needs `withInterceptors([authInterceptor])` |
| `frontend-angular/src/app/core/models.ts` | Needs `LoginRequest`, `LoginResponse`, `UserRole` types |
| `frontend-angular/src/app/core/api-error.ts` | Already used; 401/403 responses will flow through existing `toApiError` |
| `frontend-angular/src/app/core/asset.service.ts` | No direct changes, but interceptor will add headers transparently |
| `frontend-angular/src/app/core/metric.service.ts` | Same — interceptor covers all HTTP traffic |
| `frontend-angular/src/app/dashboard/dashboard.component.ts` | `canManage` must stop being `true` and derive from auth role |
| `frontend-angular/src/app/dashboard/header.component.ts` | Should show login/logout affordance and current user |
| `frontend-angular/src/app/testing/asset-service.mock.ts` | May need `AuthServiceMock` counterpart |
| `frontend-angular/src/app/app.component.ts` / `.html` | May need a conditional shell (brand + login vs dashboard) or keep router-driven |
| `openspec/config.yaml` | Frontend test runner (`npm test`, Karma/Jasmine) should be noted for auth slice TDD |

### Approaches

#### 1. **Minimal Viable Auth (Recommended)**
Add just enough to make the dashboard usable again: login page, token storage, HTTP interceptor, role-driven `canManage`, and logout. No refresh tokens, no route guards with redirect loops, no complex RBAC UI.

- **Pros**: Unblocks the demo; aligns with Sprint 7.4 roadmap item; small, reviewable PR; no visual redesign scope creep.
- **Cons**: Token expiry after 1h will force a re-login (acceptable for MVP); no automatic silent renewal.
- **Effort**: Medium (~250–350 changed lines across Angular files + tests).

#### 2. **Full Auth Shell with Route Guards & Redirects**
Add `AuthGuard`, `NoAuthGuard`, login-redirect logic, a dedicated `AuthLayoutComponent`, and deep-link preservation (`returnUrl`).

- **Pros**: More robust UX for bookmarked URLs; cleaner separation of authenticated vs public layouts.
- **Cons**: Higher complexity; more test surface; overkill for a single-dashboard app; risks exceeding 400-line review budget.
- **Effort**: High (~450–600 lines).

#### 3. **Modal-based Login (instead of route)**
Keep the dashboard at `/` and render a login modal overlay when the user is unauthenticated.

- **Pros**: No route changes; simpler router config; dashboard shell stays loaded.
- **Cons**: Harder to handle 403 vs 401 distinctions; URL does not reflect auth state; breaks the “login is a distinct page” mental model.
- **Effort**: Medium-High (modal state management + overlay styling).

### Recommendation

**Approach 1 (Minimal Viable Auth)**. The immediate goal is to restore dashboard functionality in Docker demo mode, not to build a full IAM portal. The 1h token expiry is acceptable for the 1.0 release; roadmap item 7.4 explicitly asks for “Angular login page, token storage service, authenticated API calls.”

Scope within this change:
1. `LoginComponent` (route `/login`) — Reactive form, username + password, submits to backend.
2. `AuthService` — login/logout methods, stores token in `localStorage`, exposes `user$` (or `isAuthenticated` + `role` signals).
3. `authInterceptor` — adds `Authorization: Bearer <token>` to all `/api/*` requests; on 401, calls `AuthService.logout()` and redirects to `/login`.
4. Update `DashboardComponent.canManage` to read from `AuthService.role === 'ADMIN'`.
5. Update `HeaderComponent` to show username and a logout button when authenticated.
6. Add `/login` route; protect `/` with a lightweight guard (if no token, redirect to `/login`).
7. Karma/Jasmine tests for `AuthService`, `LoginComponent`, interceptor, and guard.

Explicit **non-goals** (to keep the slice clean):
- Token refresh / silent renewal
- Password reset or registration UI
- Fine-grained per-field permissions beyond `canManage`
- Visual redesign of metrics (sparklines/gauges) — that is a later polish slice
- Dark/light mode toggle

### Risks

1. **Dev profile mismatch**: Backend dev profile has no auth beans, but the Angular dev server (`npm run dev`) proxies to `localhost:8080`. If the backend is running in dev profile, API calls succeed without tokens. If running in demo profile, the interceptor must still send the token. The frontend should work against both — the interceptor adds the header when a token exists; if none exists, the request proceeds unauthenticated (dev will accept it, demo will 401).
2. **401 during active interactions**: If the token expires while a modal or edit panel is open, the next API call will 401, the interceptor will redirect to `/login`, and the user loses in-progress form state. For MVP this is acceptable; a future slice can add an expiry warning.
3. **Test mocking surface**: Adding `AuthService` and an interceptor means existing `DashboardComponent` tests need to provide an `AuthService` mock or the test bed must be updated. The existing mock pattern (`AssetServiceMock`) should be extended, not replaced.
4. **400-line budget**: The recommended scope (7 items above + tests) is close to the 400-line review limit. If it grows, split into two chained PRs: (A) core auth (service + interceptor + login page), (B) guard + dashboard integration + logout UX.

### Product Decisions Needed Before Proposal

1. **Token storage**: `localStorage` (survives refresh, simple) vs `sessionStorage` (survives tab refresh, not cross-tab). `localStorage` is fine for MVP.
2. **Post-login redirect**: Always to `/`, or preserve a `returnUrl`? For MVP, always `/`.
3. **Logout UX**: Header button only, or also a keyboard shortcut? Header button is sufficient.
4. **Error display on login**: Inline form errors (401 → “Invalid credentials”) or a toast? Inline is consistent with existing form patterns.
5. **Dev workflow**: Should `npm run dev` against a dev-profile backend remain auth-free? Yes — the interceptor only adds a header when a token is present; dev profile accepts all requests anyway.

### Ready for Proposal

**Yes**. The backend auth contract is stable and well-documented. The frontend gap is clear. The scope candidates (login page, AuthService, token storage, HTTP interceptor, logout, 401/403 groundwork) are all well-understood and fit within the review budget if kept minimal. The orchestrator should ask the user to confirm the product decisions above, then proceed to `sdd-propose`.

### Open Questions

- None blocking exploration; the listed product decisions are lightweight and can be resolved during proposal.
