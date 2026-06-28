import { metricThresholdColor, ThresholdBand } from './threshold-color';

describe('metricThresholdColor', () => {
  it('classifies a value in the ok band (0-60%) as ok', () => {
    expect(metricThresholdColor(0)).toBe('ok');
    expect(metricThresholdColor(30)).toBe('ok');
    expect(metricThresholdColor(60)).toBe('ok');
  });

  it('classifies a value in the warning band (60-80%) as warning', () => {
    // Spec scenario: 75% MUST render the amber threshold state.
    expect(metricThresholdColor(61)).toBe('warning');
    expect(metricThresholdColor(75)).toBe('warning');
    expect(metricThresholdColor(80)).toBe('warning');
  });

  it('classifies a value in the critical band (80-100%) as critical', () => {
    expect(metricThresholdColor(81)).toBe('critical');
    expect(metricThresholdColor(95)).toBe('critical');
    expect(metricThresholdColor(100)).toBe('critical');
  });

  it('clamps out-of-range values to the nearest band', () => {
    expect(metricThresholdColor(-5)).toBe('ok');
    expect(metricThresholdColor(150)).toBe('critical');
  });

  it('returns a value constrained to the ThresholdBand union', () => {
    const band: ThresholdBand = metricThresholdColor(42);
    // Compile-time proof the return type is the union, not string.
    expect(['ok', 'warning', 'critical']).toContain(band);
  });
});
