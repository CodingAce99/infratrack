# Proposal: Angular Auth UI

## Intent

The backend JWT stack (Sprints 7.1–7.3) is complete, but the Angular dashboard has zero auth infrastructure. In Docker demo mode every API call returns `Unauthorized`. This change restores the dashboard by adding login, token storage, an HTTP interceptor, role-driven `canManage`, and logout — Sprint 7.4 of the roadmap.

## Scope

### In Scope
- `LoginComponent` at route `/login` (Reactive form: username + password)
- `AuthService` with login/logout, token in `localStorage`, `user$`/`role` state
- `authInterceptor` adding `Authorization: Bearer <token>` to `/api/*` requests
- Lightweight auth guard on `/` redirecting to `/login` when no token
- `DashboardComponent.canManage` derived from `AuthService.role === 'ADMIN'`
- Header logout button showing current user when authenticated
- Inline login error message on 401; groundwork handling for 403
- Karma/Jasmine specs for `AuthService`, `LoginComponent`, interceptor, guard

### Out of Scope
- Token refresh / silent renewal
- Registration / password reset UI
- Fine-grained per-field permissions beyond `canManage`
- Metrics/charts visual redesign, chart library choice, dark/light mode
- Advanced 403 UX beyond minimal guard/redirect

## Capabilities

### New Capabilities
- `angular-auth-session`: Frontend auth session — login/logout, token storage in `localStorage`, `user$`/`role` state, Bearer header injection via interceptor, 401→logout+redirect, minimal route guard.
- `angular-auth-ui`: Login page UI, inline error display, header logout affordance, role-driven `canManage` wiring into the dashboard shell.

### Modified Capabilities
None. (No existing OpenSpec spec covers frontend; the three existing specs are backend observability-only.)

## Approach

Approach 1 (Minimal Viable Auth) from exploration: add just enough to make the dashboard usable against the secured backend. Angular functional interceptor + standalone guard; `AuthService` as the shared auth root mirroring the `AssetService`/`MetricService` RxJS-state pattern. Interceptor adds header only when a token is present, so dev profile remains auth-free; on 401 it logs out and redirects to `/login`. Post-login always redirects to `/`.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `frontend-angular/src/app/app.routes.ts` | Modified | Add `/login` route; `/` protected by guard |
| `frontend-angular/src/app/app.config.ts` | Modified | `withInterceptors([authInterceptor])` |
| `frontend-angular/src/app/core/auth.service.ts` | New | Auth state root, login/logout, token storage |
| `frontend-angular/src/app/core/auth.interceptor.ts` | New | Bearer header + 401 handling |
| `frontend-angular/src/app/core/auth.guard.ts` | New | Redirect to `/login` when no token |
| `frontend-angular/src/app/login/` | New | Login page component + spec |
| `frontend-angular/src/app/dashboard/dashboard.component.ts` | Modified | `canManage` from role |
| `frontend-angular/src/app/dashboard/header.component.ts` | Modified | User display + logout button |
| `frontend-angular/src/app/core/models.ts` | Modified | `LoginRequest`, `LoginResponse`, `UserRole` |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| 401 mid-interaction loses in-progress form state | Medium | Acceptable for MVP; future slice adds expiry warning |
| Sliced PR near 400-line budget | Medium | Split into chained PRs if tasks forecast high: core auth, then guard/integration |
| Existing dashboard tests break without `AuthService` provider | Medium | Extend mock pattern (`AssetServiceMock` → `AuthServiceMock`) |
| Dev profile auth-free assumption changes | Low | Interceptor only adds header when token exists; dev `permitAll` unaffected |

## Rollback Plan

Revert the feature branch. No backend or DB changes; no migrations. Removing `authInterceptor`, `/login` route, guard, `AuthService`, and reverting `canManage` to `true` restores the unauthenticated dashboard against the dev-profile backend. Frontend artifacts are additive; rollback is `git revert` of the slice.

## Dependencies

- Backend login endpoint `POST /api/v1/auth/login` (stable since Sprint 7.2)
- Backend secured filter chain (demo/prod profiles) — unchanged

## Success Criteria

- [ ] Docker demo stack: dashboard loads after login, all API calls carry Bearer token
- [ ] `canManage` controls visible only for `ADMIN` role
- [ ] Header shows username and logout button; logout clears token and returns to `/login`
- [ ] 401 redirects to login; 403 produces minimal user feedback (groundwork)
- [ ] Karma specs pass (`npm test`) for `AuthService`, `LoginComponent`, interceptor, guard
- [ ] Dev profile (no token) still works unauthenticated
- [ ] Changed lines within 400-line review budget (or chained PRs planned)