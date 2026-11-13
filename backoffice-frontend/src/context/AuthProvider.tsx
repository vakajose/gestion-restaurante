import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { authApi } from '../services/backofficeApi';
import { setUnauthorizedHandler } from '../services/httpClient';
import { tokenStorage } from '../services/tokenStorage';
import type { PlatformPrincipal } from '../types/api';
import { AuthContext, type AuthContextValue } from './authContext';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [admin, setAdmin] = useState<PlatformPrincipal | null>(() => tokenStorage.getAdmin());
  const [isBootstrapping, setIsBootstrapping] = useState<boolean>(() => Boolean(tokenStorage.getAccessToken()));

  const forceClearSession = useCallback(() => {
    tokenStorage.clear();
    setAdmin(null);
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(forceClearSession);
    return () => setUnauthorizedHandler(null);
  }, [forceClearSession]);

  useEffect(() => {
    const token = tokenStorage.getAccessToken();
    if (!token) {
      setIsBootstrapping(false);
      return;
    }

    let cancelled = false;
    authApi
      .me()
      .then((fresh) => {
        if (!cancelled) {
          tokenStorage.updateAdmin(fresh);
          setAdmin(fresh);
        }
      })
      .catch(() => {
        if (!cancelled) {
          forceClearSession();
        }
      })
      .finally(() => {
        if (!cancelled) {
          setIsBootstrapping(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [forceClearSession]);

  const login = useCallback(async (loginInput: string, password: string): Promise<PlatformPrincipal> => {
    const res = await authApi.login(loginInput, password);
    tokenStorage.saveSession(res.accessToken, res.refreshToken, res.admin);
    setAdmin(res.admin);
    return res.admin;
  }, []);

  const logout = useCallback(async (): Promise<void> => {
    const refresh = tokenStorage.getRefreshToken();
    try {
      await authApi.logout(refresh);
    } catch {
      // Silencioso al cerrar sesión localmente
    } finally {
      forceClearSession();
    }
  }, [forceClearSession]);

  const refreshProfile = useCallback(async (): Promise<void> => {
    const fresh = await authApi.me();
    tokenStorage.updateAdmin(fresh);
    setAdmin(fresh);
  }, []);

  const applyUpdatedPrincipal = useCallback((principal: PlatformPrincipal): void => {
    tokenStorage.updateAdmin(principal);
    setAdmin(principal);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      admin,
      isAuthenticated: admin !== null,
      isSuperAdmin: admin?.role === 'SUPERADMIN',
      isBootstrapping,
      login,
      logout,
      refreshProfile,
      applyUpdatedPrincipal,
    }),
    [admin, isBootstrapping, login, logout, refreshProfile, applyUpdatedPrincipal],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
