import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AppComponent } from './app.component';
import { ThemeService } from './core/services/theme.service';

describe('AppComponent', () => {
  beforeEach(async () => {
    localStorage.clear();
    document.documentElement.classList.remove('dark');

    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  afterEach(() => {
    localStorage.clear();
    document.documentElement.classList.remove('dark');
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should have the title Gestión Restaurante', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app.title).toBe('Gestión Restaurante');
  });

  it('should render theme toggle button and toggle theme on click', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const themeService = TestBed.inject(ThemeService);
    const initialMode = themeService.isDarkMode();

    const button = fixture.nativeElement.querySelector('[data-testid="theme-toggle-btn"]') as HTMLButtonElement;
    expect(button).toBeTruthy();

    button.click();
    fixture.detectChanges();
    expect(themeService.isDarkMode()).toBe(!initialMode);
  });
});
