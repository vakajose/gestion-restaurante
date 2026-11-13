import { createContext, useContext } from 'react';
import type { PlatformPrincipal } from '../types/api';

export interface AuthContextValue {
  admin: PlatformPrincipal | null;
  isAuthenticated: boolean;
  isSuperAdmin: boolean;
  isBootstrapping: boolean;
  login: (login: string, password: string) => Promise<PlatformPrincipal>;
  logout: () => Promise<void>;
  refreshProfile: () => Promise<void>;
  applyUpdatedPrincipal: (principal: PlatformPrincipal) => void;
}

export const AuthContext = createContext<AuthContextValue | null>(null);

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth debe utilizarse dentro de AuthProvider');
  }
  return ctx;
}
