import { Injectable, signal, computed, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import {
  ActiveSessionDto,
  LoginRequest,
  LoginResponse,
  RefreshResponse,
  UserDto,
} from '../models/auth.models';
import { OnboardingResultDto } from '../models/company.models';

export const AUTH_TOKEN_KEY = 'auth_token';
export const AUTH_REFRESH_TOKEN_KEY = 'auth_refresh_token';
export const AUTH_USER_KEY = 'auth_user';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly platformId = inject(PLATFORM_ID);

  readonly token = signal<string | null>(null);
  readonly refreshTokenValue = signal<string | null>(null);
  readonly currentUser = signal<UserDto | null>(null);
  readonly isAuthenticated = computed(() => !!this.currentUser());

  constructor() {
    this.restoreSession();
  }

  /**
   * Autenticación adaptativa y calificada por tenant:
   * envía petición POST a /api/v1/auth/login con tenantIdentifier (slug o NIT), login y password
   */
  login(login: string, password: string, tenantIdentifier?: string): Observable<LoginResponse> {
    const request: LoginRequest = {
      login,
      password,
    };
    if (tenantIdentifier && tenantIdentifier.trim().length > 0) {
      request.tenantIdentifier = tenantIdentifier.trim();
    }
    return this.http.post<LoginResponse>('/api/v1/auth/login', request).pipe(
      tap((response) => {
        this.setSession(response);
      })
    );
  }

  /**
   * Establece la sesión inmediatamente tras completar el asistente de onboarding
   */
  establishSessionFromOnboarding(result: OnboardingResultDto): void {
    const tokenValue = result.accessToken || result.token;
    this.setSession({
      token: tokenValue,
      refreshToken: result.refreshToken ?? null,
      tokenType: 'Bearer',
      user: result.user,
    });
  }

  /**
   * Renovación silenciosa de tokens (Silent Refresh - ADR-0011)
   */
  refreshToken(): Observable<RefreshResponse> {
    const currentRefresh = this.getRefreshToken();
    return this.http
      .post<RefreshResponse>('/api/v1/auth/refresh', { refreshToken: currentRefresh })
      .pipe(
        tap((response) => {
          const newAccess = response.accessToken || response.token;
          if (newAccess) {
            this.token.set(newAccess);
            if (this.isBrowser()) {
              localStorage.setItem(AUTH_TOKEN_KEY, newAccess);
            }
          }
          if (response.refreshToken) {
            this.refreshTokenValue.set(response.refreshToken);
            if (this.isBrowser()) {
              localStorage.setItem(AUTH_REFRESH_TOKEN_KEY, response.refreshToken);
            }
          }
        })
      );
  }

  /**
   * Obtiene las sesiones activas del usuario actual
   */
  getActiveSessions(): Observable<ActiveSessionDto[]> {
    return this.http.get<ActiveSessionDto[]>('/api/v1/auth/sessions');
  }

  /**
   * Revoca una sesión activa específica del usuario actual
   */
  revokeSession(sessionId: string): Observable<void> {
    return this.http.delete<void>(`/api/v1/auth/sessions/${sessionId}`);
  }

  /**
   * Revoca todas las sesiones activas del usuario actual
   */
  revokeAllSessions(): Observable<void> {
    return this.http.post<void>('/api/v1/auth/revoke-all', {});
  }

  /**
   * Valida si el usuario actual posee un rol determinado
   */
  hasRole(role: string): boolean {
    const user = this.currentUser();
    return user?.role === role;
  }

  /**
   * Valida si el usuario actual posee alguno de los roles permitidos
   */
  hasAnyRole(roles: string[]): boolean {
    const user = this.currentUser();
    if (!user) {
      return false;
    }
    if (user.role === 'SUPERADMIN' || user.role === 'ADMIN_TENANT') {
      return true;
    }
    return roles.includes(user.role);
  }

  /**
   * Valida si el usuario actual posee un permiso granular específico (recurso:acción).
   * Los roles de administración total (SUPERADMIN, ADMIN_TENANT) o el comodín '*' tienen autorización implícita.
   */
  hasPermission(permission: string): boolean {
    const user = this.currentUser();
    if (!user) {
      return false;
    }
    if (
      user.role === 'SUPERADMIN' ||
      user.role === 'ADMIN_TENANT' ||
      user.permissions?.includes('*')
    ) {
      return true;
    }
    return user.permissions?.includes(permission) ?? false;
  }

  /**
   * Helper para directivas y vistas: verifica uno o varios privilegios granulares
   */
  hasPrivilege(privilege: string | string[]): boolean {
    if (Array.isArray(privilege)) {
      return privilege.length === 0 || privilege.some((p) => this.hasPermission(p));
    }
    return this.hasPermission(privilege);
  }

  /**
   * Cierra sesión, notifica opcionalmente al backend, limpia almacenamiento local y Signals, y redirige a /auth/login
   */
  logout(notifyBackend = false): void {
    const refresh = this.getRefreshToken();
    if (notifyBackend && refresh) {
      this.http.post('/api/v1/auth/logout', { refreshToken: refresh }).subscribe({
        error: () => {
          // Ignorar errores de red al cerrar sesión
        },
      });
    }
    this.clearSession();
    this.router.navigate(['/auth/login']);
  }

  /**
   * Obtiene el token JWT actual para uso en interceptores y peticiones autenticadas
   */
  getToken(): string | null {
    const memoryToken = this.token();
    if (memoryToken) {
      return memoryToken;
    }
    if (this.isBrowser()) {
      return localStorage.getItem(AUTH_TOKEN_KEY);
    }
    return null;
  }

  /**
   * Obtiene el refreshToken actual
   */
  getRefreshToken(): string | null {
    const memoryRefresh = this.refreshTokenValue();
    if (memoryRefresh) {
      return memoryRefresh;
    }
    if (this.isBrowser()) {
      return localStorage.getItem(AUTH_REFRESH_TOKEN_KEY);
    }
    return null;
  }

  private restoreSession(): void {
    if (!this.isBrowser()) {
      return;
    }

    const savedToken = localStorage.getItem(AUTH_TOKEN_KEY);
    const savedRefresh = localStorage.getItem(AUTH_REFRESH_TOKEN_KEY);
    const savedUserStr = localStorage.getItem(AUTH_USER_KEY);

    if (savedToken && savedUserStr) {
      try {
        const user: UserDto = JSON.parse(savedUserStr);
        this.token.set(savedToken);
        if (savedRefresh) {
          this.refreshTokenValue.set(savedRefresh);
        }
        this.currentUser.set(user);
      } catch {
        this.clearSession();
      }
    }
  }

  private setSession(response: LoginResponse): void {
    const accessToken = response.accessToken || response.token;
    this.token.set(accessToken);
    this.refreshTokenValue.set(response.refreshToken ?? null);
    this.currentUser.set(response.user);

    if (this.isBrowser()) {
      localStorage.setItem(AUTH_TOKEN_KEY, accessToken);
      if (response.refreshToken) {
        localStorage.setItem(AUTH_REFRESH_TOKEN_KEY, response.refreshToken);
      } else {
        localStorage.removeItem(AUTH_REFRESH_TOKEN_KEY);
      }
      localStorage.setItem(AUTH_USER_KEY, JSON.stringify(response.user));
    }
  }

  private clearSession(): void {
    this.token.set(null);
    this.refreshTokenValue.set(null);
    this.currentUser.set(null);

    if (this.isBrowser()) {
      localStorage.removeItem(AUTH_TOKEN_KEY);
      localStorage.removeItem(AUTH_REFRESH_TOKEN_KEY);
      localStorage.removeItem(AUTH_USER_KEY);
    }
  }

  private isBrowser(): boolean {
    return isPlatformBrowser(this.platformId) && typeof window !== 'undefined';
  }
}
