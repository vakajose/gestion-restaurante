import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { UserService } from '../../core/services/user.service';
import { CompanyService } from '../../core/services/company.service';
import { UserSummaryDto, CreateUserRequest, UpdateUserRequest } from '../../core/models/user.models';
import { BranchDto } from '../../core/models/company.models';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './users.component.html',
})
export class UsersComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  private readonly companyService = inject(CompanyService);
  readonly authService = inject(AuthService);

  readonly users = signal<UserSummaryDto[]>([]);
  readonly branches = signal<BranchDto[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly isSaving = signal<boolean>(false);
  readonly message = signal<{ type: 'success' | 'error'; text: string } | null>(null);

  readonly searchTerm = signal<string>('');
  readonly isModalOpen = signal<boolean>(false);
  readonly editingUser = signal<UserSummaryDto | null>(null);

  readonly availableRoles = [
    { code: 'ADMIN_TENANT', label: 'Administrador de Empresa' },
    { code: 'BRANCH_MANAGER', label: 'Gerente de Sucursal' },
    { code: 'CASHIER', label: 'Cajero / Punto de Venta' },
    { code: 'KITCHEN', label: 'Personal de Cocina (KDS)' },
  ];

  readonly userForm = this.fb.group({
    username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
    email: ['', [Validators.email]],
    role: ['CASHIER', [Validators.required]],
    branchId: [''],
    password: [''],
  });

  readonly filteredUsers = computed(() => {
    const term = this.searchTerm().toLowerCase().trim();
    const list = this.users();
    if (!term) {
      return list;
    }
    return list.filter(
      (u) =>
        u.username.toLowerCase().includes(term) ||
        (u.email && u.email.toLowerCase().includes(term)) ||
        u.role.toLowerCase().includes(term)
    );
  });

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.isLoading.set(true);
    this.userService.getUsers().subscribe({
      next: (list) => {
        this.users.set(list);
        this.loadBranches();
      },
      error: () => {
        this.isLoading.set(false);
        this.message.set({ type: 'error', text: 'Error al cargar los usuarios del restaurante.' });
      },
    });
  }

  loadBranches(): void {
    this.companyService.getBranches().subscribe({
      next: (branchesList) => {
        this.branches.set(branchesList);
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
      },
    });
  }

  openCreateModal(): void {
    this.editingUser.set(null);
    this.message.set(null);
    const defaultBranchId = this.branches().length > 0 ? this.branches()[0].id : '';
    this.userForm.reset({
      username: '',
      email: '',
      role: 'CASHIER',
      branchId: defaultBranchId,
      password: '',
    });
    this.userForm.controls.password.setValidators([Validators.required, Validators.minLength(6)]);
    this.userForm.controls.password.updateValueAndValidity();
    this.isModalOpen.set(true);
  }

  openEditModal(user: UserSummaryDto): void {
    this.editingUser.set(user);
    this.message.set(null);
    this.userForm.reset({
      username: user.username,
      email: user.email ?? '',
      role: user.role,
      branchId: user.branchId ?? '',
      password: '',
    });
    this.userForm.controls.password.clearValidators();
    this.userForm.controls.password.updateValueAndValidity();
    this.isModalOpen.set(true);
  }

  closeModal(): void {
    this.isModalOpen.set(false);
    this.editingUser.set(null);
  }

  saveUser(): void {
    if (this.userForm.invalid || this.isSaving()) {
      this.userForm.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);
    this.message.set(null);

    const formVal = this.userForm.getRawValue();
    const branchIdVal = formVal.branchId && formVal.branchId.trim().length > 0 ? formVal.branchId : null;
    const emailVal = formVal.email && formVal.email.trim().length > 0 ? formVal.email.trim() : undefined;

    const editing = this.editingUser();
    if (editing) {
      const updateReq: UpdateUserRequest = {
        username: formVal.username ?? '',
        email: emailVal,
        role: formVal.role ?? 'CASHIER',
        branchId: branchIdVal,
        newPassword: formVal.password && formVal.password.trim().length >= 6 ? formVal.password.trim() : undefined,
      };

      this.userService.updateUser(editing.id, updateReq).subscribe({
        next: (updated) => {
          this.users.update((prev) => prev.map((u) => (u.id === updated.id ? updated : u)));
          this.isSaving.set(false);
          this.closeModal();
          this.message.set({ type: 'success', text: `Colaborador '${updated.username}' actualizado exitosamente.` });
        },
        error: (err) => {
          this.isSaving.set(false);
          const detail = err.error?.detail || err.error?.message || 'Error al actualizar el colaborador.';
          this.message.set({ type: 'error', text: detail });
        },
      });
    } else {
      const createReq: CreateUserRequest = {
        username: formVal.username ?? '',
        email: emailVal,
        password: formVal.password ?? '',
        role: formVal.role ?? 'CASHIER',
        branchId: branchIdVal,
      };

      this.userService.createUser(createReq).subscribe({
        next: (created) => {
          this.users.update((prev) => [...prev, created]);
          this.isSaving.set(false);
          this.closeModal();
          this.message.set({ type: 'success', text: `Colaborador '${created.username}' creado exitosamente.` });
        },
        error: (err) => {
          this.isSaving.set(false);
          const detail = err.error?.detail || err.error?.message || 'Error al crear el colaborador.';
          this.message.set({ type: 'error', text: detail });
        },
      });
    }
  }

  toggleStatus(user: UserSummaryDto): void {
    const nextStatus = !user.isActive;
    this.userService.toggleUserStatus(user.id, nextStatus).subscribe({
      next: (updated) => {
        this.users.update((prev) => prev.map((u) => (u.id === updated.id ? updated : u)));
        this.message.set({
          type: 'success',
          text: `Colaborador '${user.username}' ${nextStatus ? 'activado' : 'desactivado'} correctamente.`,
        });
      },
      error: (err) => {
        const detail = err.error?.detail || err.error?.message || 'No se pudo cambiar el estado del colaborador.';
        this.message.set({ type: 'error', text: detail });
      },
    });
  }

  getBranchName(branchId?: string | null): string {
    if (!branchId) {
      return 'Todas las sucursales';
    }
    const b = this.branches().find((item) => item.id === branchId);
    return b ? b.name : 'Sucursal asignada';
  }

  getRoleLabel(role: string): string {
    const found = this.availableRoles.find((r) => r.code === role);
    return found ? found.label : role;
  }
}
