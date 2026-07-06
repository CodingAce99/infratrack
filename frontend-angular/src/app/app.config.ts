import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { authInterceptor } from './core/auth.interceptor';

/**
 * Application providers for the production runtime.
 *
 * HTTP testing is configured per test via `TestBed` (`provideHttpClientTesting()`),
 * so it is intentionally omitted here to keep production providers test-free.
 *
 * `authInterceptor` is registered globally so every `/api/*` request carries
 * the Bearer token and protegected-request 401s clear the session. The
 * interceptor itself decides whether to attach a header (only `/api/*` and
 * only when a token exists), so dev-profile (no token) stays auth-free.
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
  ],
};