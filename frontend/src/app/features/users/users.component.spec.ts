import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { UsersComponent } from './users.component';
import { UserService } from '../../core/services/user.service';
import { CompanyService } from '../../core/services/company.service';
import { AuthService } from '../../core/services/auth.service';
import { UserSummaryDto } from '../../core/models/user.models';
import { BranchDto } from '../../core/models/company.models';

describe('UsersComponent', () => {
  let component: UsersComponent;
  let fixture: ComponentFixture<UsersComponent>;
  let userServiceSpy: {
    getUsers: ReturnType<typeof vi.fn>;
    createUser: ReturnType<typeof vi.fn>;
    updateUser: ReturnType<typeof vi.fn>;
    toggleUserStatus: ReturnType<typeof vi.fn>;
  };
  let companyServiceSpy: { getBranches: ReturnType<typeof vi.fn> };
  let authServiceSpy: { hasPermission: ReturnType<typeof vi.fn> };

  const mockUsers: UserSummaryDto[] = [
    {
      id: 'u-1',
      tenantId: 't-1',
      branchId: 'b-1',
      username: 'admin',
      email: 'admin@restaurant.com',
      role: 'ADMIN_TENANT',
      isActive: true,
      createdAt: '2026-10-25T10:00:00Z',
    },
    {
      id: 'u-2',
      tenantId: 't-1',
      branchId: 'b-1',
      username: 'cajero1',
      email: null,
      role: 'CASHIER',
      isActive: true,
      createdAt: '2026-10-25T11:00:00Z',
    },
  ];

  const mockBranches: BranchDto[] = [
    {
      id: 'b-1',
      tenantId: 't-1',
      name: 'Sucursal Central',
      timezone: 'America/La_Paz',
      isActive: true,
      createdAt: '2026-10-25T10:00:00Z',
    },
  ];

  beforeEach(async () => {
    userServiceSpy = {
      getUsers: vi.fn().mockReturnValue(of(mockUsers)),
      createUser: vi.fn(),
      updateUser: vi.fn(),
      toggleUserStatus: vi.fn(),
    };
    companyServiceSpy = {
      getBranches: vi.fn().mockReturnValue(of(mockBranches)),
    };
    authServiceSpy = {
      hasPermission: vi.fn().mockReturnValue(true),
    };

    await TestBed.configureTestingModule({
      imports: [UsersComponent],
      providers: [
        { provide: UserService, useValue: userServiceSpy },
        { provide: CompanyService, useValue: companyServiceSpy },
        { provide: AuthService, useValue: authServiceSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(UsersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe inicializarse y cargar los colaboradores y sucursales', () => {
    expect(component).toBeTruthy();
    expect(component.users().length).toBe(2);
    expect(component.branches().length).toBe(1);
    expect(userServiceSpy.getUsers).toHaveBeenCalled();
  });

  it('debe filtrar colaboradores según el término de búsqueda', () => {
    component.searchTerm.set('cajero');
    fixture.detectChanges();

    expect(component.filteredUsers().length).toBe(1);
    expect(component.filteredUsers()[0].username).toBe('cajero1');
  });

  it('debe abrir modal de creación y guardar un nuevo colaborador', () => {
    const newUser: UserSummaryDto = {
      id: 'u-3',
      tenantId: 't-1',
      branchId: 'b-1',
      username: 'nuevo_mesero',
      email: 'mesero@test.com',
      role: 'CASHIER',
      isActive: true,
      createdAt: '2026-10-27T10:00:00Z',
    };
    userServiceSpy.createUser.mockReturnValue(of(newUser));

    component.openCreateModal();
    expect(component.isModalOpen()).toBe(true);
    expect(component.editingUser()).toBeNull();

    component.userForm.patchValue({
      username: 'nuevo_mesero',
      email: 'mesero@test.com',
      role: 'CASHIER',
      branchId: 'b-1',
      password: 'password123',
    });

    component.saveUser();

    expect(userServiceSpy.createUser).toHaveBeenCalled();
    expect(component.users().length).toBe(3);
    expect(component.isModalOpen()).toBe(false);
  });

  it('debe abrir modal de edición y actualizar colaborador existente', () => {
    const updatedUser: UserSummaryDto = {
      ...mockUsers[1],
      role: 'BRANCH_MANAGER',
    };
    userServiceSpy.updateUser.mockReturnValue(of(updatedUser));

    component.openEditModal(mockUsers[1]);
    expect(component.isModalOpen()).toBe(true);
    expect(component.editingUser()?.id).toBe('u-2');

    component.userForm.patchValue({
      role: 'BRANCH_MANAGER',
    });

    component.saveUser();

    expect(userServiceSpy.updateUser).toHaveBeenCalled();
    expect(component.users().find((u) => u.id === 'u-2')?.role).toBe('BRANCH_MANAGER');
    expect(component.isModalOpen()).toBe(false);
  });

  it('debe conmutar el estado del usuario', () => {
    const toggled = { ...mockUsers[1], isActive: false };
    userServiceSpy.toggleUserStatus.mockReturnValue(of(toggled));

    component.toggleStatus(mockUsers[1]);

    expect(userServiceSpy.toggleUserStatus).toHaveBeenCalledWith('u-2', false);
    expect(component.users().find((u) => u.id === 'u-2')?.isActive).toBe(false);
  });
});
