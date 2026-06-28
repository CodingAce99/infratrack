import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';

/**
 * Application providers for the production runtime.
 *
 * HTTP testing is configured per test via `TestBed` (`provideHttpClientTesting()`),
 * so it is intentionally omitted here to keep production providers test-free.
 */
export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(),
  ],
};