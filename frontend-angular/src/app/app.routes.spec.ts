import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { By } from '@angular/platform-browser';

import { routes } from './app.routes';
import { provideRouter } from '@angular/router';
import { AssetService } from './core/asset.service';
import { of, Subject } from 'rxjs';
import { ApiError } from './core/api-error';
import { MetricService } from './core/metric.service';

describe('app.routes', () => {
  async function setup() {
    const err$ = new Subject<ApiError>();
    await TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        {
          provide: AssetService,
          useValue: {
            assets$: of([]),
            loading$: of(false),
            error$: err$,
            refresh: () => {},
            createAsset: () => of({}),
            deleteAsset: () => of({}),
            updateStatus: () => of({}),
            updateIp: () => of({}),
            updateCredentials: () => of({}),
          },
        },
        {
          provide: MetricService,
          useValue: { history$: () => of([]), error$: of() },
        },
      ],
    }).compileComponents();
    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/');
    return harness;
  }

  it('renders the real DashboardComponent at "/" (not the placeholder)', async () => {
    const harness = await setup();
    const dashboard = harness.fixture.debugElement.query(
      By.css('[data-testid="dashboard-header"]'),
    );
    expect(dashboard).not.toBeNull();
  });
});