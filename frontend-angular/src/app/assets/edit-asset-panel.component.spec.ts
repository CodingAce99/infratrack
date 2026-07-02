import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { of, throwError } from 'rxjs';

import { EditAssetPanelComponent } from './edit-asset-panel.component';
import { AssetService } from '../core/asset.service';
import { ApiError } from '../core/api-error';
import { Asset, AssetServiceMock } from '../testing/asset-service.mock';

describe('EditAssetPanelComponent', () => {
  let assetService: AssetServiceMock;

  const asset: Asset = {
    id: 'a-1',
    name: 'web-server-01',
    type: 'SERVER',
    ipAddress: '192.168.1.10',
    status: 'ACTIVE',
    username: 'sshuser',
  };

  function setup() {
    const fixture = TestBed.createComponent(EditAssetPanelComponent);
    fixture.componentRef.setInput('asset', asset);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EditAssetPanelComponent],
      providers: [{ provide: AssetService, useClass: AssetServiceMock }],
    }).compileComponents();
    assetService = TestBed.inject(AssetService) as unknown as AssetServiceMock;
  });

  it('renders three independent save sections (status, IP, credentials)', () => {
    const fixture = setup();
    expect(
      fixture.debugElement.query(By.css('[data-testid="section-status"]')),
    ).not.toBeNull();
    expect(
      fixture.debugElement.query(By.css('[data-testid="section-ip"]')),
    ).not.toBeNull();
    expect(
      fixture.debugElement.query(By.css('[data-testid="section-credentials"]')),
    ).not.toBeNull();
  });

  it('saves status via AssetService.updateStatus and emits updated', () => {
    const fixture = setup();
    let updated = 0;
    fixture.componentInstance.updated.subscribe(() => (updated += 1));

    const select = fixture.debugElement.query(
      By.css('[data-testid="status-select"]'),
    ).nativeElement as HTMLSelectElement;
    select.value = 'MAINTENANCE';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    const save = fixture.debugElement.query(
      By.css('[data-testid="status-save"]'),
    ).nativeElement as HTMLButtonElement;
    save.click();
    fixture.detectChanges();

    expect(assetService.updateStatus).toHaveBeenCalledWith('a-1', {
      status: 'MAINTENANCE',
    });
    expect(updated).toBe(1);
  });

  it('saves IP via AssetService.updateIp and emits updated', () => {
    const fixture = setup();
    let updated = 0;
    fixture.componentInstance.updated.subscribe(() => (updated += 1));

    const input = fixture.debugElement.query(
      By.css('[data-testid="ip-input"]'),
    ).nativeElement as HTMLInputElement;
    input.value = '10.0.0.5';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const save = fixture.debugElement.query(
      By.css('[data-testid="ip-save"]'),
    ).nativeElement as HTMLButtonElement;
    save.click();
    fixture.detectChanges();

    expect(assetService.updateIp).toHaveBeenCalledWith('a-1', {
      ipAddress: '10.0.0.5',
    });
    expect(updated).toBe(1);
  });

  it('saves credentials via AssetService.updateCredentials and emits updated', () => {
    const fixture = setup();
    let updated = 0;
    fixture.componentInstance.updated.subscribe(() => (updated += 1));

    const user = fixture.debugElement.query(
      By.css('[data-testid="cred-username"]'),
    ).nativeElement as HTMLInputElement;
    user.value = 'newuser';
    user.dispatchEvent(new Event('input'));

    const pw = fixture.debugElement.query(
      By.css('[data-testid="cred-password"]'),
    ).nativeElement as HTMLInputElement;
    pw.value = 'pw';
    pw.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const save = fixture.debugElement.query(
      By.css('[data-testid="cred-save"]'),
    ).nativeElement as HTMLButtonElement;
    save.click();
    fixture.detectChanges();

    expect(assetService.updateCredentials).toHaveBeenCalledWith('a-1', {
      username: 'newuser',
      password: 'pw',
    });
    expect(updated).toBe(1);
  });

  it('shows a section error and does not emit updated when a save fails', () => {
    const fixture = setup();
    let updated = 0;
    fixture.componentInstance.updated.subscribe(() => (updated += 1));
    assetService.updateStatus.and.returnValue(
      throwError(() => new ApiError(400, 'invalid status')),
    );

    const save = fixture.debugElement.query(
      By.css('[data-testid="status-save"]'),
    ).nativeElement as HTMLButtonElement;
    save.click();
    fixture.detectChanges();

    expect(updated).toBe(0);
    const err = fixture.debugElement.query(By.css('[data-testid="status-error"]'));
    expect(err).not.toBeNull();
    expect(err.nativeElement.textContent).toContain('invalid status');
  });

  it('opens a confirm dialog before deleting and cancels leave asset unchanged', () => {
    const fixture = setup();
    let deleted = 0;
    fixture.componentInstance.deleted.subscribe(() => (deleted += 1));

    const open = fixture.debugElement.query(
      By.css('[data-testid="delete-button"]'),
    ).nativeElement as HTMLButtonElement;
    open.click();
    fixture.detectChanges();

    // Confirm dialog rendered.
    expect(
      fixture.debugElement.query(By.css('[data-testid="confirm-dialog"]')),
    ).not.toBeNull();

    // Cancel the confirmation — nothing should be deleted.
    const cancel = fixture.debugElement.query(
      By.css('[data-testid="cancel-button"]'),
    ).nativeElement as HTMLButtonElement;
    cancel.click();
    fixture.detectChanges();

    expect(deleted).toBe(0);
    expect(assetService.deleteAsset).not.toHaveBeenCalled();
    expect(
      fixture.debugElement.query(By.css('[data-testid="confirm-dialog"]')),
    ).toBeNull();
  });

  it('confirms delete via AssetService.deleteAsset and emits deleted', () => {
    const fixture = setup();
    let deleted = 0;
    fixture.componentInstance.deleted.subscribe(() => (deleted += 1));

    fixture.debugElement.query(
      By.css('[data-testid="delete-button"]'),
    ).nativeElement.click();
    fixture.detectChanges();

    const confirm = fixture.debugElement.query(
      By.css('[data-testid="confirm-button"]'),
    ).nativeElement as HTMLButtonElement;
    confirm.click();
    fixture.detectChanges();

    expect(assetService.deleteAsset).toHaveBeenCalledWith('a-1');
    expect(deleted).toBe(1);
  });

  it('emits cancel when the cancel button is clicked', () => {
    const fixture = setup();
    let cancelCount = 0;
    fixture.componentInstance.cancel.subscribe(() => (cancelCount += 1));

    fixture.debugElement.query(
      By.css('[data-testid="panel-cancel"]'),
    ).nativeElement.click();
    fixture.detectChanges();

    expect(cancelCount).toBe(1);
  });
});