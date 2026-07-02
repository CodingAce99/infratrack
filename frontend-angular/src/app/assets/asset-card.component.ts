import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  OnInit,
  OnDestroy,
  output,
  signal,
} from '@angular/core';
import { Subscription } from 'rxjs';

import { MetricService } from '../core/metric.service';
import { Asset, MetricSnapshot } from '../core/models';
import { StatusBadgeComponent } from './status-badge.component';
import { MetricGaugeComponent } from '../metrics/metric-gauge.component';
import { SparklineComponent } from '../metrics/sparkline.component';
import { metricThresholdColor } from '../metrics/threshold-color';
import { EditAssetPanelComponent } from './edit-asset-panel.component';

/**
 * Asset card container. Owns the per-asset `MetricService.history$` subscription
 * (independent polling — one card never blocks another), composes the
 * presentational status badge, three metric gauges, and the sparkline, and
 * coordinates the single-edit-card invariant by emitting `requestEdit` /
 * `closeEdit` to the dashboard rather than toggling a global store.
 *
 * `isEditing` is a dashboard-owned input mirroring the shared `editingAssetId`;
 * the card never edits siblings directly. The edit panel is rendered only while
 * this card is the active editor.
 */
@Component({
  selector: 'app-asset-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    StatusBadgeComponent,
    MetricGaugeComponent,
    SparklineComponent,
    EditAssetPanelComponent,
  ],
  template: `
    <article
      class="asset-card"
      [attr.data-status]="asset().status"
      data-testid="asset-card"
    >
      <header class="asset-card__head">
        <div class="asset-card__title">
          <h3 class="asset-card__name" data-testid="asset-name">{{ asset().name }}</h3>
          <span class="asset-card__ip">{{ asset().ipAddress }}</span>
        </div>
        <app-status-badge [status]="asset().status" />
      </header>

      <section class="asset-card__metrics">
        <div class="asset-card__metric">
          <app-metric-gauge label="CPU" [value]="cpu()" />
          <app-sparkline [data]="cpuSeries()" [color]="cpuColor()" />
        </div>
        <div class="asset-card__metric">
          <app-metric-gauge label="Memory" [value]="memory()" />
          <app-sparkline [data]="memorySeries()" [color]="memoryColor()" />
        </div>
        <div class="asset-card__metric">
          <app-metric-gauge label="Disk" [value]="disk()" />
          <app-sparkline [data]="diskSeries()" [color]="diskColor()" />
        </div>
      </section>

      <footer class="asset-card__foot">
        @if (canManage()) {
          @if (isEditing()) {
            <button
              type="button"
              class="asset-card__edit-cancel"
              data-testid="cancel-edit-button"
              (click)="closeEdit.emit()"
            >
              Cancel
            </button>
          } @else {
            <button
              type="button"
              class="asset-card__edit-open"
              data-testid="edit-button"
              (click)="requestEdit.emit()"
            >
              Edit
            </button>
          }
        }
      </footer>

      @if (isEditing()) {
        <app-edit-asset-panel
          [asset]="asset()"
          (updated)="closeEdit.emit()"
          (deleted)="closeEdit.emit()"
          (cancel)="closeEdit.emit()"
        />
      }
    </article>
  `,
  styles: [
    `
      .asset-card {
        background: var(--bg-card);
        border: 1px solid var(--border-subtle);
        border-left: 3px solid var(--status-inactive);
        border-radius: var(--radius);
        padding: var(--spacing-md);
        display: flex;
        flex-direction: column;
        gap: var(--spacing-md);
      }
      .asset-card[data-status='ACTIVE'] {
        border-left-color: var(--status-active);
      }
      .asset-card[data-status='MAINTENANCE'] {
        border-left-color: var(--status-maintenance);
      }
      .asset-card[data-status='INACTIVE'] {
        border-left-color: var(--status-inactive);
      }
      .asset-card__head {
        display: flex;
        align-items: flex-start;
        justify-content: space-between;
        gap: var(--spacing-sm);
      }
      .asset-card__title {
        display: flex;
        flex-direction: column;
        gap: 2px;
      }
      .asset-card__name {
        margin: 0;
        font-size: 0.95rem;
        color: var(--text-primary);
        font-family: var(--font-mono);
      }
      .asset-card__ip {
        font-family: var(--font-mono);
        font-size: 0.75rem;
        color: var(--text-muted);
      }
      .asset-card__metrics {
        display: flex;
        flex-direction: column;
        gap: var(--spacing-sm);
      }
      .asset-card__metric {
        display: flex;
        flex-direction: column;
        gap: var(--spacing-xs);
      }
      .asset-card__foot {
        display: flex;
        justify-content: flex-end;
      }
      .asset-card__edit-open {
        background: transparent;
        border: 1px solid var(--border-strong);
        color: var(--text-secondary);
        border-radius: var(--radius-sm);
        font-size: 0.8125rem;
        padding: 0.25rem 0.75rem;
      }
      .asset-card__edit-open:hover {
        color: var(--text-primary);
        border-color: var(--accent);
      }
      .asset-card__edit-cancel {
        background: transparent;
        border: none;
        color: var(--text-secondary);
        font-size: 0.8125rem;
      }
    `,
  ],
})
export class AssetCardComponent implements OnInit, OnDestroy {
  readonly asset = input.required<Asset>();
  readonly isEditing = input<boolean>(false);
  readonly canManage = input<boolean>(true);
  readonly requestEdit = output<void>();
  readonly closeEdit = output<void>();

  private readonly metricService = inject(MetricService);

  private readonly latest = signal<MetricSnapshot[]>([]);
  private subscription?: Subscription;

  readonly cpu = computed(() => this.latest().at(-1)?.cpuUsage ?? 0);
  readonly memory = computed(() => this.latest().at(-1)?.memoryUsage ?? 0);
  readonly disk = computed(() => this.latest().at(-1)?.diskUsage ?? 0);

  readonly cpuSeries = computed(() => this.latest().map((s) => s.cpuUsage));
  readonly memorySeries = computed(() => this.latest().map((s) => s.memoryUsage));
  readonly diskSeries = computed(() => this.latest().map((s) => s.diskUsage));

  readonly cpuColor = computed(() => this.colorFor(this.cpu()));
  readonly memoryColor = computed(() => this.colorFor(this.memory()));
  readonly diskColor = computed(() => this.colorFor(this.disk()));

  ngOnInit(): void {
    // Subscribe here so the required `asset` input is available (signal inputs
    // are not readable in the constructor — see Angular error NG0950).
    this.subscription = this.metricService
      .history$(this.asset().id)
      .subscribe((snapshots) => this.latest.set(snapshots ?? []));
  }

  private colorFor(value: number): string {
    const band = metricThresholdColor(value);
    const map: Record<string, string> = {
      ok: 'var(--metric-ok)',
      warning: 'var(--metric-warning)',
      critical: 'var(--metric-critical)',
    };
    return map[band] ?? 'var(--metric-ok)';
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }
}