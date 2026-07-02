import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { of } from 'rxjs';

import { AssetCardComponent } from './asset-card.component';
import { MetricService } from '../core/metric.service';
import { MetricServiceMock } from '../testing/metric-service.mock';
import { Asset, AssetServiceMock } from '../testing/asset-service.mock';
import { AssetService } from '../core/asset.service';

describe('AssetCardComponent', () => {
  let metricService: MetricServiceMock;

  function setup(inputs: { asset: Asset; isEditing?: boolean; canManage?: boolean }) {
    const fixture = TestBed.createComponent(AssetCardComponent);
    fixture.componentRef.setInput('asset', inputs.asset);
    if (inputs.isEditing !== undefined) {
      fixture.componentRef.setInput('isEditing', inputs.isEditing);
    }
    if (inputs.canManage !== undefined) {
      fixture.componentRef.setInput('canManage', inputs.canManage);
    }
    fixture.detectChanges();
    return fixture;
  }

  const asset: Asset = {
    id: 'a-1',
    name: 'web-server-01',
    type: 'SERVER',
    ipAddress: '192.168.1.10',
    status: 'ACTIVE',
    username: 'sshuser',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AssetCardComponent],
      providers: [
        { provide: MetricService, useClass: MetricServiceMock },
        { provide: AssetService, useClass: AssetServiceMock },
      ],
    }).compileComponents();
    metricService = TestBed.inject(MetricService) as unknown as MetricServiceMock;
  });

  it('renders the asset name and status badge', () => {
    const fixture = setup({ asset });
    expect(
      fixture.debugElement.query(By.css('[data-testid="asset-name"]'))
        .nativeElement.textContent,
    ).toContain('web-server-01');
    expect(
      fixture.debugElement.query(By.css('[data-testid="status-badge"]')),
    ).not.toBeNull();
  });

  it('renders three metric gauges from the latest snapshot', () => {
    metricService.history$ = (_) =>
      of([
        {
          assetId: 'a-1',
          cpuUsage: 42,
          memoryUsage: 61,
          diskUsage: 83,
          collectedAt: '2026-07-01T00:00:00Z',
        },
      ]);

    const fixture = setup({ asset });
    const gauges = fixture.debugElement.queryAll(
      By.css('[data-testid="metric-gauge"]'),
    );

    expect(gauges.length).toBe(3);
    // CPU value visible from snapshot
    const values = gauges.map(
      (g) =>
        g.query(By.css('[data-testid="gauge-value"]')).nativeElement.textContent,
    );
    expect(values.some((v) => v?.includes('42'))).toBe(true);
  });

  it('renders a stable empty state when metrics history is empty', () => {
    metricService.history$ = (_) => of([]);

    const fixture = setup({ asset });
    expect(
      fixture.debugElement.query(By.css('[data-testid="sparkline-empty"]')),
    ).not.toBeNull();
    expect(
      fixture.debugElement.queryAll(By.css('[data-testid="metric-gauge"]'))
        .length,
    ).toBe(3);
  });

  it('emits requestEdit when the edit button is clicked', () => {
    const fixture = setup({ asset });
    let emitted = 0;
    fixture.componentInstance.requestEdit.subscribe(() => (emitted += 1));

    const btn = fixture.debugElement.query(By.css('[data-testid="edit-button"]'));
    btn.nativeElement.click();
    fixture.detectChanges();

    expect(emitted).toBe(1);
  });

  it('does not show the edit button when canManage is false', () => {
    const fixture = setup({ asset, canManage: false });
    expect(
      fixture.debugElement.query(By.css('[data-testid="edit-button"]')),
    ).toBeNull();
  });

  it('emits closeEdit when the cancel-edit button is clicked while editing', () => {
    const fixture = setup({ asset, isEditing: true });
    let emitted = 0;
    fixture.componentInstance.closeEdit.subscribe(() => (emitted += 1));

    const cancel = fixture.debugElement.query(
      By.css('[data-testid="cancel-edit-button"]'),
    );
    cancel.nativeElement.click();
    fixture.detectChanges();

    expect(emitted).toBe(1);
  });

  it('shows the edit panel only when isEditing is true', () => {
    const closed = setup({ asset, isEditing: false });
    expect(
      closed.debugElement.query(By.css('app-edit-asset-panel')),
    ).toBeNull();

    const open = setup({ asset, isEditing: true });
    expect(
      open.debugElement.query(By.css('app-edit-asset-panel')),
    ).not.toBeNull();
  });

  it('requests its own metrics history by asset id exactly', () => {
    let capturedId = '';
    metricService.history$ = (id) => {
      capturedId = id;
      return of([]);
    };

    setup({ asset });
    expect(capturedId).toBe('a-1');
  });
});