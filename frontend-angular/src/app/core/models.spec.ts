import { AuthUser, LoginRequest, LoginResponse, UserRole } from './models';

/**
 * Auth session contracts added for the Angular Auth UI change.
 * These tests pin the shapes consumed by `AuthService` and `authInterceptor`
 * so the REST contract is documented at the type level.
 */
describe('Auth session models', () => {
  it('exposes a LoginRequest with username and password', () => {
    const payload: LoginRequest = { username: 'admin', password: 'secret' };

    expect(payload.username).toBe('admin');
    expect(payload.password).toBe('secret');
  });

  it('exposes an AuthUser carrying username and role', () => {
    const user: AuthUser = { username: 'admin', role: 'ADMIN' };

    expect(user.username).toBe('admin');
    expect(user.role).toBe('ADMIN');
  });

  it('exposes a LoginResponse mirroring the backend auth contract', () => {
    const admin: LoginResponse = {
      token: 'token-abc',
      type: 'Bearer',
      username: 'admin',
      role: 'ADMIN',
    };
    const viewer: LoginResponse = { ...admin, role: 'VIEWER' as UserRole };

    expect(admin.type).toBe('Bearer');
    expect(admin.token).toBe('token-abc');
    expect(viewer.role).toBe('VIEWER');
  });
});