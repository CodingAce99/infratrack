import { Routes } from '@angular/router';

import { DashboardComponent } from './dashboard/dashboard.component';

/**
 * Top-level routes. Phase 3 renders the real `DashboardComponent` (the
 * operations dashboard composition root) directly at `/`. The placeholder is
 * no longer referenced. Phase 4 owns Docker/CI cutover.
 */
export const routes: Routes = [
  {
    path: '',
    component: DashboardComponent,
    title: 'Infratrack Dashboard',
  },
  { path: '**', redirectTo: '' },
];