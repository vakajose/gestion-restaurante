import { TestBed } from '@angular/core/testing';
import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { errorInterceptor } from './error.interceptor';
import { ProblemDetail } from '../models/problem-detail.model';

describe('errorInterceptor', () => {
  let httpClient: HttpClient;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
      ],
    });

    httpClient = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('debe permitir peticiones exitosas sin alteración', () => {
    const mockData = { id: '123', name: 'Test' };
    let result: unknown;

    httpClient.get('/api/test').subscribe((data) => {
      result = data;
    });

    const req = httpTesting.expectOne('/api/test');
    req.flush(mockData);

    expect(result).toEqual(mockData);
  });

  it('debe capturar y enriquecer un error con payload RFC 7807 desde el backend', () => {
    const backendProblem: ProblemDetail = {
      type: 'https://restaurant.com/errors/conflict',
      title: 'Conflicto de recurso',
      status: 409,
      detail: 'Ya existe una sucursal con ese nombre',
      errorCode: 'RESOURCE_CONFLICT',
      timestamp: '2026-10-28T12:00:00Z',
      tenantId: 'tenant-123',
    };

    let caughtError: HttpErrorResponse | null = null;

    httpClient.post('/api/branches', {}).subscribe({
      next: () => { throw new Error('No debió tener éxito'); },
      error: (err: HttpErrorResponse) => {
        caughtError = err;
      },
    });

    const req = httpTesting.expectOne('/api/branches');
    req.flush(backendProblem, { status: 409, statusText: 'Conflict' });

    expect(caughtError).not.toBeNull();
    const enriched = caughtError as unknown as { problemDetail: ProblemDetail; errorCode: string };
    expect(enriched.problemDetail).toBeDefined();
    expect(enriched.problemDetail.status).toBe(409);
    expect(enriched.problemDetail.title).toBe('Conflicto de recurso');
    expect(enriched.problemDetail.detail).toBe('Ya existe una sucursal con ese nombre');
    expect(enriched.problemDetail.errorCode).toBe('RESOURCE_CONFLICT');
    expect(enriched.errorCode).toBe('RESOURCE_CONFLICT');
  });

  it('debe manejar errores de validación con invalidParams', () => {
    const backendProblem: ProblemDetail = {
      type: 'https://restaurant.com/errors/validation',
      title: 'Error de validación',
      status: 400,
      detail: 'Uno o más campos presentan inconsistencias',
      errorCode: 'VALIDATION_ERROR',
      invalidParams: {
        amount: 'El monto debe ser mayor a 0',
        description: 'La descripción es obligatoria',
      },
    };

    let caughtError: HttpErrorResponse | null = null;

    httpClient.post('/api/expenses', {}).subscribe({
      next: () => { throw new Error('No debió tener éxito'); },
      error: (err: HttpErrorResponse) => {
        caughtError = err;
      },
    });

    const req = httpTesting.expectOne('/api/expenses');
    req.flush(backendProblem, { status: 400, statusText: 'Bad Request' });

    expect(caughtError).not.toBeNull();
    const enriched = caughtError as unknown as { problemDetail: ProblemDetail };
    expect(enriched.problemDetail.invalidParams).toBeDefined();
    expect(enriched.problemDetail.invalidParams!['amount']).toBe('El monto debe ser mayor a 0');
  });

  it('debe sintetizar un ProblemDetail estándar ante errores de conexión (status 0)', () => {
    let caughtError: HttpErrorResponse | null = null;

    httpClient.get('/api/offline-endpoint').subscribe({
      next: () => { throw new Error('No debió tener éxito'); },
      error: (err: HttpErrorResponse) => {
        caughtError = err;
      },
    });

    const req = httpTesting.expectOne('/api/offline-endpoint');
    req.error(new ProgressEvent('Network error'), { status: 0, statusText: 'Unknown Error' });

    expect(caughtError).not.toBeNull();
    const enriched = caughtError as unknown as { problemDetail: ProblemDetail; errorCode: string };
    expect(enriched.problemDetail).toBeDefined();
    expect(enriched.problemDetail.status).toBe(0);
    expect(enriched.problemDetail.title).toBe('Error de conexión');
    expect(enriched.problemDetail.errorCode).toBe('NETWORK_ERROR');
  });

  it('debe generar un fallback estándar para status 500 sin cuerpo estructurado', () => {
    let caughtError: HttpErrorResponse | null = null;

    httpClient.get('/api/server-error').subscribe({
      next: () => { throw new Error('No debió tener éxito'); },
      error: (err: HttpErrorResponse) => {
        caughtError = err;
      },
    });

    const req = httpTesting.expectOne('/api/server-error');
    req.flush('Error interno no formateado', { status: 500, statusText: 'Internal Server Error' });

    expect(caughtError).not.toBeNull();
    const enriched = caughtError as unknown as { problemDetail: ProblemDetail };
    expect(enriched.problemDetail.status).toBe(500);
    expect(enriched.problemDetail.title).toBe('Error interno del servidor');
    expect(enriched.problemDetail.errorCode).toBe('INTERNAL_SERVER_ERROR');
  });
});
