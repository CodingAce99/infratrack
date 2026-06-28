import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { AssetStatus } from '../core/models';

/** Status label lookup so the template never hardcodes display strings. */
const STATUS_LABELS: Readonly<Record<AssetStatus, string>> = {
  ACTIVE: 'Active',
  MAINTENANCE: 'Maintenance',
  INACTIVE: 'Inactive',
};

/**
 * Pure presentational status pill. Receives an `AssetStatus` input and renders
 * the matching label plus a `data-status` attribute that the theme styles use
 * to apply the status color. Has no API access and no service dependencies.
 */
@Component({
  selector: 'app-status-badge',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span class="status-badge" [attr.data-status]="status()" data-testid="status-badge">
      ● {{ label() }}
    </span>
  `,
  styles: [
    `
      .status-badge {
        display: inline-flex;
        align-items: center;
        gap: var(--spacing-xs);
        font-family: var(--font-mono);
        font-size: 0.75rem;
        font-weight: 500;
        padding: 0.125rem 0.5rem;
        border-radius: 9999px;
      }
      .status-badge[data-status='ACTIVE'] {
        background: color-mix(in srgb, var(--status-active) 12%, transparent);
        color: var(--status-active);
      }
      .status-badge[data-status='MAINTENANCE'] {
        background: color-mix(in srgb, var(--status-maintenance) 12%, transparent);
        color: var(--status-maintenance);
      }
      .status-badge[data-status='INACTIVE'] {
        background: color-mix(in srgb, var(--status-inactive) 12%, transparent);
        color: var(--status-inactive);
      }
    `,
  ],
})
export class StatusBadgeComponent {
  readonly status = input.required<AssetStatus>();

  label(): string {
    return STATUS_LABELS[this.status()];
  }
}
