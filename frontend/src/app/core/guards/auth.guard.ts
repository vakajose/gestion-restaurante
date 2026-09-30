import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

/**
 * Guard funcional que protege rutas autenticadas (/pos, /inventory, /finance).
 * Si el usuario no está autenticado, redirige a /auth/login.
 */
export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return true;
  }

  return router.parseUrl('/auth/login');
};

/**
 * Guard funcional para rutas públicas/invitados (/auth/login).
 * Si el usuario ya está autenticado, redirige automáticamente a /pos.
 */
export const guestGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    return router.parseUrl('/pos');
  }

  return true;
};
