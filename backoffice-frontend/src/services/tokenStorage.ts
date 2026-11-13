import type { PlatformPrincipal } from '../types/api';

const ACCESS_TOKEN_KEY = 'backoffice_access_token';
const REFRESH_TOKEN_KEY = 'backoffice_refresh_token';
const ADMIN_KEY = 'backoffice_admin';

export const tokenStorage = {
  getAccessToken(): string | null {
    return localStorage.getItem(ACCESS_TOKEN_KEY);
  },

  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  },

  getAdmin(): PlatformPrincipal | null {
    const raw = localStorage.getItem(ADMIN_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as PlatformPrincipal;
    } catch {
      return null;
    }
  },

  saveSession(accessToken: string, refreshToken: string, admin: PlatformPrincipal): void {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
    localStorage.setItem(ADMIN_KEY, JSON.stringify(admin));
  },

  updateAdmin(admin: PlatformPrincipal): void {
    localStorage.setItem(ADMIN_KEY, JSON.stringify(admin));
  },

  clear(): void {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(ADMIN_KEY);
  },
};
