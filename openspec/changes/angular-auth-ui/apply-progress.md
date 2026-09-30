# Apply Progress: Angular Auth UI

## PR 1 Historical Slice Scope

This apply batch implements **PR 1 — Auth Core** only, as defined by the
`force-chained` / `feature-branch-chain` decision recorded in `tasks.md`.

- Branch: `feature/angular-auth-ui-auth-core`
- Base: feature/tracker branch for the Angular Auth UI change
- Completed tasks: 1.1, 1.2, 1.3, 2.1, 2.2, 2.3 (Phase 1 + Phase 2)
- Out of scope for this slice: Phase 3 (guard), Phase 4 (login UI),
  Phase 5 (dashboard integration) — these belong to PR 2.

## Completed Tasks

- [x] 1.1 Add `LoginRequest`, `LoginResponse`, `AuthUser` types to `core/models.ts` (reuses existing `UserRole`)
- [x] 1.2 Create `core/auth.service.ts`: login/logout, localStorage token, claim decode, `user$`/`role$`/`isAuthenticated$`
- [x] 1.3 Write `core/auth.service.spec.ts`: mock login, logout clears, localStorage persistence, state restore, invalid token rejection
- [x] 2.1 Create `core/auth.interceptor.ts`: Bearer header for `/api/*`; 401 clears session only; 403 preserves state
- [x] 2.2 Write `core/auth.interceptor.spec.ts`: header injection, no-header without token, 401 clears, 403 preserves
- [x] 2.3 Register interceptor in `app.config.ts` via `withInterceptors([authInterceptor])`

## Files Changed

| File | Action | What Was Done |
|------|--------|---------------|
| `frontend-angular/src/app/core/models.ts` | Modified | Added `LoginRequest`, `AuthUser`, `LoginResponse` interfaces (reuses existing `UserRole`). |
| `frontend-angular/src/app/core/auth.service.ts` | Created | `AuthService` (session root), `decodeAuthUser`, `AUTH_TOKEN_KEY`. Login/logout, localStorage persistence, constructor-time restore from stored token. |
| `frontend-angular/src/app/core/auth.service.spec.ts` | Created/Modified | RED spec for `AuthService` + `decodeAuthUser`. Fixed pre-existing TypeScript compile blockers in the authored RED spec (`.catch` on `Observable`, generic type args on `toBe`/`toEqual`) without changing assertions. |
| `frontend-angular/src/app/core/auth.interceptor.ts` | Created | Functional `authInterceptor`: Bearer header on `/api/*` when token exists; clears session on protected 401; preserves session on 403; skips login-URL 401. No redirect yet (PR 1 boundary). |
| `frontend-angular/src/app/core/auth.interceptor.spec.ts` | Created | RED-then-GREEN coverage for header injection, no-token, non-`/api/*`, protected 401 clears, login 401 no-clear, 403 preserve. |
| `frontend-angular/src/app/app.config.ts` | Modified | Registered `withInterceptors([authInterceptor])` in `provideHttpClient`. |
| `openspec/changes/angular-auth-ui/tasks.md` | Modified | Checked off Phase 1 + Phase 2 tasks (PR 1 slice). |

## TDD Cycle Evidence

| Task | Test File | Layer | Safety Net | RED | GREEN | TRIANGULATE | REFACTOR |
|------|-----------|-------|------------|-----|-------|-------------|----------|
| 1.1 | Production build + consuming auth tests | Structural | N/A (new) | ➖ Runtime RED/GREEN is not applicable to erased TypeScript interfaces | ✅ Angular compilation and consuming auth tests pass | ✅ ADMIN + VIEWER contracts consumed by runtime-tested code | ➖ Compile-time contract only |
| 1.2 | `core/auth.service.spec.ts` | Unit | N/A (new) | ✅ Test authored first (RED pre-existed; `auth.service.ts` absent) | ✅ 6 AuthService specs passing | ✅ ADMIN + VIEWER login, restore valid/invalid | Extracted `decodeAuthUser` + `base64UrlDecode` pure helpers |
| 1.3 | `core/auth.service.spec.ts` | Unit | N/A (new) | ✅ Same spec as 1.2 covers persistence/restore/invalidate paths | ✅ Verified within the same 14/14 pass (incl. 7 `decodeAuthUser` + 2 restore) | ✅ Invalid token, missing `sub`, invalid `role`, malformed payload | ➖ Same spec used for 1.2/1.3 |
| 2.1 | `core/auth.interceptor.spec.ts` | Unit | N/A (new) | ✅ Spec written before `auth.interceptor.ts` (RED: `TS2307 Cannot find module './auth.interceptor'`) | ✅ 7 specs passing | ✅ GET + PUT header injection, 401 protected vs 401 login, 403 preserve | ➖ None needed (one pure interceptor fn + catcher) |
| 2.2 | `core/auth.interceptor.spec.ts` | Unit | N/A (new) | ✅ Same spec as 2.1 | ✅ Same 7/7 pass incl. no-token, non-`/api/*`, login-401 exclusion, 403 preserve | ✅ Two header-injection paths + two error statuses | ➖ None needed |
| 2.3 | `core/app.config.ts` wiring | Structural | ✅ Full suite green before/after | ➖ Triangulation skipped: purely structural config wiring (single `withInterceptors([...])` call). Only one possible wiring; no branching logic. | ✅ Full `npm test` green (120/120) | ➖ Skipped (structural) | ➖ Skipped (structural) |

### Test Summary

- **Total behavioral tests written this slice**: 21 (14 `auth.service.spec` + 7 `auth.interceptor.spec`)
- **Structural validation**: auth model interfaces compile through their production consumers and the Angular production build
- **Layers used**: Unit (21) + structural compile-time validation
- **Approval tests (refactoring)**: None — no refactoring tasks
- **Pure functions created**: `decodeAuthUser(token)`, `base64UrlDecode(segment)`

## Test Execution

All frontend tests run from `frontend-angular/`:

```
npm test
# => Historical PR 1 run: 120 SUCCESS before removal of 3 literal-only model specs
```

Per-file TDD runs:

```
npm test -- --include=src/app/core/auth.service.spec.ts     # 14 SUCCESS
npm test -- --include=src/app/core/auth.interceptor.spec.ts  # 7 SUCCESS
npm test                                                     # Historical PR 1 run: 120 SUCCESS
```

Backend Maven tests (`./mvnw test -Dspring.profiles.active=dev`) are not
affected — this change is frontend-only and additive. Backend coverage stays
at its prior 173-passing baseline.

## Deviations from Design

- **`decodeAuthUser` exported as a pure helper** alongside `AuthService` (design
  listed it only conceptually). Made it pure/exported so the spec tri-*angular
  cases test it in isolation without instantiating the service. This matches
  the "Pure Function Preference" rule in `strict-tdd.md`.
- **PR 1 interceptor deliberately does not redirect to `/login`** (per the
  approved clarification and design's task 5.3). It only clears the session on
  protected 401. The redirect is deferred to PR 2 once the `/login` route
  exists. Tests assert `logout()` is called and explicitly do not assert a
  redirect.
- **Authored RED spec fixes**: the pre-existing `auth.service.spec.ts`
  (written before this apply batch) had two compile-blocker patterns —
  `service.login(...).catch(...)` (`.catch` is a Promise method, not an
  Observable operator) and `toBe<UserRole>`/`toEqual<AuthUser>` (Jasmine's
  typings reject generic type args). Fixes preserved the original assertions:
  `.catch` moved onto the `firstValueFrom(...)` Promise; generic type args
  replaced with `as` casts. No assertion values or intent changed.

## Issues Found

- **PR 1 diff is ~667 changed lines**, above the 400-line per-PR review budget.
  This is the genuine cost of Strict TDD coverage for the slice (tests
  dominate: ~407 of the 659 added lines are spec files). The chain was chosen
  precisely because the full feature exceeds the budget; PR 1 stays
  autonomous and is exactly the Auth Core unit the maintainer directed. If a
  smaller PR 1 is required, the auth.service spec could be split, but that
  would weaken TDDD coverage.
- Confirm `localStorage`-as-token-store XSS tradeoff is documented in
  `auth.service.ts` (it is). Mitigation deferred per the approved clarification.

## PR 2 Plan at PR 1 Completion

This checklist records the state when PR 1 ended. All items were subsequently
completed in the PR 2 section below.

- [ ] 3.1 Create `core/auth.guard.ts`: `canActivate` redirects unauthenticated to `/login`
- [ ] 3.2 Write `core/auth.guard.spec.ts`: authenticated passes, unauthenticated redirects
- [ ] 4.1 Create standalone `login/login.component.ts` (ReactiveForms, username/password, inline 401 error, success navigates to `/`)
- [ ] 4.2 Write `login/login.component.spec.ts`: submit calls `AuthService.login()`, 401 shows error, success navigates
- [ ] 5.1 Update `dashboard/dashboard.component.ts`: inject `AuthService`, derive `canManage` from role, pass to header
- [ ] 5.2 Update `dashboard/header.component.ts`: show username + logout, gate add button by `canManage`
- [ ] 5.3 Update `core/auth.interceptor.ts`: add 401 redirect to `/login` after login route exists
- [ ] 5.4 Update `app.routes.ts`: add `/login` route with `LoginComponent`, apply `authGuard` to dashboard
- [ ] 5.5 Update existing header/dashboard specs for role-driven affordances and logout

## PR 1 Historical Boundary

- **Mode**: chained PR slice (feature-branch-chain)
- **Current work unit**: PR 1 — Auth Core
- **Boundary**: starts from planning commit `d74e7ab`; ends with auth session
  state, Bearer interceptor, and `app.config` wiring in place. No guard, no
  `/login` route, no dashboard/header integration, no redirect logic.
- **Rollback**: `git revert` of the PR 1 commits. Removing `authInterceptor`
  from `app.config.ts` and deleting the auth files restores the
  unauthenticated-dashboard baseline.
- **Review budget impact**: ~667 changed lines for PR 1. Exceeds the 400-line
  per-PR guideline because TDD spec lines dominate. Flagged for the
  maintainer; the chain exists to keep the OVERALL review across PRs sane.

## Gate Review Corrections (follow-up commit on `feature/angular-auth-ui-auth-core`)

A fresh gate review of PR 1 surfaced a WARNING: `decodeAuthUser()` validated
`sub` and `role` but not `exp`, conflicting with `design.md` line 57
("Invalid, missing, malformed, or expired-looking tokens are treated as no
session locally"). Corrective work was applied as one work-unit commit:

| File | Action | What Was Done |
|------|--------|---------------|
| `frontend-angular/src/app/core/auth.service.ts` | Modified | Added `exp` claim validation: when `exp` is present it MUST be a finite numeric Unix-seconds value strictly in the future; non-numeric or elapsed `exp` returns `null` (no session) via a new pure `isFutureUnixSeconds` helper. Missing `exp` stays lenient (per existing test intent and the login response not being re-decoded here). |
| `frontend-angular/src/app/core/auth.service.spec.ts` | Modified | Added two new `describe` blocks (6 specs total): pure `decodeAuthUser` expired/non-numeric `exp` rejection + future `exp` acceptance (numeric and numeric-string), and `AuthService` restore-from-expired-JWT → unauthenticated + stored token dropped. |
| `frontend-angular/src/app/app.config.ts` | Modified | Fixed typo in interceptor registration comment: `protegected-request` → `protected-request`. No behavior change. |

### Decision: missing `exp` stays lenient

The design statement "missing, malformed, or expired-looking tokens are
treated as no session" groups missing tokens / missing critical claims together,
but the existing `fakeJwt` test helper (and the backend login response path,
which uses `res.username`/`res.role` directly and never passes through
`decodeAuthUser`) omit `exp`. Treating a missing `exp` as invalid would silently
break those existing patterns without a corresponding restore-flow risk. Per the
corrective instruction ("Missing `exp` may be treated according to the existing
design/test intent, but avoid trusting clearly expired tokens"), a *present*
`exp` is strictly enforced (numeric + strictly future); an *absent* `exp` is
left to existing behavior. Clearly expired tokens are never trusted.

### Corrective Test Execution

```
npm test -- --include=src/app/core/auth.service.spec.ts  # 20/20 SUCCESS (was 14; +6)
npm test                                                 # Historical run: 126/126 SUCCESS (before literal-only spec removal)
```

## PR 2 Progress — Login UI + Integration

PR 2 has now completed the remaining Login UI + Integration tasks in a
mentored implementation flow.

### Completed Tasks Added After PR 1

- [x] 3.1 Create `core/auth.guard.ts`: `canActivate` redirects unauthenticated to `/login`
- [x] 3.2 Write `core/auth.guard.spec.ts`: authenticated passes, unauthenticated redirects
- [x] 4.1 Create standalone `login/login.component.ts` (ReactiveForms, username/password, inline 401 error, success navigates to `/`)
- [x] 4.2 Write `login/login.component.spec.ts`: submit calls `AuthService.login()`, 401 shows error, success navigates
- [x] 5.1 Update `dashboard/dashboard.component.ts`: inject `AuthService`, derive `canManage` from role, pass to header
- [x] 5.2 Update `dashboard/header.component.ts`: show username + logout, gate add button by `canManage`
- [x] 5.3 Update `core/auth.interceptor.ts`: add 401 redirect to `/login` after login route exists
- [x] 5.4 Update `app.routes.ts`: add `/login` route with `LoginComponent`, apply `authGuard` to dashboard
- [x] 5.5 Update existing header/dashboard specs for role-driven affordances and logout

### Files Changed in Final Dashboard Integration Batch

| File | Action | What Was Done |
|------|--------|---------------|
| `frontend-angular/src/app/dashboard/dashboard.component.ts` | Modified | Injected `AuthService`, projected `user$` into `currentUsername` and role-driven `canManage`, passed auth affordances to header and asset cards, and delegated logout through `onLogout()`. |
| `frontend-angular/src/app/dashboard/header.component.ts` | Modified | Added `username` input and `logout` output, rendered current user and logout button, kept add button role-gated by `canManage`, and added matching header action styles. |
| `frontend-angular/src/app/dashboard/header.component.spec.ts` | Modified | Added coverage for username rendering, absent-user hiding, and logout emission. |
| `frontend-angular/src/app/dashboard/dashboard.component.spec.ts` | Modified | Added `AuthService` mock stream and coverage for username projection, ADMIN management affordances, VIEWER read-only affordances, and logout delegation. |

### TDD Cycle Evidence — Final Dashboard Integration Batch

| Task | Test File | Layer | Safety Net | RED | GREEN | REFACTOR |
|------|-----------|-------|------------|-----|-------|----------|
| 5.1 | `dashboard.component.spec.ts` | Component | Existing dashboard specs + new auth projection specs | ⚠️ Mentored flow implemented TypeScript first, then added focused specs in same batch | ✅ Current suite: 17/17 | Updated stale dashboard auth-affordance comment and signal binding consistency |
| 5.2 | `header.component.spec.ts` | Component | Existing header specs + new username/logout specs | ⚠️ Mentored flow implemented template/input/output first, then added focused specs in same batch | ✅ Current suite: 9/9 | Kept header presentational; added action grouping styles |
| 5.5 | `header.component.spec.ts`, `dashboard.component.spec.ts` | Component | Existing dashboard/header behavior | ✅ Specs added for role-driven affordances and logout | ✅ Focused dashboard/header specs passed | None |

### Focused Test Execution

All frontend tests below were run from `frontend-angular/`:

```
npm test -- --include=src/app/dashboard/header.component.spec.ts     # Current total: 9 SUCCESS
npm test -- --include=src/app/dashboard/dashboard.component.spec.ts  # Current total: 17 SUCCESS
```

### Final Verification and Gate Corrections

The final Strict-TDD review removed `core/models.spec.ts` because its three
runtime assertions only inspected literals created by the tests. TypeScript
interfaces are erased at runtime; their contract is instead validated by the
production build and by the behavioral tests that consume them. A redundant
header component truthiness smoke test was also removed.

The final review also corrected four auth edge cases:

- A late 401 from an old-token or no-token request no longer clears a newer authenticated session.
- Login distinguishes invalid credentials from infrastructure failures and cancels in-flight requests when the component is destroyed.
- VIEWER users receive a read-only empty state instead of instructions for a hidden management action.
- Development builds bypass the route guard and expose management controls so `npm run dev` remains usable with the auth-free backend dev profile; production builds remain guarded and role-based.

Final verification from `frontend-angular/`:

```
npm test  # TOTAL: 143 SUCCESS
npm test -- --include=src/app/core/auth.service.spec.ts --include=src/app/core/auth.interceptor.spec.ts --include=src/app/core/auth.guard.spec.ts --include=src/app/login/login.component.spec.ts --include=src/app/app.routes.spec.ts --include=src/app/dashboard/header.component.spec.ts --include=src/app/dashboard/dashboard.component.spec.ts
# => TOTAL: 66 SUCCESS
npm run build  # production build succeeds
npm run build -- --configuration development  # development build succeeds
```

## Status

15/15 tasks complete. Ready for `sdd-verify` / full verification of the
`angular-auth-ui` change before archive or PR finalization.
