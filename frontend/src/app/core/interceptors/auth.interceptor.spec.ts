import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { authInterceptor, resetAuthInterceptorState } from './auth.interceptor';
import { AuthService } from '../services/auth.service';
import { BranchContextService } from '../services/branch-context.service';
import { BranchDto } from '../models/company.models';

describe('authInterceptor (Silent Refresh & Headers)', () => {
  let httpClient: HttpClient;
  let httpTesting: HttpTestingController;
  let authService: AuthService;
  let branchContextService: BranchContextService;
  let routerSpy: { navigate: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    localStorage.clear();
    resetAuthInterceptorState();
    routerSpy = { navigate: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        BranchContextService,
        { provide: Router, useValue: routerSpy },
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    httpClient = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService);
    branchContextService = TestBed.inject(BranchContextService);
  });

  afterEach(() => {
    httpTesting.verify();
    localStorage.clear();
    resetAuthInterceptorState();
  });

  it('no debe agregar Authorization ni X-Branch-Id si la URL es un endpoint público de auth u onboarding', () => {
    authService.token.set('some-jwt-token');
    const branch: BranchDto = {
      id: 'b-123',
      tenantId: 't-123',
      name: 'Central',
      timezone: 'America/La_Paz',
      isActive: true,
      createdAt: '2026-10-25T10:00:00Z',
    };
    branchContextService.setActiveBranch(branch);

    httpClient.post('/api/v1/auth/login', { login: 'admin', password: '123' }).subscribe();
    const loginReq = httpTesting.expectOne('/api/v1/auth/login');
    expect(loginReq.request.headers.has('Authorization')).toBe(false);
    expect(loginReq.request.headers.has('X-Branch-Id')).toBe(false);
    loginReq.flush({});

    httpClient.get('/api/v1/auth/check-slug?slug=test').subscribe();
    const slugReq = httpTesting.expectOne('/api/v1/auth/check-slug?slug=test');
    expect(slugReq.request.headers.has('Authorization')).toBe(false);
    slugReq.flush({ available: true });
  });

  it('debe agregar Authorization: Bearer <token> y X-Branch-Id cuando ambos están presentes', () => {
    authService.token.set('valid-test-token');
    const branch: BranchDto = {
      id: 'branch-uuid-456',
      tenantId: 't-123',
      name: 'Sucursal Central',
      timezone: 'America/La_Paz',
      isActive: true,
      createdAt: '2026-10-25T10:00:00Z',
    };
    branchContextService.setActiveBranch(branch);

    httpClient.get('/api/v1/pos/products').subscribe();

    const req = httpTesting.expectOne('/api/v1/pos/products');
    expect(req.request.headers.get('Authorization')).toBe('Bearer valid-test-token');
    expect(req.request.headers.get('X-Branch-Id')).toBe('branch-uuid-456');
    req.flush([]);
  });

  it('debe realizar Silent Refresh ante 401 y encolar peticiones concurrentes sin duplicar POST /api/v1/auth/refresh', () => {
    authService.token.set('expired-access-token');
    authService.refreshTokenValue.set('valid-refresh-token-1');

    let res1: unknown;
    let res2: unknown;

    // Disparar 2 peticiones concurrentes que fallarán con 401
    httpClient.get('/api/v1/pos/products').subscribe((r) => (res1 = r));
    httpClient.get('/api/v1/inventory/stock').subscribe((r) => (res2 = r));

    const initialReq1 = httpTesting.expectOne('/api/v1/pos/products');
    const initialReq2 = httpTesting.expectOne('/api/v1/inventory/stock');

    // La primera responde 401 -> inicia el refresh
    initialReq1.flush({ message: 'Token expired' }, { status: 401, statusText: 'Unauthorized' });
    // La segunda también responde 401 mientras el refresh está en curso -> se encola
    initialReq2.flush({ message: 'Token expired' }, { status: 401, statusText: 'Unauthorized' });

    // Debe haberse emitido exactamente UNA petición a /api/v1/auth/refresh
    const refreshReqs = httpTesting.match('/api/v1/auth/refresh');
    expect(refreshReqs.length).toBe(1);
    expect(refreshReqs[0].request.body).toEqual({ refreshToken: 'valid-refresh-token-1' });

    // Resolver el refresh con nuevo token rotado
    refreshReqs[0].flush({
      token: 'fresh-access-token-2',
      refreshToken: 'fresh-refresh-token-2',
    });

    // Ambas peticiones originales deben reintentarse automáticamente con el nuevo token
    const retriedReq1 = httpTesting.expectOne('/api/v1/pos/products');
    const retriedReq2 = httpTesting.expectOne('/api/v1/inventory/stock');

    expect(retriedReq1.request.headers.get('Authorization')).toBe('Bearer fresh-access-token-2');
    expect(retriedReq2.request.headers.get('Authorization')).toBe('Bearer fresh-access-token-2');

    retriedReq1.flush([{ id: 'prod-1' }]);
    retriedReq2.flush([{ id: 'stock-1' }]);

    expect(res1).toEqual([{ id: 'prod-1' }]);
    expect(res2).toEqual([{ id: 'stock-1' }]);
    expect(authService.getToken()).toBe('fresh-access-token-2');
    expect(authService.getRefreshToken()).toBe('fresh-refresh-token-2');
  });

  it('debe ejecutar logout y redirigir a /auth/login si el refreshToken falla con 401', () => {
    authService.token.set('expired-access-token');
    authService.refreshTokenValue.set('revoked-refresh-token');

    let caughtError: unknown;
    httpClient.get('/api/v1/pos/products').subscribe({
      error: (err) => (caughtError = err),
    });

    const initialReq = httpTesting.expectOne('/api/v1/pos/products');
    initialReq.flush({}, { status: 401, statusText: 'Unauthorized' });

    const refreshReq = httpTesting.expectOne('/api/v1/auth/refresh');
    refreshReq.flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(caughtError).toBeTruthy();
    expect(authService.getToken()).toBeNull();
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/auth/login']);
  });
});
