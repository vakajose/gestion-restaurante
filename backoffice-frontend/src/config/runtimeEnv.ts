declare global {
  interface Window {
    __ENV__?: {
      API_BASE_URL?: string;
    };
  }
}

/**
 * Resuelve la URL base del API siguiendo Twelve-Factor:
 * 1) window.__ENV__.API_BASE_URL inyectado en runtime por public/env.js (Nginx/Docker)
 * 2) VITE_API_BASE_URL en tiempo de build/desarrollo
 * 3) Cadena vacía '' para usar mismo origen + proxy Vite /api en desarrollo local
 */
export function getApiBaseUrl(): string {
  const runtimeValue = typeof window !== 'undefined' ? window.__ENV__?.API_BASE_URL : undefined;
  const raw = runtimeValue !== undefined && runtimeValue !== ''
    ? runtimeValue
    : (import.meta.env.VITE_API_BASE_URL ?? '');

  return raw.trim().replace(/\/+$/, '');
}
