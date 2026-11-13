import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { apiBaseUrlInterceptor } from './api-base-url.interceptor';

describe('apiBaseUrlInterceptor (Runtime env.js)', () => {
  let http: HttpClient;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    window.__ENV__ = { API_BASE_URL: '' };
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiBaseUrlInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
    window.__ENV__ = { API_BASE_URL: '' };
  });

  it('mantiene rutas relativas cuando API_BASE_URL está vacío', () => {
    http.get('/api/v1/catalog/dishes').subscribe();
    const req = httpTesting.expectOne('/api/v1/catalog/dishes');
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('antepone API_BASE_URL sin trailing slash cuando se inyecta en runtime', () => {
    window.__ENV__ = { API_BASE_URL: 'https://api.staging.restaurante.bo///' };
    http.get('/api/v1/auth/login').subscribe();
    const req = httpTesting.expectOne('https://api.staging.restaurante.bo/api/v1/auth/login');
    expect(req.request.method).toBe('GET');
    req.flush({});
  });

  it('no modifica URLs absolutas externas', () => {
    window.__ENV__ = { API_BASE_URL: 'https://api.staging.restaurante.bo' };
    http.get('https://cdn.example.com/logo.png').subscribe();
    const req = httpTesting.expectOne('https://cdn.example.com/logo.png');
    req.flush({});
  });
});
