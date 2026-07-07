import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { LoginComponent } from './login/login.component';

import { DashboardComponent } from './dashboard/dashboard.component';

/**
 * Top-level routes. Phase 3 renders the real `DashboardComponent` (the
 * operations dashboard composition root) directly at `/`. The placeholder is
 * no longer referenced. Phase 4 owns Docker/CI cutover.
 */
export const routes: Routes = [
  {
    path: 'login',
    component: LoginComponent,
    title: 'Sign in | Infratrack',
  },
  {
    path: '',
    component: DashboardComponent,
    canActivate: [authGuard],
    title: 'Infratrack Dashboard',
  },
  { path: '**', redirectTo: '' },
];