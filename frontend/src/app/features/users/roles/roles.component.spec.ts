import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { RolesComponent } from './roles.component';
import { UserService } from '../../../core/services/user.service';
import { CompanyService } from '../../../core/services/company.service';
import { AuthService } from '../../../core/services/auth.service';
import {
  PermissionCatalogItemDto,
  RoleDto,
  UserSummaryDto,
} from '../../../core/models/user.models';
import { BranchDto } from '../../../core/models/company.models';

describe('RolesComponent', () => {
  let component: RolesComponent;
  let fixture: ComponentFixture<RolesComponent>;
  let userServiceSpy: {
    getRoles: ReturnType<typeof vi.fn>;
    getPermissionsCatalog: ReturnType<typeof vi.fn>;
    createRole: ReturnType<typeof vi.fn>;
    updateRole: ReturnType<typeof vi.fn>;
    deleteRole: ReturnType<typeof vi.fn>;
    getUsers: ReturnType<typeof vi.fn>;
    updateUser: ReturnType<typeof vi.fn>;
    resetUserPassword: ReturnType<typeof vi.fn>;
    revokeUserSessions: ReturnType<typeof vi.fn>;
  };
  let companyServiceSpy: { getBranches: ReturnType<typeof vi.fn> };

  const mockCatalog: PermissionCatalogItemDto[] = [
    {
      code: 'pos:access',
      name: 'Acceso a POS',
      description: 'Permite ingresar al terminal POS',
      module: 'POS',
    },
    {
      code: 'pos:discount_apply',
      name: 'Aplicar Descuentos',
      description: 'Permite aplicar descuentos en comandas',
      module: 'POS',
    },
    {
      code: 'inventory:read',
      name: 'Ver Inventario',
      description: 'Consulta de existencias y Kardex',
      module: 'INVENTORY',
    },
  ];

  const mockRoles: RoleDto[] = [
    {
      id: 'role-admin',
      tenantId: 't-1',
      code: 'ADMIN_TENANT',
      name: 'Administrador de Empresa',
      description: 'Control total inmutable',
      isSystem: true,
      permissions: ['*'],
    },
    {
      id: 'role-cashier',
      tenantId: 't-1',
      code: 'CASHIER',
      name: 'Cajero',
      description: 'Operación de punto de venta',
      isSystem: true,
      permissions: ['pos:access'],
    },
  ];

  const mockUsers: UserSummaryDto[] = [
    {
      id: 'u-2',
      tenantId: 't-1',
      branchId: 'b-1',
      username: 'cajero_noche',
      email: 'noche@test.com',
      role: 'CASHIER',
      isActive: true,
      createdAt: '2026-10-28T10:00:00Z',
    },
  ];

  const mockBranches: BranchDto[] = [
    {
      id: 'b-1',
      tenantId: 't-1',
      name: 'Sucursal Central',
      timezone: 'America/La_Paz',
      isActive: true,
      createdAt: '2026-10-28T10:00:00Z',
    },
  ];

  beforeEach(async () => {
    userServiceSpy = {
      getRoles: vi.fn().mockReturnValue(of(mockRoles)),
      getPermissionsCatalog: vi.fn().mockReturnValue(of(mockCatalog)),
      createRole: vi.fn(),
      updateRole: vi.fn(),
      deleteRole: vi.fn(),
      getUsers: vi.fn().mockReturnValue(of(mockUsers)),
      updateUser: vi.fn(),
      resetUserPassword: vi.fn(),
      revokeUserSessions: vi.fn(),
    };

    companyServiceSpy = {
      getBranches: vi.fn().mockReturnValue(of(mockBranches)),
    };

    await TestBed.configureTestingModule({
      imports: [RolesComponent],
      providers: [
        provideRouter([]),
        { provide: UserService, useValue: userServiceSpy },
        { provide: CompanyService, useValue: companyServiceSpy },
        {
          provide: AuthService,
          useValue: {
            hasPermission: vi.fn().mockReturnValue(true),
            hasRole: vi.fn().mockReturnValue(true),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(RolesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe cargar el catálogo de permisos agrupados por módulo y los roles del tenant', () => {
    expect(component).toBeTruthy();
    expect(component.roles().length).toBe(2);
    expect(component.groupedPermissions().length).toBe(2);
    expect(component.selectedRole()?.code).toBe('ADMIN_TENANT');
    expect(component.isSelectedRoleImmutable()).toBe(true);
  });

  it('debe impedir modificar permisos del rol inmutable ADMIN_TENANT pero permitir editar CASHIER', () => {
    // ADMIN_TENANT es inmutable
    component.togglePermission('pos:discount_apply');
    expect(component.selectedPermissions()).toEqual(['*']);

    // Seleccionar CASHIER
    component.selectRole(mockRoles[1]);
    expect(component.isSelectedRoleImmutable()).toBe(false);
    expect(component.selectedPermissions()).toEqual(['pos:access']);

    component.togglePermission('pos:discount_apply');
    expect(component.selectedPermissions()).toContain('pos:discount_apply');
  });

  it('debe permitir crear un nuevo rol personalizado y guardarlo', () => {
    const createdCustomRole: RoleDto = {
      id: 'role-custom-1',
      tenantId: 't-1',
      code: 'CAJERO_SENIOR',
      name: 'Cajero Senior',
      description: 'Cajero con permiso de descuento',
      isSystem: false,
      permissions: ['pos:access', 'pos:discount_apply'],
    };
    userServiceSpy.createRole.mockReturnValue(of(createdCustomRole));

    component.startCreateNewRole();
    expect(component.isCreatingNewRole()).toBe(true);

    component.roleForm.patchValue({
      name: 'Cajero Senior',
      description: 'Cajero con permiso de descuento',
    });
    component.togglePermission('pos:discount_apply');
    component.saveRole();

    expect(userServiceSpy.createRole).toHaveBeenCalledWith({
      name: 'Cajero Senior',
      code: undefined,
      description: 'Cajero con permiso de descuento',
      permissions: ['pos:access', 'pos:discount_apply'],
    });
    expect(component.roles().length).toBe(3);
    expect(component.selectedRole()?.code).toBe('CAJERO_SENIOR');
  });

  it('debe permitir abrir el modal de seguridad de colaborador, resetear su contraseña y revocar sesiones', () => {
    userServiceSpy.resetUserPassword.mockReturnValue(
      of({
        userId: 'u-2',
        mustChangePassword: true,
        resetAt: '2026-10-28T12:00:00Z',
      })
    );
    userServiceSpy.revokeUserSessions.mockReturnValue(of(undefined));

    component.openCollaboratorSecurityModal(mockUsers[0]);
    expect(component.isCollaboratorModalOpen()).toBe(true);

    component.temporaryResetPassword.set('Temp1234');
    component.triggerDelegatedPasswordReset();
    expect(userServiceSpy.resetUserPassword).toHaveBeenCalledWith('u-2', 'Temp1234');

    component.triggerRevokeUserSessions(mockUsers[0]);
    expect(userServiceSpy.revokeUserSessions).toHaveBeenCalledWith('u-2');
  });
});
