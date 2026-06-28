import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { metricThresholdColor, ThresholdBand } from './threshold-color';

/**
 * Pure presentational metric gauge. Renders a label, the numeric percentage,
 * and a fill bar whose color is driven by the threshold band derived from the
 * value via the `metricThresholdColor` pure function. The active band is
 * exposed on `data-threshold` so theme styles apply the matching color and the
 * behavior is observable without coupling tests to CSS class names.
 */
@Component({
  selector: 'app-metric-gauge',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="metric-gauge" [attr.data-threshold]="threshold()" data-testid="metric-gauge">
      <div class="metric-gauge__head">
        <span class="metric-gauge__label" data-testid="gauge-label">{{ label() }}</span>
        <span class="metric-gauge__value" data-testid="gauge-value">{{ value() }}%</span>
      </div>
      <div class="metric-gauge__track">
        <div
          class="metric-gauge__fill"
          data-testid="gauge-fill"
          [style.width.%]="clampedWidth()"
        ></div>
      </div>
    </div>
  `,
  styles: [
    `
      .metric-gauge {
        display: flex;
        flex-direction: column;
        gap: var(--spacing-xs);
      }
      .metric-gauge__head {
        display: flex;
        justify-content: space-between;
        align-items: center;
      }
      .metric-gauge__label {
        font-family: var(--font-mono);
        font-size: 0.75rem;
        color: var(--text-secondary);
      }
      .metric-gauge__value {
        font-family: var(--font-mono);
        font-size: 0.875rem;
        font-weight: 700;
        color: var(--text-primary);
      }
      .metric-gauge__track {
        width: 100%;
        height: 4px;
        border-radius: 9999px;
        background: var(--border-strong);
        overflow: hidden;
      }
      .metric-gauge__fill {
        height: 100%;
        border-radius: 9999px;
        transition: width 0.3s ease;
      }
      .metric-gauge[data-threshold='ok'] .metric-gauge__fill {
        background: var(--metric-ok);
      }
      .metric-gauge[data-threshold='warning'] .metric-gauge__fill {
        background: var(--metric-warning);
      }
      .metric-gauge[data-threshold='critical'] .metric-gauge__fill {
        background: var(--metric-critical);
      }
    `,
  ],
})
export class MetricGaugeComponent {
  readonly label = input.required<string>();
  readonly value = input.required<number>();

  readonly clampedWidth = computed<number>(() => Math.min(100, Math.max(0, this.value())));
  readonly threshold = computed<ThresholdBand>(() => metricThresholdColor(this.value()));
}
