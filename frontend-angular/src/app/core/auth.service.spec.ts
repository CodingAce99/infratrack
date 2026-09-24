import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import {
  provideHttpClientTesting,
  HttpTestingController,
} from '@angular/common/http/testing';
import { firstValueFrom } from 'rxjs';

import { AuthService, decodeAuthUser, AUTH_TOKEN_KEY } from './auth.service';
import { AuthUser, LoginRequest, LoginResponse, UserRole } from './models';

/**
 * Builds a fake JWT whose payload encodes the supplied claims.
 * Only the payload is meaningful for the frontend restore logic; the
 * header/signature are not validated client-side.
 */
function fakeJwt(payload: Record<string, unknown>): string {
  const header = { alg: 'HS256', typ: 'JWT' };
  const enc = (obj: unknown) =>
    btoa(JSON.stringify(obj)).replace(/=+$/, '').replace(/\+/g, '-').replace(/\//g, '_');
  return `${enc(header)}.${enc(payload)}.signature`;
}

describe('decodeAuthUser (pure JWT restore)', () => {
  it('returns null for null or empty tokens', () => {
    expect(decodeAuthUser(null)).toBeNull();
    expect(decodeAuthUser('')).toBeNull();
  });

  it('returns null for a malformed token with no payload segment', () => {
    expect(decodeAuthUser('not-a-jwt')).toBeNull();
    expect(decodeAuthUser('only.two')).toBeNull();
  });

  it('returns null when the payload cannot be parsed as JSON', () => {
    const enc = (raw: string) =>
      btoa(raw).replace(/=+$/, '').replace(/\+/g, '-').replace(/\//g, '_');
    expect(decodeAuthUser(`header.${enc('not-json')}.sig`)).toBeNull();
  });

  it('decodes a valid ADMIN token into an AuthUser', () => {
    const token = fakeJwt({ sub: 'admin', role: 'ADMIN' });
    expect(decodeAuthUser(token)).toEqual({
      username: 'admin',
      role: 'ADMIN',
    } as AuthUser);
  });

  it('decodes a valid VIEWER token into an AuthUser (triangulation)', () => {
    const token = fakeJwt({ sub: 'viewer', role: 'VIEWER' });
    expect(decodeAuthUser(token)).toEqual({
      username: 'viewer',
      role: 'VIEWER',
    } as AuthUser);
  });

  it('rejects a payload whose role is not a valid UserRole', () => {
    const token = fakeJwt({ sub: 'x', role: 'SUPERUSER' });
    expect(decodeAuthUser(token)).toBeNull();
  });

  it('rejects a payload missing the sub claim', () => {
    const token = fakeJwt({ role: 'ADMIN' });
    expect(decodeAuthUser(token)).toBeNull();
  });
});

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  const loginUrl = '/api/v1/auth/login';

  const adminLogin: LoginRequest = { username: 'admin', password: 'secret' };

  const adminResponse: LoginResponse = {
    token: fakeJwt({ sub: 'admin', role: 'ADMIN' }),
    type: 'Bearer',
    username: 'admin',
    role: 'ADMIN',
  };

  const viewerResponse: LoginResponse = {
    ...adminResponse,
    token: fakeJwt({ sub: 'viewer', role: 'VIEWER' }),
    username: 'viewer',
    role: 'VIEWER',
  };

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [AuthService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('starts unauthenticated with no user and no token', async () => {
    const user = await firstValueFrom(service.user$);
    const role = await firstValueFrom(service.role$);
    const authed = await firstValueFrom(service.isAuthenticated$);

    expect(user).toBeNull();
    expect(role).toBeNull();
    expect(authed).toBe(false);
    expect(service.getToken()).toBeNull();
    expect(localStorage.getItem(AUTH_TOKEN_KEY)).toBeNull();
  });

  it('persists the token and exposes user/role after a successful login', async () => {
    const promise = firstValueFrom(service.login(adminLogin));
    const req = httpMock.expectOne(loginUrl);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(adminLogin);
    req.flush(adminResponse);

    const result = await promise;
    expect(result.token).toBe(adminResponse.token);

    const user = await firstValueFrom(service.user$);
    const role = await firstValueFrom(service.role$);
    const authed = await firstValueFrom(service.isAuthenticated$);

    expect(user).toEqual({ username: 'admin', role: 'ADMIN' } as AuthUser);
    expect(role).toBe('ADMIN' as UserRole);
    expect(authed).toBe(true);
    expect(service.getToken()).toBe(adminResponse.token);
    expect(localStorage.getItem(AUTH_TOKEN_KEY)).toBe(adminResponse.token);
  });

  it('exposes the correct role for a VIEWER login (triangulation)', async () => {
    const promise = firstValueFrom(service.login({ username: 'viewer', password: 'pw' }));
    httpMock.expectOne(loginUrl).flush(viewerResponse);
    await promise;

    const role = await firstValueFrom(service.role$);
    const authed = await firstValueFrom(service.isAuthenticated$);

    expect(role).toBe('VIEWER' as UserRole);
    expect(authed).toBe(true);
  });

  it('leaves the session unauthenticated and propagates the error when login fails with 401', async () => {
    const promise = firstValueFrom(service.login(adminLogin)).catch((e: unknown) => e);
    const req = httpMock.expectOne(loginUrl);
    req.flush({ error: 'Invalid credentials' }, { status: 401, statusText: 'Unauthorized' });

    const caught = await promise;
    expect(caught).toBeTruthy();

    const user = await firstValueFrom(service.user$);
    const authed = await firstValueFrom(service.isAuthenticated$);
    expect(user).toBeNull();
    expect(authed).toBe(false);
    expect(service.getToken()).toBeNull();
    expect(localStorage.getItem(AUTH_TOKEN_KEY)).toBeNull();
  });

  it('clears the token and session state on logout after a login', async () => {
    firstValueFrom(service.login(adminLogin));
    httpMock.expectOne(loginUrl).flush(adminResponse);

    service.logout();

    const user = await firstValueFrom(service.user$);
    const role = await firstValueFrom(service.role$);
    const authed = await firstValueFrom(service.isAuthenticated$);

    expect(user).toBeNull();
    expect(role).toBeNull();
    expect(authed).toBe(false);
    expect(service.getToken()).toBeNull();
    expect(localStorage.getItem(AUTH_TOKEN_KEY)).toBeNull();
  });
});

describe('AuthService session restore from localStorage', () => {
  afterEach(() => localStorage.clear());

  it('restores the user and role from a valid token present at construction', async () => {
    const token = fakeJwt({ sub: 'restored-admin', role: 'ADMIN' });
    localStorage.setItem(AUTH_TOKEN_KEY, token);

    TestBed.configureTestingModule({
      providers: [AuthService, provideHttpClient(), provideHttpClientTesting()],
    });
    const fresh = TestBed.inject(AuthService);

    const user = await firstValueFrom(fresh.user$);
    const role = await firstValueFrom(fresh.role$);
    const authed = await firstValueFrom(fresh.isAuthenticated$);

    expect(user).toEqual({ username: 'restored-admin', role: 'ADMIN' } as AuthUser);
    expect(role).toBe('ADMIN' as UserRole);
    expect(authed).toBe(true);
    expect(fresh.getToken()).toBe(token);
  });

  it('stays unauthenticated when localStorage holds an invalid token', async () => {
    localStorage.setItem(AUTH_TOKEN_KEY, 'not-a-jwt');

    TestBed.configureTestingModule({
      providers: [AuthService, provideHttpClient(), provideHttpClientTesting()],
    });
    const fresh = TestBed.inject(AuthService);

    const user = await firstValueFrom(fresh.user$);
    const authed = await firstValueFrom(fresh.isAuthenticated$);

    expect(user).toBeNull();
    expect(authed).toBe(false);
  });
});

describe('decodeAuthUser expired JWT restore behavior', () => {
  const futureExp = Math.floor(Date.now() / 1000) + 3600;
  const pastExp = Math.floor(Date.now() / 1000) - 3600;

  it('rejects a token whose exp claim is in the past (expired)', () => {
    const token = fakeJwt({ sub: 'admin', role: 'ADMIN', exp: pastExp });
    expect(decodeAuthUser(token)).toBeNull();
  });

  it('rejects a token whose exp claim equals the current time (already elapsed)', () => {
    const now = Math.floor(Date.now() / 1000);
    const token = fakeJwt({ sub: 'admin', role: 'ADMIN', exp: now });
    expect(decodeAuthUser(token)).toBeNull();
  });

  it('rejects a token whose exp claim is not a finite number', () => {
    const malformed: [string, unknown][] = [
      ['non-numeric string', 'next-week'],
      ['boolean true', true],
      ['null', null],
      ['object', { value: 1 }],
      ['NaN string', 'NaN'],
      ['Infinity string', 'Infinity'],
    ];
    for (const [label, value] of malformed) {
      const token = fakeJwt({ sub: 'admin', role: 'ADMIN', exp: value });
      expect(decodeAuthUser(token)).toBeNull(
        `expected expired-looking exp (${label}) to be rejected`,
      );
    }
  });

  it('accepts a valid token whose exp claim is a future numeric timestamp', () => {
    const token = fakeJwt({ sub: 'admin', role: 'ADMIN', exp: futureExp });
    expect(decodeAuthUser(token)).toEqual({
      username: 'admin',
      role: 'ADMIN',
    } as AuthUser);
  });

  it('accepts a valid token whose exp claim is a numeric string in seconds', () => {
    const token = fakeJwt({
      sub: 'admin',
      role: 'ADMIN',
      exp: String(futureExp),
    });
    expect(decodeAuthUser(token)).toEqual({
      username: 'admin',
      role: 'ADMIN',
    } as AuthUser);
  });
});

describe('AuthService expired token restore', () => {
  afterEach(() => localStorage.clear());

  it('restores as unauthenticated and drops the stored token when localStorage holds an expired JWT', async () => {
    const expiredToken = fakeJwt({
      sub: 'admin',
      role: 'ADMIN',
      exp: Math.floor(Date.now() / 1000) - 60,
    });
    localStorage.setItem(AUTH_TOKEN_KEY, expiredToken);

    TestBed.configureTestingModule({
      providers: [AuthService, provideHttpClient(), provideHttpClientTesting()],
    });
    const fresh = TestBed.inject(AuthService);

    const user = await firstValueFrom(fresh.user$);
    const role = await firstValueFrom(fresh.role$);
    const authed = await firstValueFrom(fresh.isAuthenticated$);

    expect(user).toBeNull();
    expect(role).toBeNull();
    expect(authed).toBe(false);
    expect(fresh.getToken()).toBeNull();
    expect(localStorage.getItem(AUTH_TOKEN_KEY)).toBeNull();
  });
});