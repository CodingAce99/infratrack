import {
  ChangeDetectionStrategy,
  Component,
  inject,
  input,
  OnInit,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';

import { AssetService } from '../core/asset.service';
import { ApiError } from '../core/api-error';
import { ASSET_STATUSES, Asset, AssetStatus } from '../core/models';
import { ConfirmDialogComponent } from '../shared/confirm-dialog.component';

const IP_HOST_PATTERN = /^([0-9]{1,3}(\.[0-9]{1,3}){3}|[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?)$/;

/**
 * Inline edit panel for a single asset. The panel owns three INDEPENDENT
 * ReactiveForms groups — status, IP, and credentials — each with its own Save
 * action mapped 1:1 to the matching `AssetService.update*()` method (no "save
 * all", matching the backend's separate endpoints). Delete is gated behind a
 * `ConfirmDialog` so a cancelled confirmation leaves the asset unchanged.
 *
 * The panel never owns edit coordination across siblings; it only reports
 * outcomes upward via `updated` / `deleted` / `cancel` outputs.
 */
@Component({
  selector: 'app-edit-asset-panel',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, ReactiveFormsModule, ConfirmDialogComponent],
  template: `
    <div class="edit-panel" data-testid="edit-panel">
      <section class="section" data-testid="section-status">
        <h4 class="section__title">Status</h4>
        <select
          data-testid="status-select"
          [formControl]="statusControl"
        >
          @for (s of statuses; track s) {
            <option [value]="s">{{ s }}</option>
          }
        </select>
        <button
          type="button"
          data-testid="status-save"
          (click)="saveStatus()"
        >Save</button>
        @if (statusError) {
          <p class="section__error" data-testid="status-error" role="alert">
            {{ statusError }}
          </p>
        }
      </section>

      <section class="section" data-testid="section-ip">
        <h4 class="section__title">IP address / hostname</h4>
        <input
          type="text"
          data-testid="ip-input"
          [formControl]="ipControl"
        />
        <button
          type="button"
          data-testid="ip-save"
          (click)="saveIp()"
        >Save</button>
        @if (ipError) {
          <p class="section__error" data-testid="ip-error" role="alert">
            {{ ipError }}
          </p>
        }
      </section>

      <section class="section" data-testid="section-credentials">
        <h4 class="section__title">Credentials</h4>
        <input
          type="text"
          data-testid="cred-username"
          placeholder="SSH username"
          [formControl]="usernameControl"
        />
        <input
          type="password"
          data-testid="cred-password"
          placeholder="SSH password"
          [formControl]="passwordControl"
        />
        <button
          type="button"
          data-testid="cred-save"
          (click)="saveCredentials()"
        >Save</button>
        @if (credError) {
          <p class="section__error" data-testid="cred-error" role="alert">
            {{ credError }}
          </p>
        }
      </section>

      <section class="section section--danger">
        <h4 class="section__title">Delete asset</h4>
        <button
          type="button"
          class="danger"
          data-testid="delete-button"
          (click)="confirmDelete = true"
        >Delete…</button>
      </section>

      <div class="edit-panel__cancel">
        <button
          type="button"
          data-testid="panel-cancel"
          (click)="cancel.emit()"
        >Close</button>
      </div>

      @if (confirmDelete) {
        <app-confirm-dialog
          message="Delete this asset? This action cannot be undone."
          (confirm)="confirmDeleteAsset()"
          (cancel)="confirmDelete = false"
        />
      }
    </div>
  `,
  styles: [
    `
      .edit-panel {
        display: flex;
        flex-direction: column;
        gap: var(--spacing-md);
        padding-top: var(--spacing-md);
        border-top: 1px solid var(--border-subtle);
      }
      .section {
        display: flex;
        flex-direction: column;
        gap: var(--spacing-xs);
      }
      .section__title {
        margin: 0;
        font-size: 0.75rem;
        text-transform: uppercase;
        letter-spacing: 0.05em;
        color: var(--text-muted);
      }
      .section input,
      .section select {
        background: var(--bg-base);
        border: 1px solid var(--border-strong);
        border-radius: var(--radius-sm);
        color: var(--text-primary);
        font-size: 0.8125rem;
        padding: 0.4rem;
      }
      .section button {
        background: var(--accent);
        border: none;
        border-radius: var(--radius-sm);
        color: #001018;
        font-size: 0.8125rem;
        font-weight: 600;
        padding: 0.4rem 0.8rem;
        align-self: flex-start;
      }
      .section .danger {
        background: var(--danger);
        color: #fff;
      }
      .section .danger:hover {
        background: var(--danger-hover);
      }
      .section__error {
        margin: 0;
        color: var(--danger);
        font-size: 0.8125rem;
      }
      .edit-panel__cancel {
        display: flex;
        justify-content: flex-end;
      }
      .edit-panel__cancel button {
        background: transparent;
        border: none;
        color: var(--text-secondary);
        font-size: 0.8125rem;
      }
    `,
  ],
})
export class EditAssetPanelComponent implements OnInit {
  readonly asset = input.required<Asset>();
  readonly updated = output<void>();
  readonly deleted = output<void>();
  readonly cancel = output<void>();

  private readonly assetService = inject(AssetService);
  private readonly fb = inject(FormBuilder);

  readonly statuses: readonly AssetStatus[] = ASSET_STATUSES;

  // Built in ngOnInit so the required `asset` input is available (signal inputs
  // are not readable in field initializers — see Angular error NG0950).
  statusControl!: FormControl<AssetStatus>;
  ipControl!: FormControl<string>;
  usernameControl!: FormControl<string>;
  passwordControl!: FormControl<string>;

  ngOnInit(): void {
    const asset = this.asset();
    this.statusControl = this.fb.nonNullable.control(asset.status, {
      validators: [Validators.required],
    }) as FormControl<AssetStatus>;
    this.ipControl = this.fb.nonNullable.control(asset.ipAddress, {
      validators: [Validators.required, Validators.pattern(IP_HOST_PATTERN)],
    }) as FormControl<string>;
    this.usernameControl = this.fb.nonNullable.control(asset.username, {
      validators: [Validators.required],
    }) as FormControl<string>;
    this.passwordControl = this.fb.nonNullable.control('', {
      validators: [Validators.required],
    }) as FormControl<string>;
  }

  statusError: string | null = null;
  ipError: string | null = null;
  credError: string | null = null;
  confirmDelete = false;

  saveStatus(): void {
    if (this.statusControl.invalid) {
      return;
    }
    this.statusError = null;
    this.assetService
      .updateStatus(this.asset().id, { status: this.statusControl.value })
      .subscribe({
        next: () => this.updated.emit(),
        error: (err: unknown) => {
          this.statusError = err instanceof ApiError ? err.message : 'Failed to update status';
        },
      });
  }

  saveIp(): void {
    if (this.ipControl.invalid) {
      return;
    }
    this.ipError = null;
    this.assetService
      .updateIp(this.asset().id, { ipAddress: this.ipControl.value })
      .subscribe({
        next: () => this.updated.emit(),
        error: (err: unknown) => {
          this.ipError = err instanceof ApiError ? err.message : 'Failed to update IP';
        },
      });
  }

  saveCredentials(): void {
    if (this.usernameControl.invalid || this.passwordControl.invalid) {
      return;
    }
    this.credError = null;
    this.assetService
      .updateCredentials(this.asset().id, {
        username: this.usernameControl.value,
        password: this.passwordControl.value,
      })
      .subscribe({
        next: () => this.updated.emit(),
        error: (err: unknown) => {
          this.credError =
            err instanceof ApiError ? err.message : 'Failed to update credentials';
        },
      });
  }

  confirmDeleteAsset(): void {
    this.confirmDelete = false;
    this.assetService.deleteAsset(this.asset().id).subscribe({
      next: () => this.deleted.emit(),
      error: () => {
        // Even on error we keep the panel usable; the dashboard still owns
        // list revalidation via the service. We do NOT emit deleted on failure.
      },
    });
  }
}