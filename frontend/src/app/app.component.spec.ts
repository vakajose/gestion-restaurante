import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { AppComponent } from './app.component';
import { ThemeService } from './core/services/theme.service';
import { AuthService } from './core/services/auth.service';
import { UserDto } from './core/models/auth.models';

describe('AppComponent', () => {
  const mockUser: UserDto = {
    id: 'user-1',
    username: 'admin',
    email: 'admin@restaurant.com',
    role: 'ADMIN_TENANT',
    tenantId: 'tenant-1',
    branchId: 'branch-1',
  };

  beforeEach(async () => {
    localStorage.clear();
    document.documentElement.classList.remove('dark');

    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        provideRouter([{ path: 'auth/login', component: AppComponent }]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
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

  it('should render theme toggle button and toggle theme on click when authenticated', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const authService = TestBed.inject(AuthService);
    authService.currentUser.set(mockUser);
    fixture.detectChanges();

    const themeService = TestBed.inject(ThemeService);
    const initialMode = themeService.isDarkMode();

    const button = fixture.nativeElement.querySelector('[data-testid="theme-toggle-btn"]') as HTMLButtonElement;
    expect(button).toBeTruthy();

    button.click();
    fixture.detectChanges();
    expect(themeService.isDarkMode()).toBe(!initialMode);
  });

  it('should not display the sidebar when user is unauthenticated', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const authService = TestBed.inject(AuthService);
    authService.currentUser.set(null);
    fixture.detectChanges();

    const aside = fixture.nativeElement.querySelector('aside');
    expect(aside).toBeFalsy();
  });

  it('should call authService.logout when clicking logout in authenticated sidebar', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const authService = TestBed.inject(AuthService);
    authService.currentUser.set(mockUser);
    const logoutSpy = vi.spyOn(authService, 'logout');
    fixture.detectChanges();

    const logoutBtn = fixture.nativeElement.querySelector('[data-testid="logout-btn"]') as HTMLButtonElement;
    expect(logoutBtn).toBeTruthy();

    logoutBtn.click();
    expect(logoutSpy).toHaveBeenCalled();
  });
});
