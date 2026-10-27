import { TestBed } from '@angular/core/testing';
import { of, firstValueFrom } from 'rxjs';
import { BranchContextService, ACTIVE_BRANCH_ID_KEY } from './branch-context.service';
import { CompanyService } from './company.service';
import { AuthService } from './auth.service';
import { BranchDto } from '../models/company.models';
import { UserDto } from '../models/auth.models';

describe('BranchContextService', () => {
  let service: BranchContextService;
  let companyServiceSpy: { getBranches: ReturnType<typeof vi.fn> };
  let authServiceSpy: { currentUser: ReturnType<typeof vi.fn> };

  const mockBranches: BranchDto[] = [
    {
      id: 'branch-1',
      tenantId: 'tenant-1',
      name: 'Sucursal Central',
      timezone: 'America/La_Paz',
      isActive: true,
      createdAt: '2026-10-25T10:00:00Z',
    },
    {
      id: 'branch-2',
      tenantId: 'tenant-1',
      name: 'Sucursal Sur',
      timezone: 'America/La_Paz',
      isActive: true,
      createdAt: '2026-10-25T10:00:00Z',
    },
  ];

  beforeEach(() => {
    localStorage.clear();
    companyServiceSpy = {
      getBranches: vi.fn(),
    };
    authServiceSpy = {
      currentUser: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        BranchContextService,
        { provide: CompanyService, useValue: companyServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
      ],
    });

    service = TestBed.inject(BranchContextService);
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('debe crearse correctamente con estado inicial vacío', () => {
    expect(service).toBeTruthy();
    expect(service.branches()).toEqual([]);
    expect(service.activeBranch()).toBeNull();
    expect(service.activeBranchId()).toBeNull();
  });

  it('debe cargar todas las sucursales para ADMIN_TENANT y seleccionar la primera por defecto', async () => {
    const adminUser: UserDto = {
      id: 'u-1',
      username: 'admin',
      email: 'admin@test.com',
      role: 'ADMIN_TENANT',
      tenantId: 'tenant-1',
      branchId: null,
      authorizedBranchIds: ['branch-1', 'branch-2'],
    };
    authServiceSpy.currentUser.mockReturnValue(adminUser);
    companyServiceSpy.getBranches.mockReturnValue(of(mockBranches));

    const branches = await firstValueFrom(service.loadBranches());
    expect(branches.length).toBe(2);
    expect(service.branches().length).toBe(2);
    expect(service.activeBranch()?.id).toBe('branch-1');
    expect(service.activeBranchId()).toBe('branch-1');
    expect(localStorage.getItem(ACTIVE_BRANCH_ID_KEY)).toBe('branch-1');
  });

  it('debe filtrar sucursales para un usuario no admin según authorizedBranchIds', async () => {
    const cashierUser: UserDto = {
      id: 'u-2',
      username: 'cajero1',
      email: null,
      role: 'CASHIER',
      tenantId: 'tenant-1',
      branchId: 'branch-2',
      authorizedBranchIds: ['branch-2'],
    };
    authServiceSpy.currentUser.mockReturnValue(cashierUser);
    companyServiceSpy.getBranches.mockReturnValue(of(mockBranches));

    const branches = await firstValueFrom(service.loadBranches());
    expect(branches.length).toBe(1);
    expect(branches[0].id).toBe('branch-2');
    expect(service.activeBranch()?.id).toBe('branch-2');
    expect(localStorage.getItem(ACTIVE_BRANCH_ID_KEY)).toBe('branch-2');
  });

  it('debe respetar la sucursal previamente guardada en localStorage si sigue autorizada', async () => {
    localStorage.setItem(ACTIVE_BRANCH_ID_KEY, 'branch-2');

    const adminUser: UserDto = {
      id: 'u-1',
      username: 'admin',
      email: 'admin@test.com',
      role: 'ADMIN_TENANT',
      tenantId: 'tenant-1',
      branchId: 'branch-1',
    };
    authServiceSpy.currentUser.mockReturnValue(adminUser);
    companyServiceSpy.getBranches.mockReturnValue(of(mockBranches));

    await firstValueFrom(service.loadBranches());
    expect(service.activeBranch()?.id).toBe('branch-2');
    expect(service.activeBranchId()).toBe('branch-2');
  });

  it('debe cambiar la sucursal activa y persistirla en localStorage con setActiveBranch', () => {
    service.setActiveBranch(mockBranches[1]);
    expect(service.activeBranch()?.id).toBe('branch-2');
    expect(service.activeBranchId()).toBe('branch-2');
    expect(localStorage.getItem(ACTIVE_BRANCH_ID_KEY)).toBe('branch-2');

    service.setActiveBranch(null);
    expect(service.activeBranch()).toBeNull();
    expect(localStorage.getItem(ACTIVE_BRANCH_ID_KEY)).toBeNull();
  });

  it('debe limpiar todo el estado y el almacenamiento local con clear()', () => {
    service.setActiveBranch(mockBranches[0]);
    service.branches.set(mockBranches);

    service.clear();

    expect(service.branches()).toEqual([]);
    expect(service.activeBranch()).toBeNull();
    expect(localStorage.getItem(ACTIVE_BRANCH_ID_KEY)).toBeNull();
  });
});
