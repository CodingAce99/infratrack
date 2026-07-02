import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { HeaderComponent } from './header.component';

describe('HeaderComponent', () => {
  function setup(inputs: { assetCount: number; isConnected?: boolean; canManage?: boolean }) {
    const fixture = TestBed.createComponent(HeaderComponent);
    fixture.componentRef.setInput('assetCount', inputs.assetCount);
    if (inputs.isConnected !== undefined) {
      fixture.componentRef.setInput('isConnected', inputs.isConnected);
    }
    if (inputs.canManage !== undefined) {
      fixture.componentRef.setInput('canManage', inputs.canManage);
    }
    fixture.detectChanges();
    return fixture;
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HeaderComponent],
    }).compileComponents();
  });

  it('renders the provided asset count', () => {
    const fixture = setup({ assetCount: 7 });
    const count = fixture.debugElement.query(By.css('[data-testid="asset-count"]'));

    expect(count).not.toBeNull();
    expect(count.nativeElement.textContent).toContain('7');
  });

  it('shows a connected indicator when isConnected is true', () => {
    const fixture = setup({ assetCount: 3, isConnected: true });
    const dot = fixture.debugElement.query(By.css('[data-testid="connection-indicator"]'));

    expect(dot).not.toBeNull();
    expect(dot.nativeElement.getAttribute('data-connected')).toBe('true');
  });

  it('shows a disconnected indicator when isConnected is false', () => {
    const fixture = setup({ assetCount: 3, isConnected: false });
    const dot = fixture.debugElement.query(By.css('[data-testid="connection-indicator"]'));

    expect(dot.nativeElement.getAttribute('data-connected')).toBe('false');
  });

  it('defaults to disconnected when isConnected is not provided', () => {
    const fixture = setup({ assetCount: 0 });
    const dot = fixture.debugElement.query(By.css('[data-testid="connection-indicator"]'));

    expect(dot.nativeElement.getAttribute('data-connected')).toBe('false');
  });

  it('emits addAsset when the add button is clicked', () => {
    const fixture = setup({ assetCount: 2 });
    let emitted = 0;
    fixture.componentInstance.addAsset.subscribe(() => (emitted += 1));

    const button = fixture.debugElement.query(By.css('[data-testid="add-asset-button"]'));
    button.nativeElement.click();
    fixture.detectChanges();

    expect(emitted).toBe(1);
  });

  it('hides the add button when canManage is false', () => {
    const fixture = setup({ assetCount: 2, canManage: false });
    const button = fixture.debugElement.query(By.css('[data-testid="add-asset-button"]'));

    expect(button).toBeNull();
  });

  it('renders without any injected API dependency (pure coordination)', () => {
    const fixture = setup({ assetCount: 1 });
    expect(fixture.componentInstance).toBeTruthy();
  });
});