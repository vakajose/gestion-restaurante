import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { LoginComponent } from './login.component';
import { AuthService } from '../../../core/services/auth.service';
import { ThemeService } from '../../../core/services/theme.service';
import { LoginResponse, UserDto } from '../../../core/models/auth.models';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authServiceSpy: { login: ReturnType<typeof vi.fn> };
  let router: Router;

  const mockUser: UserDto = {
    id: 'user-uuid-1',
    username: 'admin',
    email: 'admin@restaurant.com',
    role: 'ADMIN_TENANT',
    tenantId: 'tenant-uuid-1',
    branchId: 'branch-uuid-1',
  };

  const mockLoginResponse: LoginResponse = {
    token: 'fake-jwt-token',
    tokenType: 'Bearer',
    user: mockUser,
  };

  beforeEach(async () => {
    localStorage.clear();
    authServiceSpy = {
      login: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        ThemeService,
        { provide: AuthService, useValue: authServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('debe crearse e inicializarse correctamente con el formulario vacío', () => {
    expect(component).toBeTruthy();
    expect(component.loginForm.valid).toBe(false);
    expect(component.loginForm.get('login')?.value).toBe('');
    expect(component.loginForm.get('password')?.value).toBe('');
    expect(component.isLoading()).toBe(false);
    expect(component.errorMessage()).toBeNull();
  });

  it('debe validar que los campos login y password sean obligatorios', () => {
    const loginCtrl = component.loginForm.controls.login;
    const passCtrl = component.loginForm.controls.password;

    expect(loginCtrl.valid).toBe(false);
    expect(loginCtrl.errors?.['required']).toBe(true);

    loginCtrl.setValue('ad');
    expect(loginCtrl.valid).toBe(false);
    expect(loginCtrl.errors?.['minlength']).toBeTruthy();

    loginCtrl.setValue('admin');
    expect(loginCtrl.valid).toBe(true);

    expect(passCtrl.valid).toBe(false);
    expect(passCtrl.errors?.['required']).toBe(true);

    passCtrl.setValue('123');
    expect(passCtrl.valid).toBe(false);
    expect(passCtrl.errors?.['minlength']).toBeTruthy();

    passCtrl.setValue('admin123');
    expect(passCtrl.valid).toBe(true);

    expect(component.loginForm.valid).toBe(true);
  });

  it('debe deshabilitar el botón de submit si el formulario es inválido', () => {
    const submitBtn = fixture.nativeElement.querySelector('[data-testid="login-submit-btn"]') as HTMLButtonElement;
    expect(submitBtn.disabled).toBe(true);

    component.loginForm.setValue({
      login: 'admin',
      password: 'admin123',
    });
    fixture.detectChanges();

    expect(submitBtn.disabled).toBe(false);
  });

  it('debe conmutar la visibilidad de la contraseña', () => {
    expect(component.showPassword()).toBe(false);
    component.togglePasswordVisibility();
    expect(component.showPassword()).toBe(true);
    component.togglePasswordVisibility();
    expect(component.showPassword()).toBe(false);
  });

  it('debe cargar credenciales al pulsar el botón Demo Admin', () => {
    const demoAdminBtn = fixture.nativeElement.querySelector('[data-testid="demo-admin-btn"]') as HTMLButtonElement;
    demoAdminBtn.click();
    fixture.detectChanges();

    expect(component.loginForm.value).toEqual({
      login: 'admin',
      password: 'admin123',
    });
    expect(component.loginForm.valid).toBe(true);
  });

  it('debe cargar credenciales al pulsar el botón Demo Cajero', () => {
    const demoCashierBtn = fixture.nativeElement.querySelector('[data-testid="demo-cashier-btn"]') as HTMLButtonElement;
    demoCashierBtn.click();
    fixture.detectChanges();

    expect(component.loginForm.value).toEqual({
      login: 'cajero1',
      password: 'cajero123',
    });
    expect(component.loginForm.valid).toBe(true);
  });

  it('debe invocar authService.login y redirigir a /pos si la autenticación es exitosa', () => {
    authServiceSpy.login.mockReturnValue(of(mockLoginResponse));

    component.fillDemoAdmin();
    component.onSubmit();

    expect(authServiceSpy.login).toHaveBeenCalledWith('admin', 'admin123');
    expect(component.isLoading()).toBe(false);
    expect(router.navigate).toHaveBeenCalledWith(['/pos']);
  });

  it('debe mostrar mensaje amigable cuando las credenciales son incorrectas (401)', () => {
    const errorResponse = new HttpErrorResponse({
      status: 401,
      statusText: 'Unauthorized',
      error: { message: 'Bad credentials' },
    });
    authServiceSpy.login.mockReturnValue(throwError(() => errorResponse));

    component.fillDemoAdmin();
    component.onSubmit();
    fixture.detectChanges();

    expect(component.isLoading()).toBe(false);
    expect(component.errorMessage()).toBeTruthy();

    const alert = fixture.nativeElement.querySelector('[role="alert"]');
    expect(alert).toBeTruthy();
  });

  it('debe mostrar el detalle de RFC 7807 ProblemDetail si el backend lo retorna', () => {
    const problemDetailResponse = new HttpErrorResponse({
      status: 401,
      statusText: 'Unauthorized',
      error: {
        type: 'about:blank',
        title: 'Authentication Failure',
        status: 401,
        detail: 'Credenciales inválidas para el usuario especificado',
      },
    });
    authServiceSpy.login.mockReturnValue(throwError(() => problemDetailResponse));

    component.fillDemoAdmin();
    component.onSubmit();
    fixture.detectChanges();

    expect(component.errorMessage()).toBe('Credenciales inválidas para el usuario especificado');
  });

  it('debe permitir cerrar la alerta de error con clearError', () => {
    component.errorMessage.set('Error de prueba');
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeTruthy();

    component.clearError();
    fixture.detectChanges();

    expect(component.errorMessage()).toBeNull();
    expect(fixture.nativeElement.querySelector('[role="alert"]')).toBeFalsy();
  });

  it('debe alternar el tema al pulsar el botón de cambio de tema', () => {
    const themeService = TestBed.inject(ThemeService);
    const toggleSpy = vi.spyOn(themeService, 'toggleTheme');

    const themeBtn = fixture.nativeElement.querySelector('[data-testid="theme-toggle-btn"]') as HTMLButtonElement;
    expect(themeBtn).toBeTruthy();

    themeBtn.click();
    expect(toggleSpy).toHaveBeenCalled();
  });
});
