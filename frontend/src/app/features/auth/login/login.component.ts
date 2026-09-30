import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import { ThemeService } from '../../../core/services/theme.service';
import { ProblemDetail } from '../../../core/models/auth.models';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.component.html',
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  readonly themeService = inject(ThemeService);

  readonly isLoading = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly showPassword = signal<boolean>(false);

  readonly loginForm = this.fb.group({
    login: ['', [Validators.required, Validators.minLength(3)]],
    password: ['', [Validators.required, Validators.minLength(4)]],
  });

  onSubmit(): void {
    if (this.loginForm.invalid || this.isLoading()) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    const formValue = this.loginForm.getRawValue();
    const loginValue = (formValue.login ?? '').trim();
    const passwordValue = formValue.password ?? '';

    this.authService.login(loginValue, passwordValue).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.router.navigate(['/pos']);
      },
      error: (error: unknown) => {
        this.isLoading.set(false);
        if (error instanceof HttpErrorResponse) {
          if (error.error && typeof error.error === 'object') {
            const problem = error.error as ProblemDetail;
            this.errorMessage.set(
              problem.detail || problem.title || 'Credenciales inválidas. Por favor verifique sus datos.'
            );
          } else if (error.status === 401) {
            this.errorMessage.set('Usuario o contraseña incorrectos.');
          } else if (error.status === 0) {
            this.errorMessage.set('No se pudo conectar con el servidor backend.');
          } else {
            this.errorMessage.set('Error en el servidor al intentar iniciar sesión.');
          }
        } else {
          this.errorMessage.set('Ocurrió un error inesperado al iniciar sesión.');
        }
      },
    });
  }

  fillDemoAdmin(): void {
    this.loginForm.patchValue({
      login: 'admin',
      password: 'admin123',
    });
    this.errorMessage.set(null);
  }

  fillDemoCashier(): void {
    this.loginForm.patchValue({
      login: 'cajero1',
      password: 'cajero123',
    });
    this.errorMessage.set(null);
  }

  togglePasswordVisibility(): void {
    this.showPassword.update((val) => !val);
  }

  toggleTheme(): void {
    this.themeService.toggleTheme();
  }

  clearError(): void {
    this.errorMessage.set(null);
  }
}
