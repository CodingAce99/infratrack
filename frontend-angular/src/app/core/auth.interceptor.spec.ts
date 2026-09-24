import { TestBed } from '@angular/core/testing';
import {
  HttpClient,
  HttpErrorResponse,
  provideHttpClient,
  withInterceptors,
} from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';

import { authInterceptor } from './auth.interceptor';
import { AuthService } from './auth.service';

/**
 * `authInterceptor` coverage for PR 1 (Auth Core):
 *  - Bearer header injection on `/api/*` requests when a token exists.
 *  - No header when no token exists.
 *  - No header for non-`/api/*` requests (token never leaks off-origin).
 *  - 401 from a protected `/api/*` request clears the session via logout().
 *  - 401 from the login request itself does NOT clear state (form-level error).
 *  - 403 does NOT clear the session (authorization failure, not auth failure).
 *
 * PR 1 constraint: the interceptor MUST NOT redirect to `/login` here. The
 * `/login` route does not exist yet (added in PR 2); redirect lives in PR 2's
 * task 5.3. These tests therefore assert only session-clearing behavior.
 */
describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authSpy: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    authSpy = jasmine.createSpyObj<AuthService>('AuthService', [
      'getToken',
      'logout',
    ]);

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authSpy },
      ],
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('adds Authorization: Bearer <token> to GET /api/* requests when a token is stored', () => {
    authSpy.getToken.and.returnValue('jwt-123');

    http.get('/api/v1/assets').subscribe();

    const req = httpMock.expectOne('/api/v1/assets');
    expect(req.request.headers.get('Authorization')).toBe('Bearer jwt-123');
    req.flush([]);
  });

  it('adds the Bearer header to PUT /api/* requests (triangulation across methods/paths)', () => {
    authSpy.getToken.and.returnValue('jwt-abc');

    http
      .put('/api/v1/assets/asset-1/status', { status: 'MAINTENANCE' })
      .subscribe();

    const req = httpMock.expectOne('/api/v1/assets/asset-1/status');
    expect(req.request.method).toBe('PUT');
    expect(req.request.headers.get('Authorization')).toBe('Bearer jwt-abc');
    req.flush({});
  });

  it('does not add an Authorization header when no token is stored', () => {
    authSpy.getToken.and.returnValue(null);

    http.get('/api/v1/assets').subscribe();

    const req = httpMock.expectOne('/api/v1/assets');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush([]);
  });

  it('does not attach a Bearer header to non-/api/* requests even when a token exists', () => {
    authSpy.getToken.and.returnValue('jwt-123');

    http.get('/assets/elsewhere').subscribe();

    const req = httpMock.expectOne('/assets/elsewhere');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush([]);
  });

  it('clears the session (logout) when a protected /api/* request returns 401', () => {
    authSpy.getToken.and.returnValue('jwt-123');

    http.get('/api/v1/assets').subscribe({
      next: () => fail('expected the 401 response to error'),
      error: (err: HttpErrorResponse) => expect(err.status).toBe(401),
    });

    const req = httpMock.expectOne('/api/v1/assets');
    req.flush({ error: 'Unauthorized' }, { status: 401, statusText: 'Unauthorized' });

    expect(authSpy.logout).toHaveBeenCalledTimes(1);
  });

  it('does NOT clear the session when the login request itself returns 401', () => {
    authSpy.getToken.and.returnValue(null);

    http
      .post('/api/v1/auth/login', { username: 'admin', password: 'secret' })
      .subscribe({
        next: () => fail('expected the 401 login response to error'),
        error: (err: HttpErrorResponse) => expect(err.status).toBe(401),
      });

    const req = httpMock.expectOne('/api/v1/auth/login');
    req.flush(
      { error: 'Invalid credentials' },
      { status: 401, statusText: 'Unauthorized' },
    );

    expect(authSpy.logout).not.toHaveBeenCalled();
  });

  it('does NOT clear the session and surfaces the error when a protected request returns 403', () => {
    authSpy.getToken.and.returnValue('jwt-123');

    http.post('/api/v1/assets', { name: 'x' }).subscribe({
      next: () => fail('expected the 403 response to error'),
      error: (err: HttpErrorResponse) => expect(err.status).toBe(403),
    });

    const req = httpMock.expectOne('/api/v1/assets');
    req.flush({ error: 'Forbidden' }, { status: 403, statusText: 'Forbidden' });

    expect(authSpy.logout).not.toHaveBeenCalled();
  });
});