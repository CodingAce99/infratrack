import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs/operators';

import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = () => {
  if (!environment.authRequired) {
    return true;
  }

  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.isAuthenticated$.pipe(
    map((isAuthenticated) =>
      isAuthenticated ? true : router.parseUrl('/login'),
    ),
  );
};
