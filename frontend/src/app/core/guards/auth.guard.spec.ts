import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { authGuard, guestGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';
import { UserDto } from '../models/auth.models';

describe('Auth Guards', () => {
  let authService: AuthService;
  let router: Router;

  const mockRoute = {} as ActivatedRouteSnapshot;
  const mockState = {} as RouterStateSnapshot;

  const mockUser: UserDto = {
    id: 'user-id-1',
    username: 'admin',
    email: 'admin@restaurant.com',
    role: 'ADMIN_TENANT',
    tenantId: 'tenant-1',
    branchId: 'branch-1',
  };

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: Router,
          useValue: {
            parseUrl: vi.fn((url: string) => url as unknown as UrlTree),
          },
        },
      ],
    });

    authService = TestBed.inject(AuthService);
    router = TestBed.inject(Router);
  });

  afterEach(() => {
    localStorage.clear();
  });

  describe('authGuard', () => {
    it('debe permitir acceso si el usuario está autenticado', () => {
      authService.currentUser.set(mockUser);

      const result = TestBed.runInInjectionContext(() => authGuard(mockRoute, mockState));
      expect(result).toBe(true);
    });

    it('debe redirigir a /auth/login si el usuario no está autenticado', () => {
      authService.currentUser.set(null);

      const result = TestBed.runInInjectionContext(() => authGuard(mockRoute, mockState));
      expect(router.parseUrl).toHaveBeenCalledWith('/auth/login');
      expect(result).toBe('/auth/login' as unknown as UrlTree);
    });
  });

  describe('guestGuard', () => {
    it('debe permitir acceso a /auth/login si no está autenticado', () => {
      authService.currentUser.set(null);

      const result = TestBed.runInInjectionContext(() => guestGuard(mockRoute, mockState));
      expect(result).toBe(true);
    });

    it('debe redirigir automáticamente a /pos si el usuario ya está autenticado', () => {
      authService.currentUser.set(mockUser);

      const result = TestBed.runInInjectionContext(() => guestGuard(mockRoute, mockState));
      expect(router.parseUrl).toHaveBeenCalledWith('/pos');
      expect(result).toBe('/pos' as unknown as UrlTree);
    });
  });
});
