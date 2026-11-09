import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { UserService } from '../../core/services/user.service';
import { AuthService } from '../../core/services/auth.service';
import { BranchContextService } from '../../core/services/branch-context.service';
import { ActiveSessionDto, UserDto } from '../../core/models/auth.models';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './profile.component.html',
})
export class ProfileComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly userService = inject(UserService);
  readonly authService = inject(AuthService);
  readonly branchContextService = inject(BranchContextService);

  readonly user = signal<UserDto | null>(null);
  readonly activeSessions = signal<ActiveSessionDto[]>([]);
  readonly isLoading = signal<boolean>(false);
  readonly isSavingProfile = signal<boolean>(false);
  readonly isChangingPassword = signal<boolean>(false);
  readonly isUpdatingPin = signal<boolean>(false);

  readonly profileMessage = signal<{ type: 'success' | 'error'; text: string } | null>(null);
  readonly passwordMessage = signal<{ type: 'success' | 'error'; text: string } | null>(null);
  readonly sessionsMessage = signal<{ type: 'success' | 'error'; text: string } | null>(null);
  readonly pinMessage = signal<{ type: 'success' | 'error'; text: string } | null>(null);

  readonly showCurrentPassword = signal<boolean>(false);
  readonly showNewPassword = signal<boolean>(false);

  readonly profileForm = this.fb.group({
    fullName: ['', [Validators.required, Validators.maxLength(100)]],
    phoneNumber: ['', [Validators.maxLength(30)]],
    email: ['', [Validators.email]],
  });

  readonly passwordForm = this.fb.group({
    currentPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(6)]],
    confirmPassword: ['', [Validators.required]],
  });

  readonly pinForm = this.fb.group({
    pin: ['', [Validators.required, Validators.pattern('^[0-9]{4,6}$')]],
    confirmPin: ['', [Validators.required, Validators.pattern('^[0-9]{4,6}$')]],
  });

  ngOnInit(): void {
    this.loadProfile();
    this.loadActiveSessions();
  }

  loadActiveSessions(): void {
    if (typeof this.authService.getActiveSessions !== 'function') {
      return;
    }
    this.authService.getActiveSessions().subscribe({
      next: (list) => this.activeSessions.set(list ?? []),
      error: () => {
        // Ignorar errores de carga opcional de sesiones
      },
    });
  }

  revokeSingleSession(sessionId: string): void {
    this.authService.revokeSession(sessionId).subscribe({
      next: () => {
        this.activeSessions.update((prev) => prev.filter((s) => s.id !== sessionId));
        this.sessionsMessage.set({
          type: 'success',
          text: 'Sesión remota revocada exitosamente.',
        });
      },
      error: () => {
        this.sessionsMessage.set({
          type: 'error',
          text: 'No se pudo revocar la sesión seleccionada.',
        });
      },
    });
  }

  revokeAllMySessions(): void {
    this.authService.revokeAllSessions().subscribe({
      next: () => {
        this.activeSessions.set([]);
        this.sessionsMessage.set({
          type: 'success',
          text: 'Todas las sesiones activas han sido cerradas.',
        });
      },
      error: () => {
        this.sessionsMessage.set({
          type: 'error',
          text: 'Error al intentar cerrar todas las sesiones.',
        });
      },
    });
  }

  loadProfile(): void {
    this.isLoading.set(true);
    this.userService.getProfile().subscribe({
      next: (u) => {
        this.user.set(u);
        this.profileForm.patchValue({
          fullName: u.fullName ?? '',
          phoneNumber: u.phoneNumber ?? '',
          email: u.email ?? '',
        });
        this.isLoading.set(false);
      },
      error: () => {
        this.isLoading.set(false);
        this.profileMessage.set({ type: 'error', text: 'Error al cargar los datos del perfil.' });
      },
    });
  }

  saveProfile(): void {
    if (this.profileForm.invalid || this.isSavingProfile()) {
      this.profileForm.markAllAsTouched();
      return;
    }

    this.isSavingProfile.set(true);
    this.profileMessage.set(null);

    const val = this.profileForm.getRawValue();
    this.userService
      .updateProfile({
        fullName: (val.fullName ?? '').trim(),
        phoneNumber: val.phoneNumber ? val.phoneNumber.trim() : null,
        email: val.email && val.email.trim().length > 0 ? val.email.trim() : null,
      })
      .subscribe({
        next: (updated) => {
          this.user.set(updated);
          if (typeof this.authService.currentUser?.set === 'function') {
            this.authService.currentUser.set(updated);
          }
          this.isSavingProfile.set(false);
          this.profileMessage.set({ type: 'success', text: 'Datos personales actualizados exitosamente.' });
        },
        error: (err) => {
          this.isSavingProfile.set(false);
          const detail = err.error?.detail || err.error?.message || 'Error al actualizar los datos personales.';
          this.profileMessage.set({ type: 'error', text: detail });
        },
      });
  }

  getUserInitials(name?: string | null, username?: string): string {
    const text = name && name.trim().length > 0 ? name.trim() : username || 'U';
    const parts = text.split(/\s+/).filter(Boolean);
    if (parts.length >= 2) {
      return (parts[0][0] + parts[1][0]).toUpperCase();
    }
    return text.substring(0, 2).toUpperCase();
  }

  getRoleDisplayName(role?: string | null): string {
    if (!role) return 'Colaborador';
    switch (role) {
      case 'SUPERADMIN':
        return 'Superadministrador';
      case 'ADMIN_TENANT':
        return 'Administrador de Empresa';
      case 'BRANCH_MANAGER':
        return 'Gerente de Sucursal';
      case 'CASHIER':
        return 'Cajero de Punto de Venta';
      case 'KITCHEN':
        return 'Encargado de Cocina (KDS)';
      default:
        return role;
    }
  }

  changePassword(): void {
    if (this.passwordForm.invalid || this.isChangingPassword()) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    const val = this.passwordForm.getRawValue();
    if (val.newPassword !== val.confirmPassword) {
      this.passwordMessage.set({ type: 'error', text: 'La nueva contraseña y su confirmación no coinciden.' });
      return;
    }

    this.isChangingPassword.set(true);
    this.passwordMessage.set(null);

    this.userService
      .changePassword({
        currentPassword: val.currentPassword ?? '',
        newPassword: val.newPassword ?? '',
      })
      .subscribe({
        next: () => {
          this.isChangingPassword.set(false);
          this.passwordForm.reset();
          this.passwordMessage.set({ type: 'success', text: 'Contraseña actualizada con éxito.' });
        },
        error: (err) => {
          this.isChangingPassword.set(false);
          const detail = err.error?.detail || err.error?.message || 'Error al actualizar la contraseña.';
          this.passwordMessage.set({ type: 'error', text: detail });
        },
      });
  }

  updateSupervisorPin(): void {
    if (this.pinForm.invalid || this.isUpdatingPin()) {
      this.pinForm.markAllAsTouched();
      return;
    }

    const val = this.pinForm.getRawValue();
    if (val.pin !== val.confirmPin) {
      this.pinMessage.set({ type: 'error', text: 'El PIN numérico y su confirmación no coinciden.' });
      return;
    }

    this.isUpdatingPin.set(true);
    this.pinMessage.set(null);

    this.userService
      .updatePin({ pin: val.pin! })
      .subscribe({
        next: () => {
          this.isUpdatingPin.set(false);
          this.pinForm.reset();
          this.pinMessage.set({ type: 'success', text: 'PIN de supervisor actualizado correctamente.' });
        },
        error: (err) => {
          this.isUpdatingPin.set(false);
          const detail = err.error?.detail || err.error?.message || 'Error al actualizar el PIN de supervisor.';
          this.pinMessage.set({ type: 'error', text: detail });
        },
      });
  }
}
