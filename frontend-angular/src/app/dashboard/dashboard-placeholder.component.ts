import { ChangeDetectionStrategy, Component } from '@angular/core';

/**
 * Temporary placeholder rendered at `/` until Phase 3 wires the real
 * `DashboardComponent`. Keeps the route map valid for this foundation slice.
 */
@Component({
  selector: 'app-dashboard-placeholder',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: '<p class="dashboard-placeholder">Dashboard coming in a later slice.</p>',
  styles: [
    `
      .dashboard-placeholder {
        color: var(--text-secondary);
        font-family: var(--font-mono);
      }
    `,
  ],
})
export class DashboardPlaceholderComponent {}