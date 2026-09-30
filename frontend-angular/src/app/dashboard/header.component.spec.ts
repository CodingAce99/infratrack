import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

import { HeaderComponent } from './header.component';

describe('HeaderComponent', () => {
  function setup(inputs: {
    assetCount: number;
    isConnected?: boolean;
    canManage?: boolean;
    username?: string | null;
  }) {
    const fixture = TestBed.createComponent(HeaderComponent);
    fixture.componentRef.setInput('assetCount', inputs.assetCount);
    if (inputs.isConnected !== undefined) {
      fixture.componentRef.setInput('isConnected', inputs.isConnected);
    }
    if (inputs.canManage !== undefined) {
      fixture.componentRef.setInput('canManage', inputs.canManage);
    }
    if (inputs.username !== undefined) {
      fixture.componentRef.setInput('username', inputs.username);
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
    const fixture = setup({ assetCount: 2, canManage: true });
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

  it('renders the current username and logout button when username is provided', () => {
    const fixture = setup({ assetCount: 2, username: 'admin' });

    const user = fixture.debugElement.query(By.css('[data-testid="current-user"]'));
    const logout = fixture.debugElement.query(By.css('[data-testid="logout-button"]'));

    expect(user).not.toBeNull();
    expect(user.nativeElement.textContent).toContain('admin');
    expect(logout).not.toBeNull();
  });

  it('hides the current user and logout button when username is not provided', () => {
    const fixture = setup({ assetCount: 2, username: null });

    expect(fixture.debugElement.query(By.css('[data-testid="current-user"]'))).toBeNull();
    expect(fixture.debugElement.query(By.css('[data-testid="logout-button"]'))).toBeNull();
  });

  it('emits logout when the logout button is clicked', () => {
    const fixture = setup({ assetCount: 2, username: 'viewer' });
    let emitted = 0;
    fixture.componentInstance.logout.subscribe(() => (emitted += 1));

    fixture.debugElement.query(By.css('[data-testid="logout-button"]')).nativeElement.click();
    fixture.detectChanges();

    expect(emitted).toBe(1);
  });

});
