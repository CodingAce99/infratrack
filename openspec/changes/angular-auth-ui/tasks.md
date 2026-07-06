# Tasks: Angular Auth UI

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~650-750 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 (Auth Core) → PR 2 (Login UI + Integration) |
| Delivery strategy | force-chained |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Auth session core | PR 1 | Models + AuthService + Interceptor (Bearer + clear state) + app.config + tests. Base = feature/tracker branch. No guard, no redirect — fully additive. |
| 2 | Login UI + route protection | PR 2 | Guard + LoginComponent + Header/Dashboard wiring + routes + interceptor 401 redirect + tests. Base = PR 1 branch. |

## Phase 1: Foundation — Models & AuthService

- [ ] 1.1 Add `LoginRequest`, `LoginResponse`, `AuthUser` types to `core/models.ts` (reuses existing `UserRole`)
- [ ] 1.2 Create `core/auth.service.ts`: login/logout, localStorage token, claim decode, `user$`/`role$`/`isAuthenticated$`
- [ ] 1.3 Write `core/auth.service.spec.ts`: mock login, logout clears, localStorage persistence, state restore, invalid token rejection

## Phase 2: HTTP Interceptor

- [ ] 2.1 Create `core/auth.interceptor.ts`: Bearer header for `/api/*`; 401 clears session only; 403 preserves state
- [ ] 2.2 Write `core/auth.interceptor.spec.ts`: header injection, no-header without token, 401 clears, 403 preserves
- [ ] 2.3 Register interceptor in `app.config.ts` via `withInterceptors([authInterceptor])`

## Phase 3: Route Guard

- [ ] 3.1 Create `core/auth.guard.ts`: `canActivate` redirects unauthenticated to `/login`
- [ ] 3.2 Write `core/auth.guard.spec.ts`: authenticated passes, unauthenticated redirects

## Phase 4: Login Page UI

- [ ] 4.1 Create standalone `login/login.component.ts` (ReactiveForms, username/password, inline 401 error, success navigates to `/`)
- [ ] 4.2 Write `login/login.component.spec.ts`: submit calls `AuthService.login()`, 401 shows error, success navigates

## Phase 5: Dashboard Integration & Route Wiring

- [ ] 5.1 Update `dashboard/dashboard.component.ts`: inject `AuthService`, derive `canManage` from role, pass to header
- [ ] 5.2 Update `dashboard/header.component.ts`: show username + logout, gate add button by `canManage`
- [ ] 5.3 Update `core/auth.interceptor.ts`: add 401 redirect to `/login` after login route exists
- [ ] 5.4 Update `app.routes.ts`: add `/login` route with `LoginComponent`, apply `authGuard` to dashboard
- [ ] 5.5 Update existing header/dashboard specs for role-driven affordances and logout
