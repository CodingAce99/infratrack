import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { provideRouter } from '@angular/router';
import { BehaviorSubject, firstValueFrom, Observable } from 'rxjs';

import { authGuard } from './auth.guard';
import { AuthService } from './auth.service';

describe('authGuard', () => {
  let authenticatedSubject: BehaviorSubject<boolean>;
  let router: Router;

  beforeEach(() => {
    authenticatedSubject = new BehaviorSubject<boolean>(false);

    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            isAuthenticated$: authenticatedSubject.asObservable(),
          },
        },
      ],
    });

    router = TestBed.inject(Router);
  });

  it('allows activation when the local auth session is active', async () => {
    authenticatedSubject.next(true);

    const result = await runGuard();

    expect(result).toBe(true);
  });

  it('redirects unauthenticated users to /login', async () => {
    authenticatedSubject.next(false);

    const result = await runGuard();

    expect(result instanceof UrlTree).toBe(true);
    expect(router.serializeUrl(result as UrlTree)).toBe('/login');
  });

  function runGuard(): Promise<boolean | UrlTree> {
    const result = TestBed.runInInjectionContext(() =>
      authGuard({} as never, {} as never),
    );

    return firstValueFrom(result as Observable<boolean | UrlTree>);
  }
});
