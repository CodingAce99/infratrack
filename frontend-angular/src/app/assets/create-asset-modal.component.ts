import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { AssetService } from '../core/asset.service';
import { ApiError } from '../core/api-error';
import { ASSET_TYPES, AssetType } from '../core/models';

/** IPv4 or RFC 1123 hostname — matches the backend `CreateAssetRequest` pattern. */
const IP_HOST_PATTERN = /^([0-9]{1,3}(\.[0-9]{1,3}){3}|[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?)$/;

/**
 * Create-asset modal. Owns a `ReactiveFormsModule` five-field form (name, type,
 * IP, username, password), submits to `AssetService.createAsset()`, and emits
 * `created(message)` on success or displays a form-level error on failure.
 *
 * The component does not decide list revalidation — `AssetService.createAsset`
 * already triggers `refresh()` internally on success. On a 409 duplicate IP the
 * previous asset list stays intact (service contract) and the modal stays open
 * with a visible inline error so the user can correct the form.
 */
@Component({
  selector: 'app-create-asset-modal',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule],
  template: `
    @if (isOpen()) {
      <div
        class="modal__backdrop"
        data-testid="create-modal"
        (click)="onBackdropClick($event)"
      >
        <div class="modal" role="dialog" aria-modal="true" (click)="$event.stopPropagation()">
          <div class="modal__head">
            <h2 class="modal__title">Add Asset</h2>
            <button
              type="button"
              class="modal__close"
              data-testid="modal-close"
              (click)="close.emit()"
            >
              ×
            </button>
          </div>

          <form [formGroup]="form" (ngSubmit)="submit()" class="modal__form">
            <label class="field">
              <span class="field__label">Name</span>
              <input
                type="text"
                data-testid="form-name"
                formControlName="name"
              />
            </label>

            <label class="field">
              <span class="field__label">Type</span>
              <select data-testid="form-type" formControlName="type">
                @for (t of assetTypes; track t) {
                  <option [value]="t">{{ t }}</option>
                }
              </select>
            </label>

            <label class="field">
              <span class="field__label">IP address / hostname</span>
              <input
                type="text"
                data-testid="form-ip"
                formControlName="ipAddress"
              />
            </label>

            <label class="field">
              <span class="field__label">SSH username</span>
              <input
                type="text"
                data-testid="form-username"
                formControlName="username"
              />
            </label>

            <label class="field">
              <span class="field__label">SSH password</span>
              <input
                type="password"
                data-testid="form-password"
                formControlName="password"
              />
            </label>

            @if (errorMessage) {
              <p class="field__error" data-testid="form-error" role="alert">
                {{ errorMessage }}
              </p>
            }

            <div class="modal__actions">
              <button
                type="button"
                class="modal__cancel"
                data-testid="modal-cancel"
                (click)="close.emit()"
              >
                Cancel
              </button>
              <button
                type="submit"
                class="modal__submit"
                data-testid="modal-submit"
                [disabled]="form.invalid || submitting"
              >
                {{ submitting ? 'Saving…' : 'Save' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    }
  `,
  styles: [
    `
      .modal__backdrop {
        position: fixed;
        inset: 0;
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 40;
        background: rgba(0, 0, 0, 0.6);
      }
      .modal {
        background: var(--bg-card);
        border: 1px solid var(--border-subtle);
        border-radius: var(--radius);
        padding: var(--spacing-lg);
        width: 100%;
        max-width: 30rem;
      }
      .modal__head {
        display: flex;
        align-items: center;
        justify-content: space-between;
        margin-bottom: var(--spacing-md);
      }
      .modal__title {
        margin: 0;
        font-size: 1rem;
        color: var(--text-primary);
      }
      .modal__close {
        background: transparent;
        border: none;
        color: var(--text-secondary);
        font-size: 1.25rem;
        line-height: 1;
      }
      .modal__form {
        display: flex;
        flex-direction: column;
        gap: var(--spacing-sm);
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
      .field input,
      .field select {
        background: var(--bg-base);
        border: 1px solid var(--border-strong);
        border-radius: var(--radius-sm);
        color: var(--text-primary);
        font-size: 0.8125rem;
        padding: 0.5rem;
      }
      .field__error {
        margin: 0;
        color: var(--danger);
        font-size: 0.8125rem;
      }
      .modal__actions {
        display: flex;
        justify-content: flex-end;
        gap: var(--spacing-sm);
        margin-top: var(--spacing-sm);
      }
      .modal__cancel {
        background: transparent;
        border: none;
        color: var(--text-secondary);
        font-size: 0.8125rem;
      }
      .modal__submit {
        background: var(--accent);
        border: none;
        border-radius: var(--radius-sm);
        color: #001018;
        font-size: 0.8125rem;
        font-weight: 600;
        padding: 0.5rem 1rem;
      }
      .modal__submit:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
    `,
  ],
})
export class CreateAssetModalComponent {
  readonly isOpen = input<boolean>(false);
  readonly close = output<void>();
  readonly created = output<string>();

  private readonly assetService = inject(AssetService);
  private readonly fb = inject(FormBuilder);

  readonly assetTypes: readonly AssetType[] = ASSET_TYPES;

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required]],
    type: ['SERVER' as AssetType, [Validators.required]],
    ipAddress: ['', [Validators.required, Validators.pattern(IP_HOST_PATTERN)]],
    username: ['', [Validators.required]],
    password: ['', [Validators.required]],
  });

  submitting = false;
  errorMessage: string | null = null;

  onBackdropClick(_event: MouseEvent): void {
    this.close.emit();
  }

  submit(): void {
    if (this.form.invalid || this.submitting) {
      return;
    }
    this.submitting = true;
    this.errorMessage = null;

    this.assetService.createAsset(this.form.getRawValue()).subscribe({
      next: () => {
        this.submitting = false;
        this.form.reset();
        this.created.emit('Asset added');
      },
      error: (err: unknown) => {
        this.submitting = false;
        this.errorMessage =
          err instanceof ApiError ? err.message : 'Failed to create asset';
      },
    });
  }
}