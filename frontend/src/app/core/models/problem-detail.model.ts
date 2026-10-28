/**
 * Modelo estándar conforme a RFC 7807 (Problem Details for HTTP APIs).
 */
export interface ProblemDetail {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  errorCode?: string;
  timestamp?: string;
  tenantId?: string;
  invalidParams?: Record<string, string>;
  [key: string]: unknown;
}

/**
 * Error de aplicación enriquecido con los metadatos de RFC 7807 ProblemDetail.
 */
export interface AppHttpError extends Error {
  status: number;
  problemDetail?: ProblemDetail;
  errorCode?: string;
  timestamp?: string;
}
