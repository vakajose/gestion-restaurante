import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { ProblemDetail } from '../models/problem-detail.model';

/**
 * Interceptor funcional que captura respuestas de error HTTP y las normaliza
 * según el estándar RFC 7807 (Problem Details for HTTP APIs).
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const problemDetail = extractOrBuildProblemDetail(error);

      // Enriquecer el objeto de error para que los servicios/componentes puedan consumirlo uniformemente
      Object.assign(error, {
        problemDetail,
        errorCode: problemDetail.errorCode,
      });

      return throwError(() => error);
    })
  );
};

function extractOrBuildProblemDetail(error: HttpErrorResponse): ProblemDetail {
  // 1. Si la respuesta ya incluye un ProblemDetail válido en el cuerpo
  if (error.error && typeof error.error === 'object' && !Array.isArray(error.error)) {
    const raw = error.error as Record<string, unknown>;
    if (raw['detail'] || raw['title'] || raw['errorCode'] || raw['status']) {
      return {
        type: typeof raw['type'] === 'string' ? raw['type'] : undefined,
        title: typeof raw['title'] === 'string' ? raw['title'] : getDefaultTitle(error.status),
        status: typeof raw['status'] === 'number' ? raw['status'] : error.status,
        detail: typeof raw['detail'] === 'string' ? raw['detail'] : getDefaultDetail(error.status),
        instance: typeof raw['instance'] === 'string' ? raw['instance'] : undefined,
        errorCode: typeof raw['errorCode'] === 'string' ? raw['errorCode'] : getDefaultErrorCode(error.status),
        timestamp: typeof raw['timestamp'] === 'string' ? raw['timestamp'] : new Date().toISOString(),
        tenantId: typeof raw['tenantId'] === 'string' ? raw['tenantId'] : undefined,
        invalidParams: raw['invalidParams'] && typeof raw['invalidParams'] === 'object'
          ? (raw['invalidParams'] as Record<string, string>)
          : undefined,
      };
    }
  }

  // 2. Errores de red o timeout (status 0)
  if (error.status === 0) {
    return {
      type: 'https://restaurant.com/errors/network-error',
      title: 'Error de conexión',
      status: 0,
      detail: 'No fue posible establecer conexión con el servidor. Verifique su red.',
      errorCode: 'NETWORK_ERROR',
      timestamp: new Date().toISOString(),
    };
  }

  // 3. Fallback genérico para otros estados HTTP
  return {
    type: `https://restaurant.com/errors/http-${error.status}`,
    title: getDefaultTitle(error.status),
    status: error.status,
    detail: typeof error.error === 'string' && error.error.trim().length > 0
      ? error.error
      : getDefaultDetail(error.status),
    errorCode: getDefaultErrorCode(error.status),
    timestamp: new Date().toISOString(),
  };
}

function getDefaultTitle(status: number): string {
  switch (status) {
    case 400: return 'Solicitud inválida';
    case 401: return 'Autenticación requerida';
    case 403: return 'Acceso denegado';
    case 404: return 'Recurso no encontrado';
    case 409: return 'Conflicto de recurso';
    case 422: return 'Regla de negocio no procesable';
    case 500: return 'Error interno del servidor';
    default: return 'Error en la solicitud';
  }
}

function getDefaultDetail(status: number): string {
  switch (status) {
    case 400: return 'Los datos enviados en la solicitud son incorrectos.';
    case 401: return 'Su sesión ha expirado o no cuenta con credenciales válidas.';
    case 403: return 'No tiene permisos suficientes para ejecutar esta acción.';
    case 404: return 'El elemento solicitado no existe o no fue encontrado.';
    case 409: return 'El recurso entra en conflicto con un registro existente o estado actual.';
    case 422: return 'La operación viola una regla de negocio del sistema.';
    case 500: return 'Ha ocurrido un error inesperado en el servidor.';
    default: return 'No se pudo completar la operación solicitada.';
  }
}

function getDefaultErrorCode(status: number): string {
  switch (status) {
    case 400: return 'BAD_REQUEST';
    case 401: return 'UNAUTHORIZED';
    case 403: return 'FORBIDDEN';
    case 404: return 'RESOURCE_NOT_FOUND';
    case 409: return 'RESOURCE_CONFLICT';
    case 422: return 'BUSINESS_RULE_VIOLATION';
    case 500: return 'INTERNAL_SERVER_ERROR';
    default: return 'UNKNOWN_ERROR';
  }
}
