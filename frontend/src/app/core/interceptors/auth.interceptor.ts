import {
  HttpInterceptorFn,
  HttpErrorResponse,
  HttpRequest,
  HttpHandlerFn,
  HttpEvent,
} from '@angular/common/http';
import { inject } from '@angular/core';
import {
  BehaviorSubject,
  Observable,
  catchError,
  filter,
  switchMap,
  take,
  throwError,
} from 'rxjs';
import { AuthService } from '../services/auth.service';
import { BranchContextService } from '../services/branch-context.service';

let isRefreshing = false;
const refreshTokenSubject = new BehaviorSubject<string | null>(null);

/**
 * Reinicia el estado interno de la cola de renovación silenciosa para pruebas unitarias.
 */
export function resetAuthInterceptorState(): void {
  isRefreshing = false;
  refreshTokenSubject.next(null);
}

function isPublicAuthUrl(url: string): boolean {
  return (
    url.includes('/api/v1/auth/login') ||
    url.includes('/api/v1/auth/register') ||
    url.includes('/api/v1/auth/onboarding') ||
    url.includes('/api/v1/auth/check-slug') ||
    url.includes('/api/v1/auth/refresh') ||
    url.includes('/api/v1/auth/logout')
  );
}

/**
 * Interceptor funcional que adjunta:
 * - Authorization: Bearer <token>
 * - X-Branch-Id: <activeBranchId> (si existe contexto de sucursal activa)
 * y gestiona Silent Refresh con cola RxJS ante respuestas 401 (ADR-0011).
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (isPublicAuthUrl(req.url)) {
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

  const authReq = Object.keys(setHeaders).length > 0 ? req.clone({ setHeaders }) : req;

  return next(authReq).pipe(
    catchError((error: unknown) => {
      if (
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        !isPublicAuthUrl(req.url)
      ) {
        return handle401Error(authReq, next, authService);
      }
      return throwError(() => error);
    })
  );
};

function handle401Error(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  authService: AuthService
): Observable<HttpEvent<unknown>> {
  const currentRefreshToken = authService.getRefreshToken();
  if (!currentRefreshToken) {
    authService.logout(false);
    return throwError(() => new Error('No refresh token available'));
  }

  if (!isRefreshing) {
    isRefreshing = true;
    refreshTokenSubject.next(null);

    return authService.refreshToken().pipe(
      switchMap((response) => {
        isRefreshing = false;
        const newToken = response.accessToken || response.token;
        refreshTokenSubject.next(newToken);
        return next(
          req.clone({
            setHeaders: {
              Authorization: `Bearer ${newToken}`,
            },
          })
        );
      }),
      catchError((err: unknown) => {
        isRefreshing = false;
        authService.logout(false);
        return throwError(() => err);
      })
    );
  }

  // Encolar peticiones concurrentes hasta que el nuevo token esté listo
  return refreshTokenSubject.pipe(
    filter((token): token is string => token !== null),
    take(1),
    switchMap((newToken) =>
      next(
        req.clone({
          setHeaders: {
            Authorization: `Bearer ${newToken}`,
          },
        })
      )
    )
  );
}
