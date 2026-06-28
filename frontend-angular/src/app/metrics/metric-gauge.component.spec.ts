import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { MetricGaugeComponent } from './metric-gauge.component';

describe('MetricGaugeComponent', () => {
  function setup(label: string, value: number) {
    const fixture = TestBed.createComponent(MetricGaugeComponent);
    fixture.componentRef.setInput('label', label);
    fixture.componentRef.setInput('value', value);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MetricGaugeComponent],
    }).compileComponents();
  });

  it('renders the label and numeric value', () => {
    const fixture = setup('CPU', 42);

    const label = fixture.debugElement.query(By.css('[data-testid="gauge-label"]'));
    const value = fixture.debugElement.query(By.css('[data-testid="gauge-value"]'));

    expect(label.nativeElement.textContent).toContain('CPU');
    expect(value.nativeElement.textContent).toContain('42');
  });

  it('applies the ok threshold state for a value in the 0-60% band', () => {
    const fixture = setup('CPU', 30);
    const gauge = fixture.debugElement.query(By.css('[data-testid="metric-gauge"]'));

    expect(gauge.nativeElement.getAttribute('data-threshold')).toBe('ok');
  });

  it('applies the warning (amber) threshold state for a 75% value (spec scenario)', () => {
    const fixture = setup('Memory', 75);
    const gauge = fixture.debugElement.query(By.css('[data-testid="metric-gauge"]'));

    expect(gauge.nativeElement.getAttribute('data-threshold')).toBe('warning');
  });

  it('applies the critical threshold state for a value above 80%', () => {
    const fixture = setup('Disk', 92);
    const gauge = fixture.debugElement.query(By.css('[data-testid="metric-gauge"]'));

    expect(gauge.nativeElement.getAttribute('data-threshold')).toBe('critical');
  });

  it('renders a fill bar whose width reflects the value percentage', () => {
    const fixture = setup('CPU', 40);
    const fill = fixture.debugElement.query(By.css('[data-testid="gauge-fill"]'));

    expect(fill.nativeElement.style.width).toBe('40%');
  });

  it('clamps the fill width to 100% when the input value exceeds the visual range', () => {
    const fixture = setup('Disk', 140);
    const fill = fixture.debugElement.query(By.css('[data-testid="gauge-fill"]'));

    expect(fill.nativeElement.style.width).toBe('100%');
  });

  it('clamps the fill width to 0% when the input value is negative', () => {
    const fixture = setup('CPU', -15);
    const fill = fixture.debugElement.query(By.css('[data-testid="gauge-fill"]'));

    expect(fill.nativeElement.style.width).toBe('0%');
  });

  it('renders without any injected API dependency (pure presentational)', () => {
    const fixture = setup('CPU', 10);
    expect(fixture.componentInstance).toBeTruthy();
  });
});
