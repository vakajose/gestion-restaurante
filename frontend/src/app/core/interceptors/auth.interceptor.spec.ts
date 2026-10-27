import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';
import { BranchContextService } from '../services/branch-context.service';
import { BranchDto } from '../models/company.models';

describe('authInterceptor', () => {
  let httpClient: HttpClient;
  let httpTesting: HttpTestingController;
  let authService: AuthService;
  let branchContextService: BranchContextService;

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        BranchContextService,
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
  });

  it('no debe agregar Authorization ni X-Branch-Id si la URL es el endpoint de login', () => {
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

    const req = httpTesting.expectOne('/api/v1/auth/login');
    expect(req.request.headers.has('Authorization')).toBe(false);
    expect(req.request.headers.has('X-Branch-Id')).toBe(false);
    req.flush({});
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

  it('no debe agregar Authorization ni X-Branch-Id si no existen en el contexto', () => {
    authService.token.set(null);
    branchContextService.setActiveBranch(null);

    httpClient.get('/api/v1/public/health').subscribe();

    const req = httpTesting.expectOne('/api/v1/public/health');
    expect(req.request.headers.has('Authorization')).toBe(false);
    expect(req.request.headers.has('X-Branch-Id')).toBe(false);
    req.flush({ status: 'UP' });
  });
});
