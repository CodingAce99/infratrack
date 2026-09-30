import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

import { AuthUser, LoginRequest, LoginResponse, UserRole } from './models';

/**
 * localStorage key under which the JWT access token is persisted.
 *
 * The token is stored in `localStorage` intentionally for this MVP. This is
 * vulnerable to XSS exfiltration and is accepted here only because there is
 * no backend cookie/refresh-token design yet; the backend remains the
 * authority for every API request. A refresh-token/CSP-based design should
 * replace it later.
 */
export const AUTH_TOKEN_KEY = 'infratrack.auth.token';

const LOGIN_URL = '/api/v1/auth/login';
const VALID_ROLES: readonly UserRole[] = ['ADMIN', 'VIEWER'];

/**
 * Reconstructs an {@link AuthUser} from the payload section of a JWT.
 *
 * The frontend uses the decoded claims ONLY to restore UI state after a
 * page reload; the backend still validates every API request. Null/empty,
 * malformed, unparseable, missing-`sub`, unknown-`role`, or expired-looking
 * tokens resolve to `null` so callers can treat the result as "no usable
 * local session".
 *
 * Expiry handling: when an `exp` claim is present it MUST be a finite number
 * of seconds since the Unix epoch that lies strictly in the future; otherwise
 * the token is treated as expired/invalid and `null` is returned. A missing
 * `exp` claim does not by itself invalidate the token (login response tokens
 * are not re-decoded here; missing-exp restore is allowed to preserve the
 * lenient MVP contract while clearly expired tokens are never trusted).
 */
export function decodeAuthUser(token: string | null): AuthUser | null {
  if (!token) {
    return null;
  }

  const segments = token.split('.');
  if (segments.length < 3) {
    return null;
  }

  let payload: unknown;
  try {
    payload = JSON.parse(base64UrlDecode(segments[1]));
  } catch {
    return null;
  }

  if (!payload || typeof payload !== 'object') {
    return null;
  }

  const record = payload as Record<string, unknown>;
  const sub = record['sub'];
  const role = record['role'];
  const exp = record['exp'];

  if (typeof sub !== 'string' || sub.length === 0) {
    return null;
  }
  if (typeof role !== 'string' || !VALID_ROLES.includes(role as UserRole)) {
    return null;
  }
  if (exp !== undefined && !isFutureUnixSeconds(exp)) {
    return null;
  }

  return { username: sub, role: role as UserRole };
}

/**
 * Returns `true` only when `exp` is a finite numeric value expressed in
 * whole seconds since the Unix epoch that lies strictly in the future.
 * Accepts numbers and numeric strings; rejects anything else, anything that
 * cannot represent a valid expiry timestamp, and any value that has already
 * elapsed (inclusive of the exact expiry instant).
 */
function isFutureUnixSeconds(exp: unknown): boolean {
  const seconds =
    typeof exp === 'number'
      ? exp
      : typeof exp === 'string' && exp.trim() !== ''
        ? Number(exp)
        : NaN;
  if (!Number.isFinite(seconds)) {
    return false;
  }
  // Use seconds resolution: compare against the current wall-clock time so
  // an `exp` exactly equal to "now" is already expired (JWT exp is "not on
  // or after", i.e. valid only while strictly greater than the issued time).
  return seconds > Math.floor(Date.now() / 1000);
}

/**
 * Decodes a Base64Url JWT segment into a UTF-8 string.
 * Pads to a multiple of 4 and converts URL-safe alphabet back to Base64.
 */
function base64UrlDecode(segment: string): string {
  const base64 = segment.replace(/-/g, '+').replace(/_/g, '/');
  const padded =
    base64 + '='.repeat((4 - (base64.length % 4)) % 4);
  const binary = atob(padded);
  try {
    return decodeURIComponent(escape(binary));
  } catch {
    return binary;
  }
}

/**
 * Owns the frontend auth session, mirroring the RxJS state pattern of
 * {@link AssetService} and {@link MetricService}.
 *
 * - Logs in through `POST /api/v1/auth/login`.
 * - Persists the returned access token in `localStorage`.
 * - Exposes `user$`, `role$`, and `isAuthenticated$` as replayed streams.
 * - Rehydrates the session on construction by decoding any stored token.
 * - Logout clears both the persisted token and the in-memory state.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly userSubject = new BehaviorSubject<AuthUser | null>(null);
  private readonly roleSubject = new BehaviorSubject<UserRole | null>(null);
  private readonly authenticatedSubject = new BehaviorSubject<boolean>(false);

  private currentToken: string | null = null;

  /** Authenticated principal, or `null` when no session is active. */
  readonly user$: Observable<AuthUser | null> = this.userSubject.asObservable();
  /** Current role, or `null` when no session is active. */
  readonly role$: Observable<UserRole | null> = this.roleSubject.asObservable();
  /** True while a valid local session exists. */
  readonly isAuthenticated$: Observable<boolean> =
    this.authenticatedSubject.asObservable();

  constructor() {
    this.restore();
  }

  /**
   * Authenticate with the login API and persist the returned token.
   * Failures propagate as the original HttpErrorResponse; the session stays
   * unauthenticated on error.
   */
  login(req: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(LOGIN_URL, req).pipe(
      tap((res) => this.setSession(res)),
    );
  }

  /** Clear all session state and the persisted token. */
  logout(): void {
    localStorage.removeItem(AUTH_TOKEN_KEY);
    this.currentToken = null;
    this.userSubject.next(null);
    this.roleSubject.next(null);
    this.authenticatedSubject.next(false);
  }

  /** Current access token for the HTTP interceptor, or `null` when unauthenticated. */
  getToken(): string | null {
    return this.currentToken;
  }

  private setSession(res: LoginResponse): void {
    localStorage.setItem(AUTH_TOKEN_KEY, res.token);
    this.currentToken = res.token;
    this.userSubject.next({ username: res.username, role: res.role });
    this.roleSubject.next(res.role);
    this.authenticatedSubject.next(true);
  }

  private restore(): void {
    const stored = localStorage.getItem(AUTH_TOKEN_KEY);
    const user = decodeAuthUser(stored);

    if (!stored || !user) {
      // Drop any stale/invalid token so it is never silently reused.
      if (stored) {
        localStorage.removeItem(AUTH_TOKEN_KEY);
      }
      this.currentToken = null;
      this.userSubject.next(null);
      this.roleSubject.next(null);
      this.authenticatedSubject.next(false);
      return;
    }

    this.currentToken = stored;
    this.userSubject.next(user);
    this.roleSubject.next(user.role);
    this.authenticatedSubject.next(true);
  }
}