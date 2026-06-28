import { Routes } from '@angular/router';

/**
 * Top-level routes. Phase 1 wires a lazy dashboard placeholder at `/`.
 * Subsequent slices replace the placeholder with the real DashboardComponent.
 */
export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./dashboard/dashboard-placeholder.component').then(
        (m) => m.DashboardPlaceholderComponent,
      ),
    title: 'Infratrack Dashboard',
  },
  { path: '**', redirectTo: '' },
];