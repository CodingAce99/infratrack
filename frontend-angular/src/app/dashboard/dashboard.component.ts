import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnDestroy,
  OnInit,
  signal,
} from '@angular/core';
import { Router } from '@angular/router';
import { Subscription } from 'rxjs';

import { environment } from '../../environments/environment';
import { ApiError } from '../core/api-error';
import { AssetService } from '../core/asset.service';
import { AuthService } from '../core/auth.service';
import { Asset, AuthUser } from '../core/models';
import { HeaderComponent } from './header.component';
import { AssetCardComponent } from '../assets/asset-card.component';
import { CreateAssetModalComponent } from '../assets/create-asset-modal.component';

/** Asset-list refresh cadence (milliseconds). Interaction-safe: suppressed
 * while a modal is open or a card is editing. */
const REFRESH_INTERVAL_MS = 60_000;

export function canManageAssets(
  user: AuthUser | null,
  authRequired: boolean,
): boolean {
  return !authRequired || user?.role === 'ADMIN';
}

/**
 * Composition root for the operations dashboard.
 *
 * Subscribes to the shared `AssetService` asset-list and owns the dashboard
 * coordination state: the connection indicator (true only after the latest
 * API asset-list check succeeds), the modal visibility, the single-edit-card
 * `editingAssetId`, and a visible confirmation/error surface. A 60-second
 * `setInterval` revalidates the asset list but is gated by an
 * interaction-active check so it never closes an open modal/panel or
 * overwrites in-progress forms.
 *
 * Auth state is projected into dashboard affordances: ADMIN users can manage
 * assets, while VIEWER users keep read-only dashboard access.
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [HeaderComponent, AssetCardComponent, CreateAssetModalComponent],
  template: `
    <main class="dashboard">
      <app-header
        [assetCount]="assets().length"
        [isConnected]="isConnected()"
        [canManage]="canManage()"
        [username]="currentUsername()"
        (addAsset)="openCreateModal()"
        (logout)="onLogout()"
      />

      @if (confirmation()) {
        <p class="dashboard__confirmation" data-testid="confirmation" role="status">
          {{ confirmation() }}
        </p>
      }
      @if (dashboardError()) {
        <p class="dashboard__error" data-testid="dashboard-error" role="alert">
          {{ dashboardError() }}
        </p>
      }

      @if (assets().length === 0) {
        <p class="dashboard__empty" data-testid="empty-state">
          @if (canManage()) {
            No assets yet. Click "+ Add Asset" to register one.
          } @else {
            No assets are currently available.
          }
        </p>
      } @else {
        <section class="dashboard__grid">
          @for (asset of assets(); track asset.id) {
            <app-asset-card
              [asset]="asset"
              [isEditing]="asset.id === editingAssetId()"
              [canManage]="canManage()"
              (requestEdit)="onRequestEdit(asset.id)"
              (closeEdit)="onCloseEdit(asset.id)"
            />
          }
        </section>
      }

      <app-create-asset-modal
        [isOpen]="isCreateModalOpen()"
        (close)="closeCreateModal()"
        (created)="onCreated($event)"
      />
    </main>
  `,
  styles: [
    `
      .dashboard {
        max-width: 64rem;
        margin: 0 auto;
        padding: 0 var(--spacing-md) var(--spacing-xl);
        display: flex;
        flex-direction: column;
        gap: var(--spacing-md);
      }
      .dashboard__confirmation {
        margin: 0;
        padding: var(--spacing-sm) var(--spacing-md);
        background: color-mix(in srgb, var(--status-active) 12%, transparent);
        color: var(--status-active);
        border-radius: var(--radius-sm);
        font-size: 0.85rem;
      }
      .dashboard__error {
        margin: 0;
        padding: var(--spacing-sm) var(--spacing-md);
        background: color-mix(in srgb, var(--danger) 12%, transparent);
        color: var(--danger);
        border-radius: var(--radius-sm);
        font-size: 0.85rem;
      }
      .dashboard__empty {
        margin: 0;
        color: var(--text-secondary);
        font-family: var(--font-mono);
        font-size: 0.9rem;
      }
      .dashboard__grid {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(18rem, 1fr));
        gap: var(--spacing-md);
      }
    `,
  ],
})
export class DashboardComponent implements OnInit, OnDestroy {
  private readonly assetService = inject(AssetService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly assets = signal<Asset[]>([]);
  readonly isConnected = signal<boolean>(true);
  readonly isCreateModalOpen = signal<boolean>(false);
  readonly editingAssetId = signal<string | null>(null);
  readonly confirmation = signal<string | null>(null);
  readonly dashboardError = signal<string | null>(null);
  readonly currentUsername = signal<string | null>(null);
  readonly canManage = signal<boolean>(false);

  private readonly subscriptions: Subscription = new Subscription();
  private intervalId?: ReturnType<typeof setInterval>;

  ngOnInit(): void {
    this.subscriptions.add(
      this.assetService.assets$.subscribe((list) => {
        this.assets.set(list ?? []);
        this.isConnected.set(true);
        this.dashboardError.set(null);
      }),
    );

    this.subscriptions.add(
      this.authService.user$.subscribe((user) => {
        this.canManage.set(canManageAssets(user, environment.authRequired));
        this.currentUsername.set(user?.username ?? null);
      }),
    );

    this.subscriptions.add(
      this.assetService.error$.subscribe((err: ApiError) => {
        this.isConnected.set(false);
        this.dashboardError.set(err.message);
      }),
    );

    this.assetService.refresh();
    this.intervalId = setInterval(() => this.maybeRefresh(), REFRESH_INTERVAL_MS);
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
  }

  openCreateModal(): void {
    this.isCreateModalOpen.set(true);
  }

  closeCreateModal(): void {
    this.isCreateModalOpen.set(false);
  }

  onCreated(message: string): void {
    this.isCreateModalOpen.set(false);
    this.confirmation.set(message);
  }

  /** Single-edit invariant: opening one card clears any previous editor. */
  onRequestEdit(assetId: string): void {
    this.editingAssetId.set(assetId);
  }

  onCloseEdit(_assetId: string): void {
    if (this.editingAssetId() === _assetId) {
      this.editingAssetId.set(null);
    }
  }

  onLogout(): void {
    this.authService.logout();
    this.router.navigateByUrl('/login');
  }

  private isInteractionActive(): boolean {
    return this.isCreateModalOpen() || this.editingAssetId() !== null;
  }

  private maybeRefresh(): void {
    if (this.isInteractionActive()) {
      return;
    }
    this.assetService.refresh();
  }
}
