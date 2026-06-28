import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { SparklineComponent } from './sparkline.component';

describe('SparklineComponent', () => {
  function setup(data: number[], color = '#4ade80') {
    const fixture = TestBed.createComponent(SparklineComponent);
    fixture.componentRef.setInput('data', data);
    fixture.componentRef.setInput('color', color);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SparklineComponent],
    }).compileComponents();
  });

  it('renders a stable empty state when data is empty (spec scenario)', () => {
    const fixture = setup([]);
    const empty = fixture.debugElement.query(By.css('[data-testid="sparkline-empty"]'));
    const path = fixture.debugElement.query(By.css('path.sparkline__line'));

    expect(empty).not.toBeNull();
    expect(empty.nativeElement.textContent).toContain('No data');
    expect(path).toBeNull();
  });

  it('renders a stable empty state when data has a single point', () => {
    const fixture = setup([42]);
    const empty = fixture.debugElement.query(By.css('[data-testid="sparkline-empty"]'));

    expect(empty).not.toBeNull();
  });

  it('renders an SVG path line when data has two or more points', () => {
    const fixture = setup([10, 50, 90]);
    const path = fixture.debugElement.query(By.css('path.sparkline__line'));

    expect(path).not.toBeNull();
    const d = path.nativeElement.getAttribute('d');
    expect(d).toContain('M');
    expect(d).toContain('L');
  });

  it('applies the provided color to the rendered line', () => {
    const fixture = setup([10, 50, 90], '#ef4444');
    const path = fixture.debugElement.query(By.css('path.sparkline__line'));

    expect(path.nativeElement.getAttribute('stroke')).toBe('#ef4444');
  });

  it('renders without any injected API dependency (pure presentational)', () => {
    const fixture = setup([10, 50, 90]);
    expect(fixture.componentInstance).toBeTruthy();
  });
});
