# Design: Angular Auth UI

## Technical Approach

Add a minimal Angular-only auth layer around the existing secured backend. `AuthService` becomes the session state root, mirroring the current `AssetService`/`MetricService` RxJS pattern. It logs in through `POST /api/v1/auth/login`, persists the access token in `localStorage`, exposes user/role state, restores state on app startup by decoding token claims, and clears state on logout. A functional HTTP interceptor adds Bearer tokens only to `/api/*` requests and centralizes protected-request 401 handling. No backend, token refresh, registration, password reset, or visual redesign work is included.

## Architecture Decisions

| Decision | Options / Tradeoff | Choice and Rationale |
|---|---|---|
| Session owner | Component-local state is simple but duplicated; global stores are overkill. | Use `AuthService` with `BehaviorSubject`s, matching existing frontend service-state conventions. |
| Token storage | Memory-only is safer but loses session on refresh; cookies require backend changes. | Use `localStorage` as specified. Accept XSS exposure risk for MVP; mitigate later with stronger CSP/refresh-token design if needed. |
| Claims source | Trusting decoded JWT alone can be misleading; changing backend is out of scope. | Use login response username/role immediately and decode `sub`/`role` only to restore UI state after reload. Backend remains the authority because API requests are still server-validated. |
| Interceptor scope | Attaching to every URL can leak tokens to external resources. | Target only relative `/api/*` requests. Exclude `/api/v1/auth/login` from session-invalidation behavior so login 401 stays a form error. |
| 401/403 handling | Clearing on all auth errors is simple but wrong for authorization failures. | Protected API 401 clears session and redirects to `/login`; 403 does not clear state and is surfaced through existing error paths/minimal UI state. |

## Data Flow

```text
/login form -> AuthService.login() -> POST /api/v1/auth/login
                         | success
                         v
localStorage token <- AuthService user/role$ -> guard/header/dashboard canManage
                         |
                         v
authInterceptor -> /api/* + Authorization: Bearer <token>
                         |
        401 protected -> clear session -> Router /login
        401 login ----> inline invalid credentials, stay /login
        403 ----------> preserve session, surface forbidden error
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `frontend-angular/src/app/core/auth.service.ts` | Create | Login/logout, token persistence, claim decoding, `user$`, `role$`, `isAuthenticated`. |
| `frontend-angular/src/app/core/auth.interceptor.ts` | Create | Functional interceptor for `/api/*` Bearer injection and protected-request 401 redirect. |
| `frontend-angular/src/app/core/auth.guard.ts` | Create | Functional guard: `/` requires token; unauthenticated users redirect to `/login`. |
| `frontend-angular/src/app/login/login.component.ts` | Create | Standalone reactive login form with inline invalid-credentials error. |
| `frontend-angular/src/app/core/models.ts` | Modify | Add `LoginRequest`, `LoginResponse`, and auth user/session types while reusing `UserRole`. |
| `frontend-angular/src/app/app.routes.ts` | Modify | Add `/login`; protect `''`; keep wildcard redirect. |
| `frontend-angular/src/app/app.config.ts` | Modify | Register `withInterceptors([authInterceptor])`. |
| `frontend-angular/src/app/dashboard/dashboard.component.ts` | Modify | Derive `canManage` from `AuthService.role === 'ADMIN'`; pass user/logout data to header. |
| `frontend-angular/src/app/dashboard/header.component.ts` | Modify | Show username and logout control; keep add button role-gated. |
| `*.spec.ts` beside changed files | Create/Modify | Karma/Jasmine coverage for service, interceptor, guard, login, header/dashboard role behavior. |

## Interfaces / Contracts

```ts
type UserRole = 'ADMIN' | 'VIEWER';
interface LoginRequest { username: string; password: string; }
interface LoginResponse { token: string; type: 'Bearer'; username: string; role: UserRole; }
interface AuthUser { username: string; role: UserRole; }
```

JWT restore contract: decode payload client-side and read `sub` as username and `role` as `UserRole`. Invalid, missing, malformed, or expired-looking tokens are treated as no session locally; server validation still decides actual access.

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Unit | `AuthService` login/logout, localStorage, restore/decode, invalid tokens | Jasmine spies/fake `localStorage`, `HttpTestingController`. |
| Unit | `authInterceptor` targeting, Bearer header, login 401 exception, protected 401 clear+redirect, 403 preserves session | Angular HTTP testing providers with `provideHttpClient(withInterceptors(...))`. |
| Component | Login validation, success navigation, inline 401 error, header username/logout, role-driven management visibility | Standalone component tests using existing `data-testid` style and service mocks. |
| Routing | Guard allows authenticated `/`, redirects unauthenticated `/`, `/login` remains reachable | Router testing with functional guard providers. |

## Migration / Rollout

No migration required. Roll out as a frontend-only chained slice if task sizing exceeds the 400-line review budget: core auth first, then UI/route integration.

## Risks and Tradeoffs

- `localStorage` is vulnerable to XSS token theft; accepted for this MVP because no backend cookie/refresh-token design exists.
- No refresh tokens means expired sessions redirect to `/login` on the next protected API 401.
- Decoded claims are UI convenience only; never use them as proof of authorization beyond display/affordance gating.
- Mid-interaction 401 can lose unsaved form state; acceptable in scope.

## Open Questions

None.
