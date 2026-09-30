import { Injectable, signal, computed, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { LoginRequest, LoginResponse, UserDto } from '../models/auth.models';

export const AUTH_TOKEN_KEY = 'auth_token';
export const AUTH_USER_KEY = 'auth_user';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly platformId = inject(PLATFORM_ID);

  readonly token = signal<string | null>(null);
  readonly currentUser = signal<UserDto | null>(null);
  readonly isAuthenticated = computed(() => !!this.currentUser());

  constructor() {
    this.restoreSession();
  }

  /**
   * Autenticación adaptativa: envía petición POST a /api/v1/auth/login con username o email
   */
  login(login: string, password: string): Observable<LoginResponse> {
    const request: LoginRequest = { login, password };
    return this.http.post<LoginResponse>('/api/v1/auth/login', request).pipe(
      tap((response) => {
        this.setSession(response);
      })
    );
  }

  /**
   * Cierra sesión, limpia almacenamiento local y Signals, y redirige a /auth/login
   */
  logout(): void {
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

  private restoreSession(): void {
    if (!this.isBrowser()) {
      return;
    }

    const savedToken = localStorage.getItem(AUTH_TOKEN_KEY);
    const savedUserStr = localStorage.getItem(AUTH_USER_KEY);

    if (savedToken && savedUserStr) {
      try {
        const user: UserDto = JSON.parse(savedUserStr);
        this.token.set(savedToken);
        this.currentUser.set(user);
      } catch {
        this.clearSession();
      }
    }
  }

  private setSession(response: LoginResponse): void {
    this.token.set(response.token);
    this.currentUser.set(response.user);

    if (this.isBrowser()) {
      localStorage.setItem(AUTH_TOKEN_KEY, response.token);
      localStorage.setItem(AUTH_USER_KEY, JSON.stringify(response.user));
    }
  }

  private clearSession(): void {
    this.token.set(null);
    this.currentUser.set(null);

    if (this.isBrowser()) {
      localStorage.removeItem(AUTH_TOKEN_KEY);
      localStorage.removeItem(AUTH_USER_KEY);
    }
  }

  private isBrowser(): boolean {
    return isPlatformBrowser(this.platformId) && typeof window !== 'undefined';
  }
}
