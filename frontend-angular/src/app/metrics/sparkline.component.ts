import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { buildSparklinePath } from './sparkline-path';

/** ViewBox dimensions for the sparkline SVG. */
const VIEW_WIDTH = 100;
const VIEW_HEIGHT = 32;

/**
 * Pure presentational sparkline. Renders a custom SVG path (no chart library)
 * from the `buildSparklinePath` pure function, and a stable empty state when
 * there are fewer than two data points. The line color is driven by the
 * `color` input so a parent gauge can pass its threshold color through.
 */
@Component({
  selector: 'app-sparkline',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (path(); as d) {
      <svg
        class="sparkline"
        [attr.viewBox]="'0 0 ' + viewWidth + ' ' + viewHeight"
        preserveAspectRatio="none"
        role="img"
        aria-label="metric trend sparkline"
      >
        <path
          class="sparkline__line"
          [attr.d]="d"
          [attr.stroke]="color()"
          fill="none"
          stroke-width="1.5"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
      </svg>
    } @else {
      <span class="sparkline__empty" data-testid="sparkline-empty">No data</span>
    }
  `,
  styles: [
    `
      .sparkline {
        display: block;
        width: 100%;
        height: 32px;
      }
      .sparkline__line {
        transition: stroke 0.3s ease;
      }
      .sparkline__empty {
        display: block;
        height: 32px;
        font-family: var(--font-mono);
        font-size: 0.6875rem;
        color: var(--text-muted);
        line-height: 32px;
      }
    `,
  ],
})
export class SparklineComponent {
  readonly data = input.required<number[]>();
  readonly color = input<string>('#4ade80');

  readonly viewWidth = VIEW_WIDTH;
  readonly viewHeight = VIEW_HEIGHT;

  readonly path = computed<string | null>(() =>
    buildSparklinePath(this.data(), VIEW_WIDTH, VIEW_HEIGHT),
  );
}
