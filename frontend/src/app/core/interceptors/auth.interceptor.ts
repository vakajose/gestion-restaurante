import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

/**
 * Interceptor funcional que agrega el encabezado Authorization: Bearer <token>
 * a todas las peticiones salientes que no sean de login.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  // Omitir peticiones de login
  if (req.url.includes('/api/v1/auth/login')) {
    return next(req);
  }

  const authService = inject(AuthService);
  const token = authService.getToken();

  if (token) {
    const authReq = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    });
    return next(authReq);
  }

  return next(req);
};
