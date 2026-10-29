import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { UserService } from '../../../core/services/user.service';
import { CompanyService } from '../../../core/services/company.service';
import { AuthService } from '../../../core/services/auth.service';
import {
  RoleDto,
  PermissionCatalogItemDto,
  CreateRoleRequest,
  UpdateRoleRequest,
  UserSummaryDto,
} from '../../../core/models/user.models';
import { BranchDto } from '../../../core/models/company.models';

export interface PermissionGroup {
  module: string;
  label: string;
  items: PermissionCatalogItemDto[];
}

@Component({
  selector: 'app-roles',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './roles.component.html',
})
export class RolesComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  private readonly companyService = inject(CompanyService);
  readonly authService = inject(AuthService);

  readonly roles = signal<RoleDto[]>([]);
  readonly permissionsCatalog = signal<PermissionCatalogItemDto[]>([]);
  readonly users = signal<UserSummaryDto[]>([]);
  readonly branches = signal<BranchDto[]>([]);

  readonly selectedRole = signal<RoleDto | null>(null);
  readonly selectedPermissions = signal<string[]>([]);
  readonly isCreatingNewRole = signal<boolean>(false);
  readonly isLoading = signal<boolean>(false);
  readonly isSaving = signal<boolean>(false);
  readonly message = signal<{ type: 'success' | 'error'; text: string } | null>(null);

  // Modal de Asignación a Colaborador / Vigencia / Sucursales / Reset Password
  readonly isCollaboratorModalOpen = signal<boolean>(false);
  readonly activeCollaborator = signal<UserSummaryDto | null>(null);
  readonly selectedAuthorizedBranchIds = signal<string[]>([]);
  readonly temporaryResetPassword = signal<string>('');

  readonly roleForm = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    code: [''],
    description: [''],
  });

  readonly collaboratorForm = this.fb.group({
    role: ['CASHIER', [Validators.required]],
    defaultBranchId: [''],
    roleValidUntil: [''],
  });

  private readonly moduleLabels: Record<string, string> = {
    POS: '💳 Punto de Venta (POS)',
    INVENTORY: '📦 Inventario y Kardex',
    FINANCE: '💰 Finanzas y Caja',
    CATALOG: '🍽️ Catálogo y Recetas',
    CONFIG: '⚙️ Configuración y Sucursales',
    COMPANY: '🏢 Empresa y Sucursales',
    USERS: '👥 Usuarios y Seguridad RBAC',
    ADMIN: '🛡️ Administración y Auditoría',
    REPORTS: '📊 Reportes y Analítica',
  };

  readonly groupedPermissions = computed<PermissionGroup[]>(() => {
    const catalog = this.permissionsCatalog();
    const map = new Map<string, PermissionCatalogItemDto[]>();

    for (const item of catalog) {
      const mod = item.module || 'GENERAL';
      const list = map.get(mod) ?? [];
      list.push(item);
      map.set(mod, list);
    }

    return Array.from(map.entries()).map(([module, items]) => ({
      module,
      label: this.moduleLabels[module] ?? module,
      items,
    }));
  });

  readonly isSelectedRoleImmutable = computed<boolean>(() => {
    const role = this.selectedRole();
    if (!role) {
      return false;
    }
    return role.code === 'ADMIN_TENANT' || role.code === 'SUPERADMIN';
  });

  ngOnInit(): void {
    this.loadAllData();
  }

  loadAllData(): void {
    this.isLoading.set(true);
    this.userService.getPermissionsCatalog().subscribe({
      next: (catalog) => {
        this.permissionsCatalog.set(catalog);
        this.loadRoles();
      },
      error: () => {
        this.loadRoles();
      },
    });

    this.userService.getUsers().subscribe({
      next: (uList) => this.users.set(uList),
      error: () => {
        // Ignorar si no hay usuarios adicionales
      },
    });

    this.companyService.getBranches().subscribe({
      next: (bList) => this.branches.set(bList),
      error: () => {
        // Ignorar si no hay sucursales adicionales
      },
    });
  }

  loadRoles(): void {
    this.userService.getRoles().subscribe({
      next: (roleList) => {
        this.roles.set(roleList);
        this.isLoading.set(false);
        if (roleList.length > 0 && !this.selectedRole() && !this.isCreatingNewRole()) {
          this.selectRole(roleList[0]);
        }
      },
      error: () => {
        this.isLoading.set(false);
        this.message.set({ type: 'error', text: 'Error al cargar los roles del restaurante.' });
      },
    });
  }

  selectRole(role: RoleDto): void {
    this.isCreatingNewRole.set(false);
    this.selectedRole.set(role);
    this.selectedPermissions.set([...(role.permissions ?? [])]);
    this.roleForm.reset({
      name: role.name,
      code: role.code,
      description: role.description ?? '',
    });
    this.message.set(null);
  }

  startCreateNewRole(): void {
    this.selectedRole.set(null);
    this.isCreatingNewRole.set(true);
    this.selectedPermissions.set(['pos:access']);
    this.roleForm.reset({
      name: '',
      code: '',
      description: '',
    });
    this.message.set(null);
  }

  hasSelectedPermission(code: string): boolean {
    const perms = this.selectedPermissions();
    return perms.includes('*') || perms.includes(code);
  }

  togglePermission(code: string): void {
    if (this.isSelectedRoleImmutable()) {
      return;
    }
    this.selectedPermissions.update((current) => {
      if (current.includes(code)) {
        return current.filter((p) => p !== code);
      }
      return [...current, code];
    });
  }

  saveRole(): void {
    if (this.roleForm.invalid || this.isSaving() || this.isSelectedRoleImmutable()) {
      this.roleForm.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);
    this.message.set(null);

    const val = this.roleForm.getRawValue();
    const perms = this.selectedPermissions();

    if (this.isCreatingNewRole()) {
      const createReq: CreateRoleRequest = {
        name: (val.name ?? '').trim(),
        code: val.code && val.code.trim().length > 0 ? val.code.trim().toUpperCase() : undefined,
        description: val.description?.trim() || undefined,
        permissions: perms,
      };

      this.userService.createRole(createReq).subscribe({
        next: (created) => {
          this.roles.update((prev) => [...prev, created]);
          this.isSaving.set(false);
          this.selectRole(created);
          this.message.set({
            type: 'success',
            text: `Rol personalizado '${created.name}' creado exitosamente.`,
          });
        },
        error: (err) => {
          this.isSaving.set(false);
          const detail = err.error?.detail || err.error?.message || 'Error al crear el rol.';
          this.message.set({ type: 'error', text: detail });
        },
      });
    } else {
      const currentRole = this.selectedRole();
      if (!currentRole) {
        this.isSaving.set(false);
        return;
      }
      const updateReq: UpdateRoleRequest = {
        name: (val.name ?? currentRole.name).trim(),
        description: val.description?.trim() || undefined,
        permissions: perms,
      };

      this.userService.updateRole(currentRole.id, updateReq).subscribe({
        next: (updated) => {
          this.roles.update((prev) => prev.map((r) => (r.id === updated.id ? updated : r)));
          this.selectedRole.set(updated);
          this.isSaving.set(false);
          this.message.set({
            type: 'success',
            text: `Permisos del rol '${updated.name}' actualizados correctamente.`,
          });
        },
        error: (err) => {
          this.isSaving.set(false);
          const detail = err.error?.detail || err.error?.message || 'Error al actualizar el rol.';
          this.message.set({ type: 'error', text: detail });
        },
      });
    }
  }

  deleteSelectedRole(): void {
    const role = this.selectedRole();
    if (!role || role.isSystem || this.isSaving()) {
      return;
    }

    this.isSaving.set(true);
    this.userService.deleteRole(role.id).subscribe({
      next: () => {
        this.roles.update((prev) => prev.filter((r) => r.id !== role.id));
        this.isSaving.set(false);
        const remaining = this.roles();
        if (remaining.length > 0) {
          this.selectRole(remaining[0]);
        } else {
          this.selectedRole.set(null);
        }
        this.message.set({
          type: 'success',
          text: `Rol '${role.name}' eliminado exitosamente.`,
        });
      },
      error: (err) => {
        this.isSaving.set(false);
        const detail = err.error?.detail || err.error?.message || 'No se pudo eliminar el rol.';
        this.message.set({ type: 'error', text: detail });
      },
    });
  }

  openCollaboratorSecurityModal(user: UserSummaryDto): void {
    this.activeCollaborator.set(user);
    this.selectedAuthorizedBranchIds.set(
      user.authorizedBranchIds && user.authorizedBranchIds.length > 0
        ? [...user.authorizedBranchIds]
        : user.branchId
          ? [user.branchId]
          : []
    );
    this.temporaryResetPassword.set('');
    this.collaboratorForm.reset({
      role: user.role,
      defaultBranchId: user.branchId ?? '',
      roleValidUntil: user.roleValidUntil ? user.roleValidUntil.substring(0, 16) : '',
    });
    this.isCollaboratorModalOpen.set(true);
  }

  closeCollaboratorModal(): void {
    this.isCollaboratorModalOpen.set(false);
    this.activeCollaborator.set(null);
  }

  toggleAuthorizedBranch(branchId: string): void {
    this.selectedAuthorizedBranchIds.update((list) => {
      if (list.includes(branchId)) {
        return list.filter((id) => id !== branchId);
      }
      return [...list, branchId];
    });
  }

  isBranchAuthorized(branchId: string): boolean {
    return this.selectedAuthorizedBranchIds().includes(branchId);
  }

  saveCollaboratorAssignment(): void {
    const user = this.activeCollaborator();
    if (!user || this.collaboratorForm.invalid || this.isSaving()) {
      return;
    }

    this.isSaving.set(true);
    const val = this.collaboratorForm.getRawValue();
    const validUntilIso =
      val.roleValidUntil && val.roleValidUntil.trim().length > 0
        ? new Date(val.roleValidUntil).toISOString()
        : null;

    this.userService
      .updateUser(user.id, {
        username: user.username,
        email: user.email ?? undefined,
        role: val.role ?? user.role,
        branchId: val.defaultBranchId || null,
        authorizedBranchIds: this.selectedAuthorizedBranchIds(),
        roleValidUntil: validUntilIso,
      })
      .subscribe({
        next: (updated) => {
          this.users.update((prev) => prev.map((u) => (u.id === updated.id ? updated : u)));
          this.isSaving.set(false);
          this.closeCollaboratorModal();
          this.message.set({
            type: 'success',
            text: `Asignación de rol, vigencia y sucursales actualizada para '${updated.username}'.`,
          });
        },
        error: (err) => {
          this.isSaving.set(false);
          const detail = err.error?.detail || 'Error al actualizar la asignación del colaborador.';
          this.message.set({ type: 'error', text: detail });
        },
      });
  }

  triggerDelegatedPasswordReset(): void {
    const user = this.activeCollaborator();
    const tempPass = this.temporaryResetPassword().trim();
    if (!user || tempPass.length < 6 || this.isSaving()) {
      return;
    }

    this.isSaving.set(true);
    this.userService.resetUserPassword(user.id, tempPass).subscribe({
      next: () => {
        this.isSaving.set(false);
        this.temporaryResetPassword.set('');
        this.message.set({
          type: 'success',
          text: `Contraseña temporal asignada a '${user.username}'. Se le exigirá cambiarla en su próximo inicio de sesión.`,
        });
      },
      error: (err) => {
        this.isSaving.set(false);
        const detail = err.error?.detail || 'Error al resetear la contraseña del colaborador.';
        this.message.set({ type: 'error', text: detail });
      },
    });
  }

  triggerRevokeUserSessions(user: UserSummaryDto): void {
    this.userService.revokeUserSessions(user.id).subscribe({
      next: () => {
        this.message.set({
          type: 'success',
          text: `Todas las sesiones activas de '${user.username}' han sido revocadas inmediatamente.`,
        });
      },
      error: () => {
        this.message.set({
          type: 'error',
          text: `No se pudieron revocar las sesiones de '${user.username}'.`,
        });
      },
    });
  }
}
