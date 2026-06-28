import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

/**
 * Pure presentational confirmation dialog. Receives a `message` input and
 * emits `confirm` / `cancel` outputs. Clicking the backdrop emits `cancel`
 * (clicking inside the dialog body does not), so a cancelled confirmation
 * leaves the underlying asset unchanged. Has no API access and no service
 * dependencies — the owning component decides what a confirm/cancel means.
 */
@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div
      class="confirm-dialog__backdrop"
      data-testid="confirm-backdrop"
      (click)="onBackdropClick($event)"
    >
      <div
        class="confirm-dialog"
        role="alertdialog"
        aria-modal="true"
        data-testid="confirm-dialog"
        (click)="onBodyClick($event)"
      >
        <p class="confirm-dialog__message" data-testid="confirm-message">{{ message() }}</p>
        <div class="confirm-dialog__actions">
          <button
            type="button"
            class="confirm-dialog__cancel"
            data-testid="cancel-button"
            (click)="cancel.emit()"
          >
            Cancel
          </button>
          <button
            type="button"
            class="confirm-dialog__confirm"
            data-testid="confirm-button"
            (click)="confirm.emit()"
          >
            Delete
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [
    `
      .confirm-dialog__backdrop {
        position: fixed;
        inset: 0;
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 50;
        background: rgba(0, 0, 0, 0.6);
      }
      .confirm-dialog {
        background: var(--bg-card);
        border: 1px solid var(--border-subtle);
        border-radius: var(--radius);
        padding: var(--spacing-lg);
        width: 100%;
        max-width: 24rem;
      }
      .confirm-dialog__message {
        margin: 0 0 var(--spacing-md);
        color: var(--text-primary);
        font-size: 0.875rem;
      }
      .confirm-dialog__actions {
        display: flex;
        justify-content: flex-end;
        gap: var(--spacing-md);
      }
      .confirm-dialog__cancel {
        background: transparent;
        border: none;
        color: var(--text-secondary);
        font-size: 0.875rem;
      }
      .confirm-dialog__cancel:hover {
        color: var(--text-primary);
        text-decoration: underline;
      }
      .confirm-dialog__confirm {
        background: var(--danger);
        border: none;
        border-radius: var(--radius-sm);
        color: #fff;
        font-size: 0.875rem;
        padding: 0.5rem 1rem;
      }
      .confirm-dialog__confirm:hover {
        background: var(--danger-hover);
      }
    `,
  ],
})
export class ConfirmDialogComponent {
  readonly message = input.required<string>();
  readonly confirm = output<void>();
  readonly cancel = output<void>();

  /** Backdrop click closes the dialog as a cancel. */
  onBackdropClick(_event: MouseEvent): void {
    this.cancel.emit();
  }

  /** Stops propagation so a click inside the body does not bubble to the backdrop. */
  onBodyClick(event: MouseEvent): void {
    event.stopPropagation();
  }
}
