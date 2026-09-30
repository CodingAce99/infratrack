import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Router } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';

import { LoginComponent } from './login.component';
import { AuthService } from '../core/auth.service';
import { LoginResponse } from '../core/models';

describe('LoginComponent', () => {
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  const loginResponse: LoginResponse = {
    token: 'jwt-token',
    type: 'Bearer',
    username: 'admin',
    role: 'ADMIN',
  };

  beforeEach(async () => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['login']);
    router = jasmine.createSpyObj<Router>('Router', ['navigateByUrl']);
    router.navigateByUrl.and.resolveTo(true);

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: Router, useValue: router },
      ],
    }).compileComponents();
  });

  function setup() {
    const fixture = TestBed.createComponent(LoginComponent);
    fixture.detectChanges();
    return fixture;
  }

  function fillValidForm(fixture: ReturnType<typeof setup>): void {
    const username = fixture.debugElement.query(
      By.css('[data-testid="login-username"]'),
    ).nativeElement as HTMLInputElement;
    const password = fixture.debugElement.query(
      By.css('[data-testid="login-password"]'),
    ).nativeElement as HTMLInputElement;

    username.value = 'admin';
    username.dispatchEvent(new Event('input'));
    password.value = 'secret';
    password.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  it('renders the login form fields', () => {
    const fixture = setup();

    expect(
      fixture.debugElement.query(By.css('[data-testid="login-form"]')),
    ).not.toBeNull();
    expect(
      fixture.debugElement.query(By.css('[data-testid="login-username"]')),
    ).not.toBeNull();
    expect(
      fixture.debugElement.query(By.css('[data-testid="login-password"]')),
    ).not.toBeNull();
  });

  it('keeps submit disabled while the form is invalid', () => {
    const fixture = setup();

    const submit = fixture.debugElement.query(
      By.css('[data-testid="login-submit"]'),
    ).nativeElement as HTMLButtonElement;

    expect(submit.disabled).toBe(true);
  });

  it('calls AuthService.login with the credentials and navigates to the dashboard on success', () => {
    const fixture = setup();
    fillValidForm(fixture);
    authService.login.and.returnValue(of(loginResponse));

    const submit = fixture.debugElement.query(
      By.css('[data-testid="login-submit"]'),
    ).nativeElement as HTMLButtonElement;
    submit.click();
    fixture.detectChanges();

    expect(authService.login).toHaveBeenCalledOnceWith({
      username: 'admin',
      password: 'secret',
    });
    expect(router.navigateByUrl).toHaveBeenCalledOnceWith('/');
    expect(fixture.componentInstance.submitting).toBe(false);
  });

  it('shows an inline error and does not navigate when login fails', () => {
    const fixture = setup();
    fillValidForm(fixture);
    authService.login.and.returnValue(
      throwError(() => ({ status: 401, statusText: 'Unauthorized' })),
    );

    const submit = fixture.debugElement.query(
      By.css('[data-testid="login-submit"]'),
    ).nativeElement as HTMLButtonElement;
    submit.click();
    fixture.detectChanges();

    const error = fixture.debugElement.query(By.css('[data-testid="login-error"]'));
    expect(error).not.toBeNull();
    expect(error.nativeElement.textContent).toContain(
      'Invalid username or password.',
    );
    expect(router.navigateByUrl).not.toHaveBeenCalled();
    expect(fixture.componentInstance.submitting).toBe(false);
  });

  it('distinguishes service failures from invalid credentials', () => {
    const fixture = setup();
    fillValidForm(fixture);
    authService.login.and.returnValue(
      throwError(() => ({ status: 503, statusText: 'Service Unavailable' })),
    );

    fixture.debugElement.query(By.css('[data-testid="login-submit"]')).nativeElement.click();
    fixture.detectChanges();

    const error = fixture.debugElement.query(By.css('[data-testid="login-error"]'));
    expect(error.nativeElement.textContent).toContain(
      'Unable to sign in. Please try again.',
    );
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });

  it('cancels an in-flight login when the component is destroyed', () => {
    const response = new Subject<LoginResponse>();
    const fixture = setup();
    fillValidForm(fixture);
    authService.login.and.returnValue(response.asObservable());

    fixture.debugElement.query(By.css('[data-testid="login-submit"]')).nativeElement.click();
    expect(response.observed).toBe(true);

    fixture.destroy();
    expect(response.observed).toBe(false);

    response.next(loginResponse);
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });
});
