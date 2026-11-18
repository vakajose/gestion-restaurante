interface RuntimeWindowEnv {
  API_BASE_URL?: string;
}

declare global {
  interface Window {
    __ENV__?: RuntimeWindowEnv;
  }
}

/**
 * Resuelve la URL base del API en runtime desde window.__ENV__.API_BASE_URL (inyectada por env.js en Nginx/Docker).
 * Retorna '' en entornos same-origin o con proxy local Angular dev server.
 */
export function getRuntimeApiBaseUrl(): string {
  const raw = typeof window !== 'undefined' ? window.__ENV__?.API_BASE_URL : undefined;
  if (!raw || raw.trim().length === 0) {
    return '';
  }
  return raw.trim().replace(/\/+$/, '');
}

/**
 * Resuelve URLs de medios/imágenes (`/uploads/...`) contra `API_BASE_URL` en entornos con subdominios
 * separados (`app.localhost` -> `api.localhost`), manteniendo intactas URLs absolutas o `data:`/`blob:`.
 */
export function resolveMediaUrl(url?: string | null): string | null {
  if (!url) {
    return null;
  }
  const trimmed = url.trim();
  if (!trimmed) {
    return null;
  }
  if (/^(https?:|data:|blob:)/i.test(trimmed)) {
    return trimmed;
  }
  const baseUrl = getRuntimeApiBaseUrl();
  if (baseUrl && trimmed.startsWith('/')) {
    return `${baseUrl}${trimmed}`;
  }
  return trimmed;
}
