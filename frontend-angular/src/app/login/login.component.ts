import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService } from '../core/auth.service';

/**
 * Login page. Owns the two-field credentials form and delegates session
 * creation to `AuthService`; successful authentication navigates to the
 * dashboard, while failures stay local as an inline form error.
 */
@Component({
  selector: 'app-login',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule],
  template: `
    <div class="login__page">
      <div class="login__card">
        <h1 class="login__title">Infratrack</h1>
        <p class="login__subtitle">Sign in to your account</p>

        <form
          [formGroup]="form"
          data-testid="login-form"
          (ngSubmit)="submit()"
          class="login__form"
        >
          <label class="field">
            <span class="field__label">Username</span>
            <input
              type="text"
              autocomplete="username"
              data-testid="login-username"
              formControlName="username"
            />
          </label>

          <label class="field">
            <span class="field__label">Password</span>
            <input
              type="password"
              autocomplete="current-password"
              data-testid="login-password"
              formControlName="password"
            />
          </label>

          @if (errorMessage) {
            <p class="field__error" data-testid="login-error" role="alert">
              {{ errorMessage }}
            </p>
          }

          <button
            type="submit"
            class="login__submit"
            data-testid="login-submit"
            [disabled]="form.invalid || submitting"
          >
            {{ submitting ? 'Signing in…' : 'Sign in' }}
          </button>
        </form>
      </div>
    </div>
  `,
  styles: [
    `
      .login__page {
        display: flex;
        align-items: center;
        justify-content: center;
        min-height: 100vh;
        padding: var(--spacing-lg);
        background: var(--bg-base);
      }
      .login__card {
        background: var(--bg-card);
        border: 1px solid var(--border-subtle);
        border-radius: var(--radius);
        padding: var(--spacing-xl);
        width: 100%;
        max-width: 22rem;
      }
      .login__title {
        margin: 0;
        font-size: 1.25rem;
        font-weight: 600;
        color: var(--text-primary);
      }
      .login__subtitle {
        margin: var(--spacing-xs) 0 var(--spacing-lg);
        font-size: 0.8125rem;
        color: var(--text-secondary);
      }
      .login__form {
        display: flex;
        flex-direction: column;
        gap: var(--spacing-md);
      }
      .field {
        display: flex;
        flex-direction: column;
        gap: 4px;
      }
      .field__label {
        font-size: 0.75rem;
        color: var(--text-secondary);
      }
      .field input {
        background: var(--bg-base);
        border: 1px solid var(--border-strong);
        border-radius: var(--radius-sm);
        color: var(--text-primary);
        font-size: 0.8125rem;
        padding: 0.5rem;
      }
      .field input:focus {
        outline: none;
        border-color: var(--accent);
      }
      .field__error {
        margin: 0;
        color: var(--danger);
        font-size: 0.8125rem;
      }
      .login__submit {
        background: var(--accent);
        border: none;
        border-radius: var(--radius-sm);
        color: #001018;
        font-size: 0.8125rem;
        font-weight: 600;
        padding: 0.5rem 1rem;
        cursor: pointer;
      }
      .login__submit:hover:not(:disabled) {
        background: var(--accent-hover);
      }
      .login__submit:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
    `,
  ],
})
export class LoginComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  readonly form = this.fb.nonNullable.group({
    username: ['', [Validators.required]],
    password: ['', [Validators.required]],
  });

  submitting = false;
  errorMessage: string | null = null;

  submit(): void {
    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting = true;
    this.errorMessage = null;

    const request = this.form.getRawValue();

    this.authService.login(request).subscribe({
      next: () => {
        this.submitting = false;
        this.router.navigateByUrl('/');
      },
      error: () => {
        this.submitting = false;
        this.errorMessage = 'Invalid username or password.';
      },
    });
  }
}
