import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { OnboardingService } from './onboarding.service';
import {
  CheckAvailabilityResultDto,
  OnboardingRequest,
  OnboardingResultDto,
} from '../models/company.models';

describe('OnboardingService', () => {
  let service: OnboardingService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        OnboardingService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(OnboardingService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('debe consultar la disponibilidad del slug en GET /api/v1/auth/check-slug', () => {
    const mockRes: CheckAvailabilityResultDto = {
      available: false,
      suggestedSlug: 'bella-napoli-2',
    };

    let result: CheckAvailabilityResultDto | undefined;
    service.checkSlugAvailability('bella-napoli').subscribe((res) => {
      result = res;
    });

    const req = httpTesting.expectOne(
      (r) => r.url === '/api/v1/auth/check-slug' && r.params.get('slug') === 'bella-napoli'
    );
    expect(req.request.method).toBe('GET');
    req.flush(mockRes);

    expect(result).toEqual(mockRes);
  });

  it('debe consultar disponibilidad combinada en GET /api/v1/auth/onboarding/check-availability', () => {
    const mockRes: CheckAvailabilityResultDto = {
      available: true,
      slugAvailable: true,
      nitAvailable: true,
    };

    let result: CheckAvailabilityResultDto | undefined;
    service.checkAvailability({ slug: 'mi-restaurante', nit: '10203040' }).subscribe((res) => {
      result = res;
    });

    const req = httpTesting.expectOne(
      (r) =>
        r.url === '/api/v1/auth/onboarding/check-availability' &&
        r.params.get('slug') === 'mi-restaurante' &&
        r.params.get('nit') === '10203040'
    );
    expect(req.request.method).toBe('GET');
    req.flush(mockRes);

    expect(result).toEqual(mockRes);
  });

  it('debe registrar el onboarding en POST /api/v1/auth/onboarding', () => {
    const payload: OnboardingRequest = {
      company: {
        legalName: 'Pizzería Bella Napoli',
        slug: 'bella-napoli',
        nitOrTaxId: null,
      },
      branch: {
        name: 'Sucursal Principal',
        address: 'Av. Central 123',
        latitude: -16.5,
        longitude: -68.15,
        phone: '70012345',
        timezone: 'America/La_Paz',
        currency: 'BOB',
      },
      businessTemplate: 'FAST_FOOD',
      includeDemoCatalog: true,
      adminUser: {
        fullName: 'Carlos Mendoza',
        username: 'admin',
        email: 'carlos@napoli.com',
        password: 'password123',
      },
    };

    const mockResult: OnboardingResultDto = {
      tenantId: 'tenant-1',
      branchId: 'branch-1',
      adminUserId: 'user-1',
      slug: 'bella-napoli',
      accessToken: 'jwt-onboarding-token',
      token: 'jwt-onboarding-token',
      user: {
        id: 'user-1',
        username: 'admin',
        email: 'carlos@napoli.com',
        role: 'ADMIN_TENANT',
        tenantId: 'tenant-1',
        branchId: 'branch-1',
      },
    };

    let result: OnboardingResultDto | undefined;
    service.registerOnboarding(payload).subscribe((res) => {
      result = res;
    });

    const req = httpTesting.expectOne('/api/v1/auth/onboarding');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush(mockResult);

    expect(result).toEqual(mockResult);
  });
});
