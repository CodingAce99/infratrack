import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { StatusBadgeComponent } from './status-badge.component';
import { AssetStatus } from '../core/models';

describe('StatusBadgeComponent', () => {
  function setup(status: AssetStatus) {
    const fixture = TestBed.createComponent(StatusBadgeComponent);
    fixture.componentRef.setInput('status', status);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StatusBadgeComponent],
    }).compileComponents();
  });

  it('renders the active label and active visual state for ACTIVE', () => {
    const fixture = setup('ACTIVE');
    const badge = fixture.debugElement.query(By.css('[data-testid="status-badge"]'));

    expect(badge).not.toBeNull();
    expect(badge.nativeElement.textContent).toContain('Active');
    expect(badge.nativeElement.getAttribute('data-status')).toBe('ACTIVE');
  });

  it('renders the maintenance label for MAINTENANCE', () => {
    const fixture = setup('MAINTENANCE');
    const badge = fixture.debugElement.query(By.css('[data-testid="status-badge"]'));

    expect(badge.nativeElement.textContent).toContain('Maintenance');
    expect(badge.nativeElement.getAttribute('data-status')).toBe('MAINTENANCE');
  });

  it('renders the inactive label for INACTIVE', () => {
    const fixture = setup('INACTIVE');
    const badge = fixture.debugElement.query(By.css('[data-testid="status-badge"]'));

    expect(badge.nativeElement.textContent).toContain('Inactive');
    expect(badge.nativeElement.getAttribute('data-status')).toBe('INACTIVE');
  });

  it('renders without any injected API dependency (pure presentational)', () => {
    // No HttpClient, no service providers — TestBed has only the component.
    // If the component tried to call an API it would throw on construction.
    const fixture = setup('ACTIVE');
    expect(fixture.componentInstance).toBeTruthy();
  });
});
