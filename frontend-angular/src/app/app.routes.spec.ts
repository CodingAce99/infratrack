import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { By } from '@angular/platform-browser';

import { routes } from './app.routes';
import { provideRouter } from '@angular/router';
import { AssetService } from './core/asset.service';
import { BehaviorSubject, of, Subject } from 'rxjs';
import { ApiError } from './core/api-error';
import { MetricService } from './core/metric.service';
import { AuthService } from './core/auth.service';

describe('app.routes', () => {
  async function setup(isAuthenticated = true) {
    const err$ = new Subject<ApiError>();
    const authenticated$ = new BehaviorSubject<boolean>(isAuthenticated);
    await TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),
        {
          provide: AuthService,
          useValue: {
            isAuthenticated$: authenticated$.asObservable(),
            login: jasmine.createSpy('login'),
          },
        },
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
    const router = TestBed.inject(Router);
    return { harness, router };
  }

  it('renders the real DashboardComponent at "/" for an authenticated session', async () => {
    const { harness } = await setup(true);
    await harness.navigateByUrl('/');

    const dashboard = harness.fixture.debugElement.query(
      By.css('[data-testid="dashboard-header"]'),
    );
    expect(dashboard).not.toBeNull();
  });

  it('redirects unauthenticated dashboard navigation to /login', async () => {
    const { harness, router } = await setup(false);

    await harness.navigateByUrl('/');

    expect(router.url).toBe('/login');
    expect(
      harness.fixture.debugElement.query(By.css('[data-testid="login-form"]')),
    ).not.toBeNull();
  });

  it('allows direct navigation to the public login route', async () => {
    const { harness, router } = await setup(false);

    await harness.navigateByUrl('/login');

    expect(router.url).toBe('/login');
    expect(
      harness.fixture.debugElement.query(By.css('[data-testid="login-form"]')),
    ).not.toBeNull();
  });
});
