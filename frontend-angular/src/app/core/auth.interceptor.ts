import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { AuthService } from './auth.service';

/**
 * Login endpoint. 401 from this URL is a form-level invalid-credentials
 * error and MUST NOT trigger protected-session invalidation — only the
 * `LoginComponent` decides what a login 401 means (inline error, stay on
 * `/login`).
 */
const LOGIN_URL = '/api/v1/auth/login';

/**
 * Functional HTTP interceptor for the auth session.
 *
 * Responsibilities (PR 1 — Auth Core):
 *  - Attach `Authorization: Bearer <token>` to `/api/*` requests ONLY when a
 *    token exists. Non-`/api/*` requests are left untouched so the token can
 *    never leak to off-origin resources.
 *  - On a 401 from a protected `/api/*` request (anything except the login
 *    endpoint), clear the session via `AuthService.logout()`. The interceptor
 *    does NOT redirect here: `/login` does not exist yet in PR 1. Task 5.3
 *    (PR 2) adds the redirect once the `/login` route lands.
 *  - On a 403, surface the error but PRESERVE the session — 403 means the
 *    identity is valid but lacks permission, which is not an auth failure.
 *
 * The login 401 special case keeps the form-error path local to
 * `LoginComponent`; without it, the interceptor would silently log users out
 * of a session they never had on every bad password attempt.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.getToken();

  let outgoing = req;
  if (token && req.url.startsWith('/api/')) {
    outgoing = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` },
    });
  }

  return next(outgoing).pipe(
    catchError((err: HttpErrorResponse) => {
      if (
        err.status === 401 &&
        req.url.startsWith('/api/') &&
        req.url !== LOGIN_URL
      ) {
        auth.logout();
      }
      return throwError(() => err);
    }),
  );
};