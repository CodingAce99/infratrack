/**
 * Builds an SVG path string (`M x0 y0 L x1 y1 ...`) for a sparkline from a
 * series of numeric values. Returns `null` when there are fewer than two
 * points, so the caller can render a stable empty state instead of a broken
 * or zero-length path.
 *
 * The path is laid out within a `width` x `height` viewBox. Values are
 * normalized to the series' own min/max range, and the y-axis is inverted so
 * that a higher value renders closer to the top of the chart (SVG y grows
 * downward).
 */
export function buildSparklinePath(
  values: readonly number[],
  width: number,
  height: number,
): string | null {
  if (values.length < 2) return null;

  const min = Math.min(...values);
  const max = Math.max(...values);
  const range = max - min;

  const xStep = width / (values.length - 1);
  const points = values.map((value, index) => {
    const x = index * xStep;
    // Normalize to [0,1]; flat series (range 0) collapses to mid-height.
    const normalized = range === 0 ? 0.5 : (value - min) / range;
    const y = height - normalized * height;
    return `${index === 0 ? 'M' : 'L'}${round(x)} ${round(y)}`;
  });

  return points.join(' ');
}

/** Rounds to two decimals to keep path strings compact without visible loss. */
function round(n: number): number {
  return Math.round(n * 100) / 100;
}
