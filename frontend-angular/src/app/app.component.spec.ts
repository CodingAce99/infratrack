import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { By } from '@angular/platform-browser';

import { AppComponent } from './app.component';

describe('AppComponent (app shell)', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('renders the Infratrack brand text in the shell', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    const brand = fixture.debugElement.query(By.css('.app-shell__brand'));
    expect(brand).not.toBeNull();
    expect(brand.nativeElement.textContent).toContain('Infratrack');
  });

  it('exposes a router outlet so route components can render under the shell', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    const outlet = fixture.debugElement.query(By.css('router-outlet'));
    expect(outlet).not.toBeNull();
  });
});