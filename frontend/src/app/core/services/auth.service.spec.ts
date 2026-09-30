import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService, AUTH_TOKEN_KEY, AUTH_USER_KEY } from './auth.service';
import { LoginResponse, UserDto } from '../models/auth.models';

describe('AuthService', () => {
  let service: AuthService;
  let httpTesting: HttpTestingController;
  let routerSpy: { navigate: ReturnType<typeof vi.fn> };

  const mockUser: UserDto = {
    id: '123e4567-e89b-12d3-a456-426614174000',
    username: 'admin',
    email: 'admin@restaurant.com',
    role: 'ADMIN_TENANT',
    tenantId: '123e4567-e89b-12d3-a456-426614174001',
    branchId: '123e4567-e89b-12d3-a456-426614174002',
  };

  const mockLoginResponse: LoginResponse = {
    token: 'jwt-test-token-12345',
    tokenType: 'Bearer',
    user: mockUser,
  };

  beforeEach(() => {
    localStorage.clear();
    routerSpy = { navigate: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: routerSpy },
      ],
    });

    service = TestBed.inject(AuthService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    localStorage.clear();
  });

  it('debe inicializarse sin usuario autenticado cuando localStorage está vacío', () => {
    expect(service.currentUser()).toBeNull();
    expect(service.isAuthenticated()).toBe(false);
    expect(service.getToken()).toBeNull();
  });

  it('debe autenticar mediante login, actualizar Signals y persistir en localStorage', () => {
    let resultResponse: LoginResponse | undefined;

    service.login('admin', 'admin123').subscribe((res) => {
      resultResponse = res;
    });

    const req = httpTesting.expectOne('/api/v1/auth/login');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ login: 'admin', password: 'admin123' });

    req.flush(mockLoginResponse);

    expect(resultResponse).toEqual(mockLoginResponse);
    expect(service.currentUser()).toEqual(mockUser);
    expect(service.isAuthenticated()).toBe(true);
    expect(service.getToken()).toBe('jwt-test-token-12345');
    expect(localStorage.getItem(AUTH_TOKEN_KEY)).toBe('jwt-test-token-12345');
    expect(localStorage.getItem(AUTH_USER_KEY)).toBe(JSON.stringify(mockUser));
  });

  it('debe limpiar Signals, remover token de localStorage y redirigir a /auth/login al hacer logout', () => {
    // Simular sesión iniciada
    service.login('admin', 'admin123').subscribe();
    const req = httpTesting.expectOne('/api/v1/auth/login');
    req.flush(mockLoginResponse);

    expect(service.isAuthenticated()).toBe(true);

    service.logout();

    expect(service.currentUser()).toBeNull();
    expect(service.isAuthenticated()).toBe(false);
    expect(service.getToken()).toBeNull();
    expect(localStorage.getItem(AUTH_TOKEN_KEY)).toBeNull();
    expect(localStorage.getItem(AUTH_USER_KEY)).toBeNull();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/auth/login']);
  });

  it('debe restaurar la sesión automáticamente desde localStorage al crearse el servicio', () => {
    localStorage.setItem(AUTH_TOKEN_KEY, 'persisted-jwt-token');
    localStorage.setItem(AUTH_USER_KEY, JSON.stringify(mockUser));

    // Crear nueva instancia de TestBed para simular reinicio de la app
    const freshService = TestBed.runInInjectionContext(() => new AuthService());

    expect(freshService.currentUser()).toEqual(mockUser);
    expect(freshService.isAuthenticated()).toBe(true);
    expect(freshService.getToken()).toBe('persisted-jwt-token');
  });

  it('debe limpiar almacenamiento corrupto si el JSON en localStorage es inválido', () => {
    localStorage.setItem(AUTH_TOKEN_KEY, 'persisted-jwt-token');
    localStorage.setItem(AUTH_USER_KEY, 'invalid-json-{broken');

    const freshService = TestBed.runInInjectionContext(() => new AuthService());

    expect(freshService.currentUser()).toBeNull();
    expect(freshService.isAuthenticated()).toBe(false);
    expect(localStorage.getItem(AUTH_TOKEN_KEY)).toBeNull();
  });
});
