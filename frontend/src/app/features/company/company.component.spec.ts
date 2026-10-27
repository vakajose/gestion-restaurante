import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { CompanyComponent } from './company.component';
import { CompanyService } from '../../core/services/company.service';
import { ThemeService } from '../../core/services/theme.service';
import { TenantDto, BranchDto } from '../../core/models/company.models';

describe('CompanyComponent', () => {
  let component: CompanyComponent;
  let fixture: ComponentFixture<CompanyComponent>;
  let companyServiceSpy: {
    getCompany: ReturnType<typeof vi.fn>;
    updateCompany: ReturnType<typeof vi.fn>;
    getBranches: ReturnType<typeof vi.fn>;
    createBranch: ReturnType<typeof vi.fn>;
    updateBranch: ReturnType<typeof vi.fn>;
    toggleBranchStatus: ReturnType<typeof vi.fn>;
  };

  const mockTenant: TenantDto = {
    id: 'tenant-1',
    legalName: 'Restaurante Don Pollo',
    nitOrTaxId: '12345678',
    slug: 'don-pollo',
    isActive: true,
    createdAt: '2026-10-27T10:00:00Z',
    updatedAt: '2026-10-27T10:00:00Z',
  };

  const mockBranches: BranchDto[] = [
    {
      id: 'branch-1',
      tenantId: 'tenant-1',
      name: 'Sucursal Central',
      address: 'Av. 16 de Julio #1490',
      phone: '22440000',
      timezone: 'America/La_Paz',
      isActive: true,
      createdAt: '2026-10-27T10:00:00Z',
    },
  ];

  beforeEach(async () => {
    companyServiceSpy = {
      getCompany: vi.fn().mockReturnValue(of(mockTenant)),
      updateCompany: vi.fn(),
      getBranches: vi.fn().mockReturnValue(of(mockBranches)),
      createBranch: vi.fn(),
      updateBranch: vi.fn(),
      toggleBranchStatus: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [CompanyComponent],
      providers: [
        provideRouter([]),
        ThemeService,
        { provide: CompanyService, useValue: companyServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CompanyComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe crearse y cargar datos de empresa y sucursales en ngOnInit', () => {
    expect(component).toBeTruthy();
    expect(companyServiceSpy.getCompany).toHaveBeenCalled();
    expect(companyServiceSpy.getBranches).toHaveBeenCalled();
    expect(component.company()).toEqual(mockTenant);
    expect(component.branches()).toEqual(mockBranches);
    expect(component.companyForm.get('legalName')?.value).toBe('Restaurante Don Pollo');
    expect(component.companyForm.get('slug')?.value).toBe('don-pollo');
  });

  it('debe conmutar pestañas entre empresa y sucursales', () => {
    expect(component.activeTab()).toBe('company');
    component.activeTab.set('branches');
    expect(component.activeTab()).toBe('branches');
  });

  it('debe guardar cambios en los datos de la empresa', () => {
    companyServiceSpy.updateCompany.mockReturnValue(
      of({ ...mockTenant, legalName: 'Don Pollo Editado' })
    );

    component.companyForm.patchValue({
      legalName: 'Don Pollo Editado',
      nitOrTaxId: '12345678',
      slug: 'don-pollo',
    });

    component.saveCompany();

    expect(companyServiceSpy.updateCompany).toHaveBeenCalledWith({
      legalName: 'Don Pollo Editado',
      nitOrTaxId: '12345678',
      slug: 'don-pollo',
    });
    expect(component.message()?.type).toBe('success');
  });

  it('debe abrir modal para crear sucursal y persistir', () => {
    const newBranch: BranchDto = {
      id: 'branch-2',
      tenantId: 'tenant-1',
      name: 'Sucursal Calacoto',
      address: 'Calle 15',
      phone: '22790000',
      timezone: 'America/La_Paz',
      isActive: true,
      createdAt: '2026-10-27T10:00:00Z',
    };
    companyServiceSpy.createBranch.mockReturnValue(of(newBranch));

    component.openCreateBranchModal();
    expect(component.isBranchModalOpen()).toBe(true);
    expect(component.editingBranch()).toBeNull();

    component.branchForm.patchValue({
      name: 'Sucursal Calacoto',
      address: 'Calle 15',
      phone: '22790000',
      timezone: 'America/La_Paz',
    });

    component.saveBranch();

    expect(companyServiceSpy.createBranch).toHaveBeenCalledWith({
      name: 'Sucursal Calacoto',
      address: 'Calle 15',
      phone: '22790000',
      timezone: 'America/La_Paz',
    });
    expect(component.isBranchModalOpen()).toBe(false);
  });

  it('debe conmutar el estado activo/inactivo de una sucursal', () => {
    const toggled = { ...mockBranches[0], isActive: false };
    companyServiceSpy.toggleBranchStatus.mockReturnValue(of(toggled));

    component.toggleBranchStatus(mockBranches[0]);

    expect(companyServiceSpy.toggleBranchStatus).toHaveBeenCalledWith('branch-1', false);
    expect(component.branches()[0].isActive).toBe(false);
  });
});
