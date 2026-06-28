/**
 * Threshold band for a metric percentage value, mirroring the dark theme
 * design tokens in `styles.css`:
 *   - ok       0-60%   (--metric-ok, green)
 *   - warning  60-80%  (--metric-warning, amber)
 *   - critical 80-100% (--metric-critical, red)
 */
export type ThresholdBand = 'ok' | 'warning' | 'critical';

/** Upper bound (inclusive) of the ok band. */
const OK_MAX = 60;
/** Upper bound (inclusive) of the warning band. */
const WARNING_MAX = 80;

/**
 * Maps a metric percentage to its threshold band. Out-of-range values are
 * clamped to the nearest band so a malformed reading never produces an
 * undefined visual state.
 */
export function metricThresholdColor(value: number): ThresholdBand {
  const clamped = Math.max(0, Math.min(100, value));
  if (clamped <= OK_MAX) return 'ok';
  if (clamped <= WARNING_MAX) return 'warning';
  return 'critical';
}
