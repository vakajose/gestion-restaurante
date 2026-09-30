import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';

describe('authInterceptor', () => {
  let httpClient: HttpClient;
  let httpTesting: HttpTestingController;
  let authService: AuthService;

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    httpClient = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService);
  });

  afterEach(() => {
    httpTesting.verify();
    localStorage.clear();
  });

  it('no debe agregar Authorization header si la URL es el endpoint de login', () => {
    authService.token.set('some-jwt-token');

    httpClient.post('/api/v1/auth/login', { login: 'admin', password: '123' }).subscribe();

    const req = httpTesting.expectOne('/api/v1/auth/login');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
  });

  it('debe agregar Authorization: Bearer <token> a peticiones protegidas cuando existe token', () => {
    authService.token.set('valid-test-token');

    httpClient.get('/api/v1/pos/products').subscribe();

    const req = httpTesting.expectOne('/api/v1/pos/products');
    expect(req.request.headers.has('Authorization')).toBe(true);
    expect(req.request.headers.get('Authorization')).toBe('Bearer valid-test-token');
    req.flush([]);
  });

  it('no debe agregar Authorization header si el usuario no tiene token', () => {
    authService.token.set(null);

    httpClient.get('/api/v1/public/health').subscribe();

    const req = httpTesting.expectOne('/api/v1/public/health');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({ status: 'UP' });
  });
});
