import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { AppComponent } from './app.component';
import { ThemeService } from './core/services/theme.service';
import { AuthService } from './core/services/auth.service';
import { BranchContextService } from './core/services/branch-context.service';
import { UserDto } from './core/models/auth.models';
import { BranchDto } from './core/models/company.models';

describe('AppComponent', () => {
  const mockUser: UserDto = {
    id: 'user-1',
    username: 'admin',
    email: 'admin@restaurant.com',
    role: 'ADMIN_TENANT',
    tenantId: 'tenant-1',
    branchId: 'branch-1',
  };

  const branch1: BranchDto = {
    id: 'branch-1',
    tenantId: 'tenant-1',
    name: 'Sucursal Central',
    timezone: 'America/La_Paz',
    isActive: true,
    createdAt: '2026-10-25T10:00:00Z',
  };

  const branch2: BranchDto = {
    id: 'branch-2',
    tenantId: 'tenant-1',
    name: 'Sucursal Sur',
    timezone: 'America/La_Paz',
    isActive: true,
    createdAt: '2026-10-25T10:00:00Z',
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

  it('should call authService.logout and clear branch context when clicking logout in authenticated sidebar', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const authService = TestBed.inject(AuthService);
    const branchContext = TestBed.inject(BranchContextService);
    authService.currentUser.set(mockUser);
    const logoutSpy = vi.spyOn(authService, 'logout');
    const clearSpy = vi.spyOn(branchContext, 'clear');
    fixture.detectChanges();

    const logoutBtn = fixture.nativeElement.querySelector('[data-testid="logout-btn"]') as HTMLButtonElement;
    expect(logoutBtn).toBeTruthy();

    logoutBtn.click();
    expect(clearSpy).toHaveBeenCalled();
    expect(logoutSpy).toHaveBeenCalled();
  });

  it('debe filtrar elementos del sidebar según permisos RBAC del usuario', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const authService = TestBed.inject(AuthService);

    // Usuario Cajero: solo permiso pos:access
    const cashierUser: UserDto = {
      ...mockUser,
      role: 'CASHIER',
      permissions: ['pos:access'],
    };
    authService.currentUser.set(cashierUser);
    fixture.detectChanges();

    const posNav = fixture.nativeElement.querySelector('[data-testid="nav-pos"]');
    const companyNav = fixture.nativeElement.querySelector('[data-testid="nav-company"]');
    const financeNav = fixture.nativeElement.querySelector('[data-testid="nav-finance"]');

    expect(posNav).toBeTruthy();
    expect(companyNav).toBeFalsy();
    expect(financeNav).toBeFalsy();

    // Usuario Admin: acceso a todo
    authService.currentUser.set(mockUser);
    fixture.detectChanges();

    const posNavAdmin = fixture.nativeElement.querySelector('[data-testid="nav-pos"]');
    const companyNavAdmin = fixture.nativeElement.querySelector('[data-testid="nav-company"]');
    expect(posNavAdmin).toBeTruthy();
    expect(companyNavAdmin).toBeTruthy();
  });

  it('debe renderizar la sucursal fija sin dropdown cuando solo hay una sucursal autorizada', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const authService = TestBed.inject(AuthService);
    const branchContext = TestBed.inject(BranchContextService);

    authService.currentUser.set(mockUser);
    branchContext.branches.set([branch1]);
    branchContext.setActiveBranch(branch1);
    fixture.detectChanges();

    const singleBranch = fixture.nativeElement.querySelector('[data-testid="single-branch-name"]');
    const dropdownBtn = fixture.nativeElement.querySelector('[data-testid="branch-selector-btn"]');

    expect(singleBranch).toBeTruthy();
    expect(singleBranch.textContent.trim()).toBe('Sucursal Central');
    expect(dropdownBtn).toBeFalsy();
  });

  it('debe renderizar el dropdown interactivo y permitir cambiar de sucursal cuando hay múltiples sucursales', () => {
    const fixture = TestBed.createComponent(AppComponent);
    const authService = TestBed.inject(AuthService);
    const branchContext = TestBed.inject(BranchContextService);

    authService.currentUser.set(mockUser);
    branchContext.branches.set([branch1, branch2]);
    branchContext.setActiveBranch(branch1);
    fixture.detectChanges();

    const dropdownBtn = fixture.nativeElement.querySelector('[data-testid="branch-selector-btn"]') as HTMLButtonElement;
    expect(dropdownBtn).toBeTruthy();

    const activeName = fixture.nativeElement.querySelector('[data-testid="active-branch-name"]');
    expect(activeName.textContent.trim()).toBe('Sucursal Central');

    // Desplegar menú
    dropdownBtn.click();
    fixture.detectChanges();

    const menu = fixture.nativeElement.querySelector('[data-testid="branch-dropdown-menu"]');
    expect(menu).toBeTruthy();

    // Seleccionar branch 2
    const optionBranch2 = fixture.nativeElement.querySelector('[data-testid="branch-option-branch-2"]') as HTMLButtonElement;
    expect(optionBranch2).toBeTruthy();
    optionBranch2.click();
    fixture.detectChanges();

    expect(branchContext.activeBranchId()).toBe('branch-2');
    expect(fixture.nativeElement.querySelector('[data-testid="branch-dropdown-menu"]')).toBeFalsy();
  });
});
