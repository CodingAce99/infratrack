import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

/**
 * Dashboard header. Presentational coordination component showing the active
 * asset count, API connection indicator, current user, logout trigger, and
 * role-gated "+ Add Asset" trigger.
 *
 * The connection indicator exposes a `data-connected` attribute (`true`/`false`)
 * so the theme can apply the connected (green) / disconnected (gray) dot color
 * without tests coupling to CSS class names. Auth state is received through
 * inputs so the header stays presentational; it never reads `AuthService`
 * directly.
 */
@Component({
  selector: 'app-header',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="header" data-testid="dashboard-header">
      <div class="header__left">
        <span
          class="header__connection"
          data-testid="connection-indicator"
          [attr.data-connected]="isConnected()"
          [attr.aria-label]="isConnected() ? 'API connected' : 'API disconnected'"
        ></span>
        <span class="header__count" data-testid="asset-count">{{ assetCount() }} assets</span>
      </div>
      <div class="header__actions">
        @if (username()) {
          <span class="header__user" data-testid="current-user">
            {{ username() }}
          </span>
          <button
            type="button"
            class="header__logout"
            data-testid="logout-button"
            (click)="logout.emit()"
          >
            Logout
          </button>
        }

        @if (canManage()) {
          <button
            type="button"
            class="header__add"
            data-testid="add-asset-button"
            (click)="addAsset.emit()"
          >
            + Add Asset
          </button>
        }
      </div>
    </header>
  `,
  styles: [
    `
      .header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: var(--spacing-md) 0;
        gap: var(--spacing-md);
      }
      .header__left {
        display: flex;
        align-items: center;
        gap: var(--spacing-sm);
      }
      .header__actions {
        display: flex;
        align-items: center;
        gap: var(--spacing-sm);
        flex-wrap: wrap;
        justify-content: flex-end;
      }
      .header__connection {
        display: inline-block;
        width: 10px;
        height: 10px;
        border-radius: 9999px;
      }
      .header__connection[data-connected='true'] {
        background: var(--status-active);
      }
      .header__connection[data-connected='false'] {
        background: var(--status-inactive);
      }
      .header__count {
        font-family: var(--font-mono);
        font-size: 0.875rem;
        color: var(--text-secondary);
      }
      .header__user {
        color: var(--text-secondary);
        font-family: var(--font-mono);
        font-size: 0.8125rem;
      }
      .header__logout {
        background: transparent;
        color: var(--text-secondary);
        border: 1px solid var(--border);
        border-radius: var(--radius-sm);
        font-size: 0.8125rem;
        font-weight: 600;
        padding: 0.5rem 0.875rem;
      }
      .header__logout:hover {
        color: var(--text-primary);
        border-color: var(--text-secondary);
      }
      .header__add {
        background: var(--accent);
        color: #001018;
        border: none;
        border-radius: var(--radius-sm);
        font-size: 0.8125rem;
        font-weight: 600;
        padding: 0.5rem 0.875rem;
      }
      .header__add:hover {
        background: var(--accent-hover);
      }
    `,
  ],
})
export class HeaderComponent {
  readonly assetCount = input.required<number>();
  readonly isConnected = input<boolean>(false);
  readonly canManage = input<boolean>(false);
  readonly addAsset = output<void>();
  readonly username = input<string | null>(null);
  readonly logout = output<void>();
}
