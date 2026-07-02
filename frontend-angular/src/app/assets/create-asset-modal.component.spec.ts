import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { of, throwError } from 'rxjs';

import { CreateAssetModalComponent } from './create-asset-modal.component';
import { AssetService } from '../core/asset.service';
import { ApiError } from '../core/api-error';
import { Asset, AssetServiceMock } from '../testing/asset-service.mock';

describe('CreateAssetModalComponent', () => {
  let assetService: AssetServiceMock;

  function setup(initialOpen = false) {
    const fixture = TestBed.createComponent(CreateAssetModalComponent);
    fixture.componentRef.setInput('isOpen', initialOpen);
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CreateAssetModalComponent],
      providers: [{ provide: AssetService, useClass: AssetServiceMock }],
    }).compileComponents();
    assetService = TestBed.inject(AssetService) as unknown as AssetServiceMock;
  });

  function fillValidForm(fixture: ReturnType<typeof setup>) {
    const name = fixture.debugElement.query(By.css('[data-testid="form-name"]'))
      .nativeElement as HTMLInputElement;
    const type = fixture.debugElement.query(By.css('[data-testid="form-type"]'))
      .nativeElement as HTMLSelectElement;
    const ip = fixture.debugElement.query(By.css('[data-testid="form-ip"]'))
      .nativeElement as HTMLInputElement;
    const username = fixture.debugElement.query(
      By.css('[data-testid="form-username"]'),
    ).nativeElement as HTMLInputElement;
    const password = fixture.debugElement.query(By.css('[data-testid="form-password"]'))
      .nativeElement as HTMLInputElement;

    name.value = 'web-server-01';
    name.dispatchEvent(new Event('input'));
    type.value = 'SERVER';
    type.dispatchEvent(new Event('change'));
    ip.value = '192.168.1.10';
    ip.dispatchEvent(new Event('input'));
    username.value = 'sshuser';
    username.dispatchEvent(new Event('input'));
    password.value = 'secret';
    password.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  it('renders the form only when isOpen is true', () => {
    const fixture = setup(false);
    expect(
      fixture.debugElement.query(By.css('[data-testid="create-modal"]')),
    ).toBeNull();
  });

  it('renders the form with all five fields when isOpen is true', () => {
    const fixture = setup(true);
    expect(
      fixture.debugElement.query(By.css('[data-testid="create-modal"]')),
    ).not.toBeNull();
    expect(
      fixture.debugElement.query(By.css('[data-testid="form-name"]')),
    ).not.toBeNull();
    expect(
      fixture.debugElement.query(By.css('[data-testid="form-type"]')),
    ).not.toBeNull();
    expect(
      fixture.debugElement.query(By.css('[data-testid="form-ip"]')),
    ).not.toBeNull();
    expect(
      fixture.debugElement.query(By.css('[data-testid="form-username"]')),
    ).not.toBeNull();
    expect(
      fixture.debugElement.query(By.css('[data-testid="form-password"]')),
    ).not.toBeNull();
  });

  it('emits close when the close button is clicked', () => {
    const fixture = setup(true);
    let closed = 0;
    fixture.componentInstance.close.subscribe(() => (closed += 1));

    const btn = fixture.debugElement.query(By.css('[data-testid="modal-close"]'));
    btn.nativeElement.click();
    fixture.detectChanges();

    expect(closed).toBe(1);
  });

  it('disables the submit button while the form is invalid', () => {
    const fixture = setup(true);
    const submit = fixture.debugElement.query(
      By.css('[data-testid="modal-submit"]'),
    ).nativeElement as HTMLButtonElement;

    expect(submit.disabled).toBe(true);
  });

  it('enables the submit button once all fields are valid', () => {
    const fixture = setup(true);
    fillValidForm(fixture);

    const submit = fixture.debugElement.query(
      By.css('[data-testid="modal-submit"]'),
    ).nativeElement as HTMLButtonElement;

    expect(submit.disabled).toBe(false);
  });

  it('submits valid form via AssetService.createAsset and emits created event', () => {
    const fixture = setup(true);
    fillValidForm(fixture);

    let createdMessage = '';
    fixture.componentInstance.created.subscribe((m: string) => (createdMessage = m));

    let createdPayload: unknown = null;
    assetService.createAsset.and.callFake((payload) => {
      createdPayload = payload;
      return of({ id: 'asset-9', name: 'new', type: 'SERVER' } as Asset);
    });

    const submit = fixture.debugElement.query(
      By.css('[data-testid="modal-submit"]'),
    ).nativeElement as HTMLButtonElement;
    submit.click();
    fixture.detectChanges();

    expect(assetService.createAsset).toHaveBeenCalledTimes(1);
    expect(createdPayload).toEqual({
      name: 'web-server-01',
      type: 'SERVER',
      ipAddress: '192.168.1.10',
      username: 'sshuser',
      password: 'secret',
    });
    expect(createdMessage.length).toBeGreaterThan(0);
  });

  it('shows a form-level error and keeps the modal open on a 409 duplicate IP', () => {
    const fixture = setup(true);
    fillValidForm(fixture);

    assetService.createAsset.and.returnValue(
      throwError(
        () => new ApiError(409, 'Asset with IP address 192.168.1.10 already exists'),
      ),
    );

    const submit = fixture.debugElement.query(
      By.css('[data-testid="modal-submit"]'),
    ).nativeElement as HTMLButtonElement;
    submit.click();
    fixture.detectChanges();

    const error = fixture.debugElement.query(By.css('[data-testid="form-error"]'));
    expect(error).not.toBeNull();
    expect(error.nativeElement.textContent).toContain('192.168.1.10');

    // Modal must remain rendered so the user can correct the form.
    expect(
      fixture.debugElement.query(By.css('[data-testid="create-modal"]')),
    ).not.toBeNull();
  });

  it('shows a general error for a non-409 failure and keeps the modal open', () => {
    const fixture = setup(true);
    fillValidForm(fixture);

    assetService.createAsset.and.returnValue(
      throwError(() => new ApiError(500, 'backend down')),
    );

    const submit = fixture.debugElement.query(
      By.css('[data-testid="modal-submit"]'),
    ).nativeElement as HTMLButtonElement;
    submit.click();
    fixture.detectChanges();

    const error = fixture.debugElement.query(By.css('[data-testid="form-error"]'));
    expect(error).not.toBeNull();
    expect(error.nativeElement.textContent).toContain('backend down');
  });
});