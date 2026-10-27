import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { BranchContextService } from '../services/branch-context.service';

/**
 * Interceptor funcional que adjunta:
 * - Authorization: Bearer <token>
 * - X-Branch-Id: <activeBranchId> (si existe contexto de sucursal activa)
 * a todas las peticiones autenticadas salientes.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // Omitir peticiones de autenticación pública
  if (req.url.includes('/api/v1/auth/login') || req.url.includes('/api/v1/auth/register')) {
    return next(req);
  }

  const authService = inject(AuthService);
  const branchContextService = inject(BranchContextService);

  const token = authService.getToken();
  const activeBranchId = branchContextService.activeBranchId();

  const setHeaders: Record<string, string> = {};
  if (token) {
    setHeaders['Authorization'] = `Bearer ${token}`;
  }
  if (activeBranchId) {
    setHeaders['X-Branch-Id'] = activeBranchId;
  }

  if (Object.keys(setHeaders).length > 0) {
    const authReq = req.clone({ setHeaders });
    return next(authReq);
  }

  return next(req);
};
