import { CanActivateFn, Router, ActivatedRouteSnapshot } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';

/**
 * Guard funcional que protege rutas autenticadas (/pos, /inventory, /finance, /company).
 * Si el usuario no está autenticado, redirige a /auth/login.
 * Si el usuario tiene mustChangePassword=true, bloquea cualquier ruta distinta de /profile.
 */
export const authGuard: CanActivateFn = (_route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    return router.parseUrl('/auth/login');
  }

  if (authService.currentUser()?.mustChangePassword && !state?.url?.startsWith('/profile')) {
    return router.parseUrl('/profile');
  }

  return true;
};

/**
 * Guard funcional para rutas públicas/invitados (/auth/login).
 * Si el usuario ya está autenticado, redirige automáticamente a /profile (si debe cambiar clave) o a /pos.
 */
export const guestGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isAuthenticated()) {
    if (authService.currentUser()?.mustChangePassword) {
      return router.parseUrl('/profile');
    }
    return router.parseUrl('/pos');
  }

  return true;
};

/**
 * Guard funcional para autorización granular por roles.
 * Verifica si el usuario posee alguno de los roles requeridos especificados en data: { roles: [...] }.
 */
export const roleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    return router.parseUrl('/auth/login');
  }

  if (authService.currentUser()?.mustChangePassword) {
    return router.parseUrl('/profile');
  }

  const expectedRoles = (route.data?.['roles'] as string[]) || [];
  if (expectedRoles.length === 0 || authService.hasAnyRole(expectedRoles)) {
    return true;
  }

  return router.parseUrl('/pos');
};

/**
 * Guard funcional para Control de Acceso Basado en Recursos (Granular Resource Permissions).
 * Verifica si el usuario posee los permisos requeridos (recurso:acción) en data: { permissions: [...] }.
 */
export const permissionGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (!authService.isAuthenticated()) {
    return router.parseUrl('/auth/login');
  }

  if (authService.currentUser()?.mustChangePassword) {
    return router.parseUrl('/profile');
  }

  const expectedPermissions = (route.data?.['permissions'] as string[]) || [];
  if (expectedPermissions.length === 0) {
    return true;
  }

  const hasAll = expectedPermissions.every((perm) => authService.hasPermission(perm));
  if (hasAll) {
    return true;
  }

  return router.parseUrl('/pos');
};
