import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { ConfirmDialogComponent } from './confirm-dialog.component';

describe('ConfirmDialogComponent', () => {
  let fixture: ReturnType<typeof TestBed.createComponent<ConfirmDialogComponent>>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConfirmDialogComponent],
    }).compileComponents();
  });

  function setup(message: string) {
    fixture = TestBed.createComponent(ConfirmDialogComponent);
    fixture.componentRef.setInput('message', message);
    fixture.detectChanges();
  }

  it('renders the provided message', () => {
    setup('Delete asset web-server-01?');
    const message = fixture.debugElement.query(By.css('[data-testid="confirm-message"]'));

    expect(message.nativeElement.textContent).toContain('Delete asset web-server-01?');
  });

  it('emits confirm when the confirm button is clicked', () => {
    setup('Are you sure?');
    const spy = jasmine.createSpy('confirm');
    fixture.componentInstance.confirm.subscribe(spy);

    const button = fixture.debugElement.query(By.css('[data-testid="confirm-button"]'));
    button.nativeElement.click();

    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('emits cancel when the cancel button is clicked', () => {
    setup('Are you sure?');
    const spy = jasmine.createSpy('cancel');
    fixture.componentInstance.cancel.subscribe(spy);

    const button = fixture.debugElement.query(By.css('[data-testid="cancel-button"]'));
    button.nativeElement.click();

    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('emits cancel when the backdrop is clicked (spec: cancel leaves asset unchanged)', () => {
    setup('Delete?');
    const spy = jasmine.createSpy('cancel');
    fixture.componentInstance.cancel.subscribe(spy);

    const backdrop = fixture.debugElement.query(By.css('[data-testid="confirm-backdrop"]'));
    backdrop.nativeElement.click();

    expect(spy).toHaveBeenCalledTimes(1);
  });

  it('does not emit cancel when a click lands inside the dialog body', () => {
    setup('Delete?');
    const spy = jasmine.createSpy('cancel');
    fixture.componentInstance.cancel.subscribe(spy);

    const body = fixture.debugElement.query(By.css('[data-testid="confirm-dialog"]'));
    body.nativeElement.click();

    expect(spy).not.toHaveBeenCalled();
  });

  it('renders without any injected API dependency (pure presentational)', () => {
    setup('Delete?');
    expect(fixture.componentInstance).toBeTruthy();
  });
});
