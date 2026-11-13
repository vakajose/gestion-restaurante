import { HttpInterceptorFn } from '@angular/common/http';
import { getRuntimeApiBaseUrl } from '../config/runtime-env';

/**
 * Reescribe rutas relativas `/api/...` al origen configurado en runtime (`window.__ENV__.API_BASE_URL`).
 * Permite construir una única imagen Docker inmutable promovida entre testing-local, staging y producción.
 */
export const apiBaseUrlInterceptor: HttpInterceptorFn = (req, next) => {
  if (!req.url.startsWith('/api/')) {
    return next(req);
  }

  const baseUrl = getRuntimeApiBaseUrl();
  if (!baseUrl) {
    return next(req);
  }

  return next(req.clone({ url: `${baseUrl}${req.url}` }));
};
