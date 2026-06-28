import { buildSparklinePath } from './sparkline-path';

describe('buildSparklinePath', () => {
  it('returns null when there are fewer than two points (no line to draw)', () => {
    expect(buildSparklinePath([], 100, 32)).toBeNull();
    expect(buildSparklinePath([42], 100, 32)).toBeNull();
  });

  it('builds an M...L... path that spans the full width for a multi-point series', () => {
    const path = buildSparklinePath([10, 90], 100, 40);
    expect(path).not.toBeNull();
    // First command is a move to the left edge (x=0); last point reaches the right edge (x=100).
    expect(path!.startsWith('M0 ')).toBeTrue();
    expect(path!).toContain('L100 ');
  });

  it('inverts the y-axis so a higher value renders higher on the chart', () => {
    // value 10 (low) -> larger y (near bottom); value 90 (high) -> smaller y (near top).
    const path = buildSparklinePath([10, 90], 100, 40)!.split(' ');
    const y0 = Number(path[1]); // y of first point (value 10)
    const y1 = Number(path.at(-1)); // y of last point (value 90)
    expect(y0).toBeGreaterThan(y1);
  });

  it('normalizes values to the provided height using the series min/max range', () => {
    // Series [20, 50, 80] — min 20 maps to bottom (y=height), max 80 maps to top (y=0).
    const path = buildSparklinePath([20, 50, 80], 120, 60)!;
    const ys = path.match(/-?\d+(?:\.\d+)?/g)!.filter((_, i) => i % 2 === 1).map(Number);
    expect(Math.min(...ys)).toBe(0); // max value 80 -> y=0
    expect(Math.max(...ys)).toBe(60); // min value 20 -> y=60
  });

  it('handles a flat series without dividing by zero', () => {
    const path = buildSparklinePath([50, 50, 50], 90, 30);
    expect(path).not.toBeNull();
    // All points share the same y (mid-height) — no NaN, no crash.
    const ys = path!.match(/-?\d+(?:\.\d+)?/g)!.filter((_, i) => i % 2 === 1).map(Number);
    expect(ys.every((y) => Number.isFinite(y))).toBeTrue();
  });
});
