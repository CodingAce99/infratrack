import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { of, Subject } from 'rxjs';

import { DashboardComponent } from './dashboard.component';
import { AssetService } from '../core/asset.service';
import { ApiError } from '../core/api-error';
import { Asset, AssetServiceMock } from '../testing/asset-service.mock';
import { MetricService } from '../core/metric.service';
import { MetricServiceMock } from '../testing/metric-service.mock';

describe('DashboardComponent', () => {
  let assetService: AssetServiceMock;
  let metricService: MetricServiceMock;
  let assetsSubject: Subject<Asset[]>;
  let errorSubject: Subject<ApiError>;

  function setup() {
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(async () => {
    assetsSubject = new Subject<Asset[]>();
    errorSubject = new Subject<ApiError>();

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        { provide: AssetService, useClass: AssetServiceMock },
        { provide: MetricService, useClass: MetricServiceMock },
      ],
    }).compileComponents();

    assetService = TestBed.inject(AssetService) as unknown as AssetServiceMock;
    metricService = TestBed.inject(MetricService) as unknown as MetricServiceMock;

    // Wire the service streams the dashboard subscribes to.
    (assetService as unknown as Record<string, unknown>)['assets$'] =
      assetsSubject.asObservable();
    (assetService as unknown as Record<string, unknown>)['error$'] =
      errorSubject.asObservable();
  });

  function emitAssets(list: Asset[]): void {
    assetsSubject.next(list);
  }

  it('renders the Header and an empty state when there are no assets', () => {
    const fixture = setup();
    emitAssets([]);
    fixture.detectChanges();

    expect(
      fixture.debugElement.query(By.css('[data-testid="dashboard-header"]')),
    ).not.toBeNull();
    expect(
      fixture.debugElement.query(By.css('[data-testid="empty-state"]')),
    ).not.toBeNull();
  });

  it('renders an asset card for each emitted asset', () => {
    const fixture = setup();
    emitAssets([
      {
        id: 'a-1',
        name: 'web-01',
        type: 'SERVER',
        ipAddress: '1.1.1.1',
        status: 'ACTIVE',
        username: 'u',
      },
      {
        id: 'a-2',
        name: 'web-02',
        type: 'SERVER',
        ipAddress: '2.2.2.2',
        status: 'INACTIVE',
        username: 'u',
      },
    ]);
    fixture.detectChanges();

    const cards = fixture.debugElement.queryAll(
      By.css('[data-testid="asset-card"]'),
    );
    expect(cards.length).toBe(2);
  });

  it('opens the create modal when the header add button is clicked', () => {
    const fixture = setup();
    emitAssets([]);
    fixture.detectChanges();

    fixture.debugElement
      .query(By.css('[data-testid="add-asset-button"]'))
      .nativeElement.click();
    fixture.detectChanges();

    expect(
      fixture.debugElement.query(By.css('[data-testid="create-modal"]')),
    ).not.toBeNull();
  });

  it('closes the modal and shows a visible confirmation on a successful create', () => {
    const fixture = setup();
    emitAssets([]);
    fixture.detectChanges();

    assetService.createAsset.and.callFake(() => {
      // Simulate the service refreshing + emitting the new list.
      const created: Asset = {
        id: 'a-9',
        name: 'new',
        type: 'SERVER',
        ipAddress: '9.9.9.9',
        status: 'ACTIVE',
        username: 'u',
      };
      emitAssets([created]);
      return of(created);
    });

    fixture.debugElement
      .query(By.css('[data-testid="add-asset-button"]'))
      .nativeElement.click();
    fixture.detectChanges();

    // Fill a valid form and submit (delegates to modal).
    const name = fixture.debugElement.query(
      By.css('[data-testid="form-name"]'),
    ).nativeElement as HTMLInputElement;
    name.value = 'new';
    name.dispatchEvent(new Event('input'));

    const type = fixture.debugElement.query(
      By.css('[data-testid="form-type"]'),
    ).nativeElement as HTMLSelectElement;
    type.value = 'SERVER';
    type.dispatchEvent(new Event('change'));

    const ip = fixture.debugElement.query(
      By.css('[data-testid="form-ip"]'),
    ).nativeElement as HTMLInputElement;
    ip.value = '9.9.9.9';
    ip.dispatchEvent(new Event('input'));

    const user = fixture.debugElement.query(
      By.css('[data-testid="form-username"]'),
    ).nativeElement as HTMLInputElement;
    user.value = 'u';
    user.dispatchEvent(new Event('input'));

    const pw = fixture.debugElement.query(
      By.css('[data-testid="form-password"]'),
    ).nativeElement as HTMLInputElement;
    pw.value = 'p';
    pw.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    fixture.debugElement.query(
      By.css('[data-testid="modal-submit"]'),
    ).nativeElement.click();
    fixture.detectChanges();

    // Modal must close.
    expect(
      fixture.debugElement.query(By.css('[data-testid="create-modal"]')),
    ).toBeNull();
    // Visible confirmation must appear.
    const confirm = fixture.debugElement.query(
      By.css('[data-testid="confirmation"]'),
    );
    expect(confirm).not.toBeNull();
    expect(confirm.nativeElement.textContent.length).toBeGreaterThan(0);
  });

  it('enforces the single-edit-card invariant: opening edit on one card clears the previous', () => {
    const fixture = setup();
    emitAssets([
      {
        id: 'a-1',
        name: 'web-01',
        type: 'SERVER',
        ipAddress: '1.1.1.1',
        status: 'ACTIVE',
        username: 'u',
      },
      {
        id: 'a-2',
        name: 'web-02',
        type: 'SERVER',
        ipAddress: '2.2.2.2',
        status: 'ACTIVE',
        username: 'u',
      },
    ]);
    fixture.detectChanges();

    const editButtons = fixture.debugElement.queryAll(
      By.css('[data-testid="edit-button"]'),
    );
    expect(editButtons.length).toBe(2);

    // Open edit on card 1.
    editButtons[0].nativeElement.click();
    fixture.detectChanges();
    expect(
      fixture.debugElement.queryAll(By.css('app-edit-asset-panel')).length,
    ).toBe(1);

    // Card 1 is now editing → its button became "Cancel", so exactly one
    // edit button remains (card 2). Clicking it must move editing to card 2.
    const editButtonsAfter = fixture.debugElement.queryAll(
      By.css('[data-testid="edit-button"]'),
    );
    expect(editButtonsAfter.length).toBe(1);
    editButtonsAfter[0].nativeElement.click();
    fixture.detectChanges();

    // Only ONE card should remain editing (card 2), proving the invariant.
    expect(
      fixture.debugElement.queryAll(By.css('app-edit-asset-panel')).length,
    ).toBe(1);
  });

  it('60s refresh preserves an open modal and does not close it', fakeAsync(() => {
    const fixture = setup();
    emitAssets([]);
    fixture.detectChanges();

    fixture.debugElement
      .query(By.css('[data-testid="add-asset-button"]'))
      .nativeElement.click();
    fixture.detectChanges();

    expect(
      fixture.debugElement.query(By.css('[data-testid="create-modal"]')),
    ).not.toBeNull();

    // Advance the 60s timer.
    tick(60_000);
    fixture.detectChanges();

    // Modal MUST remain open — interaction-safe refresh does not close it.
    expect(
      fixture.debugElement.query(By.css('[data-testid="create-modal"]')),
    ).not.toBeNull();
  }));

  it('suppresses refresh while a card is editing', fakeAsync(() => {
    const fixture = setup();
    emitAssets([
      {
        id: 'a-1',
        name: 'web-01',
        type: 'SERVER',
        ipAddress: '1.1.1.1',
        status: 'ACTIVE',
        username: 'u',
      },
    ]);
    fixture.detectChanges();

    assetService.refresh.calls.reset();

    fixture.debugElement
      .query(By.css('[data-testid="edit-button"]'))
      .nativeElement.click();
    fixture.detectChanges();

    tick(60_000);

    // With a card editing, the timer gate MUST NOT trigger refresh.
    expect(assetService.refresh).not.toHaveBeenCalled();
  }));

  it('triggers refresh on the 60s cadence when no interaction is active', fakeAsync(() => {
    const fixture = setup();
    emitAssets([
      {
        id: 'a-1',
        name: 'web-01',
        type: 'SERVER',
        ipAddress: '1.1.1.1',
        status: 'ACTIVE',
        username: 'u',
      },
    ]);
    fixture.detectChanges();

    assetService.refresh.calls.reset();
    tick(60_000);
    expect(assetService.refresh).toHaveBeenCalledTimes(1);
  }));

  it('reflects a connected header after a successful list load', () => {
    const fixture = setup();
    emitAssets([]);
    fixture.detectChanges();

    const dot = fixture.debugElement.query(
      By.css('[data-testid="connection-indicator"]'),
    );
    expect(dot.nativeElement.getAttribute('data-connected')).toBe('true');
  });

  it('reflects a disconnected header when an API error is emitted', () => {
    const fixture = setup();
    emitAssets([]);
    fixture.detectChanges();

    errorSubject.next(new ApiError(500, 'backend down'));
    fixture.detectChanges();

    const dot = fixture.debugElement.query(
      By.css('[data-testid="connection-indicator"]'),
    );
    expect(dot.nativeElement.getAttribute('data-connected')).toBe('false');
  });

  it('reconnects after a subsequent successful refresh', () => {
    const fixture = setup();
    emitAssets([]);
    fixture.detectChanges();

    errorSubject.next(new ApiError(500, 'backend down'));
    fixture.detectChanges();

    // Successful refresh re-emits assets (mocked) → reconnect.
    assetService.refresh.and.callFake(() => {
      emitAssets([]);
    });
    assetService.refresh();
    fixture.detectChanges();

    const dot = fixture.debugElement.query(
      By.css('[data-testid="connection-indicator"]'),
    );
    expect(dot.nativeElement.getAttribute('data-connected')).toBe('true');
  });
});