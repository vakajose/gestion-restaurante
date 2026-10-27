import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { CompanyService } from './company.service';
import { TenantDto, BranchDto } from '../models/company.models';

describe('CompanyService', () => {
  let service: CompanyService;
  let httpTesting: HttpTestingController;

  const mockTenant: TenantDto = {
    id: 'tenant-1',
    legalName: 'Restaurante Test S.R.L.',
    nitOrTaxId: '12345678',
    slug: 'restaurante-test',
    isActive: true,
    createdAt: '2026-10-27T10:00:00Z',
    updatedAt: '2026-10-27T10:00:00Z',
  };

  const mockBranches: BranchDto[] = [
    {
      id: 'branch-1',
      tenantId: 'tenant-1',
      name: 'Central',
      address: 'Calle 1',
      phone: '1234',
      timezone: 'America/La_Paz',
      isActive: true,
      createdAt: '2026-10-27T10:00:00Z',
    },
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        CompanyService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(CompanyService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('debe obtener la información de la empresa mediante GET /api/v1/company', () => {
    service.getCompany().subscribe((res) => {
      expect(res).toEqual(mockTenant);
    });

    const req = httpTesting.expectOne('/api/v1/company');
    expect(req.request.method).toBe('GET');
    req.flush(mockTenant);
  });

  it('debe actualizar los datos de la empresa mediante PUT /api/v1/company', () => {
    service
      .updateCompany({
        legalName: 'Nuevo Nombre',
        nitOrTaxId: '87654321',
        slug: 'nuevo-slug',
      })
      .subscribe((res) => {
        expect(res.legalName).toBe('Nuevo Nombre');
      });

    const req = httpTesting.expectOne('/api/v1/company');
    expect(req.request.method).toBe('PUT');
    req.flush({ ...mockTenant, legalName: 'Nuevo Nombre' });
  });

  it('debe listar sucursales mediante GET /api/v1/branches', () => {
    service.getBranches().subscribe((res) => {
      expect(res.length).toBe(1);
      expect(res[0].name).toBe('Central');
    });

    const req = httpTesting.expectOne('/api/v1/branches');
    expect(req.request.method).toBe('GET');
    req.flush(mockBranches);
  });

  it('debe crear una nueva sucursal mediante POST /api/v1/branches', () => {
    service
      .createBranch({
        name: 'Sucursal Sur',
        address: 'Calle 15',
        timezone: 'America/La_Paz',
      })
      .subscribe((res) => {
        expect(res.name).toBe('Sucursal Sur');
      });

    const req = httpTesting.expectOne('/api/v1/branches');
    expect(req.request.method).toBe('POST');
    req.flush({ ...mockBranches[0], id: 'branch-2', name: 'Sucursal Sur' });
  });

  it('debe conmutar el estado de la sucursal mediante PATCH /api/v1/branches/:id/status', () => {
    service.toggleBranchStatus('branch-1', false).subscribe((res) => {
      expect(res.isActive).toBe(false);
    });

    const req = httpTesting.expectOne('/api/v1/branches/branch-1/status');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ isActive: false });
    req.flush({ ...mockBranches[0], isActive: false });
  });
});
