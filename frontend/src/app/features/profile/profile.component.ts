import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { UserService } from '../../core/services/user.service';
import { AuthService } from '../../core/services/auth.service';
import { BranchContextService } from '../../core/services/branch-context.service';
import { UserDto } from '../../core/models/auth.models';

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
  readonly isLoading = signal<boolean>(false);
  readonly isSavingProfile = signal<boolean>(false);
  readonly isChangingPassword = signal<boolean>(false);

  readonly profileMessage = signal<{ type: 'success' | 'error'; text: string } | null>(null);
  readonly passwordMessage = signal<{ type: 'success' | 'error'; text: string } | null>(null);

  readonly showCurrentPassword = signal<boolean>(false);
  readonly showNewPassword = signal<boolean>(false);

  readonly profileForm = this.fb.group({
    email: ['', [Validators.email]],
  });

  readonly passwordForm = this.fb.group({
    currentPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(6)]],
    confirmPassword: ['', [Validators.required]],
  });

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.isLoading.set(true);
    this.userService.getProfile().subscribe({
      next: (u) => {
        this.user.set(u);
        this.profileForm.patchValue({
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
    this.userService.updateProfile({ email: val.email ?? '' }).subscribe({
      next: (updated) => {
        this.user.set(updated);
        this.isSavingProfile.set(false);
        this.profileMessage.set({ type: 'success', text: 'Perfil actualizado exitosamente.' });
      },
      error: (err) => {
        this.isSavingProfile.set(false);
        const detail = err.error?.detail || err.error?.message || 'Error al actualizar el perfil.';
        this.profileMessage.set({ type: 'error', text: detail });
      },
    });
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
}
