import { getApiBaseUrl } from '../config/runtimeEnv';
import type { PlatformAuthResponse } from '../types/api';
import { tokenStorage } from './tokenStorage';

export class ApiError extends Error {
  public readonly status: number;
  public readonly errorCode: string | undefined;
  public readonly detail: string;

  constructor(status: number, detail: string, errorCode?: string) {
    super(detail);
    this.name = 'ApiError';
    this.status = status;
    this.detail = detail;
    this.errorCode = errorCode;
  }
}

type UnauthorizedHandler = () => void;
let unauthorizedHandler: UnauthorizedHandler | null = null;
let refreshPromise: Promise<string | null> | null = null;

export function setUnauthorizedHandler(handler: UnauthorizedHandler | null): void {
  unauthorizedHandler = handler;
}

interface RequestOptions extends Omit<RequestInit, 'body'> {
  body?: unknown;
  query?: Record<string, string | number | boolean | null | undefined>;
  skipAuth?: boolean;
  skipRefreshRetry?: boolean;
}

function buildUrl(path: string, query?: RequestOptions['query']): string {
  const baseUrl = getApiBaseUrl();
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  let url = `${baseUrl}${normalizedPath}`;

  if (query) {
    const params = new URLSearchParams();
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== null && value !== '') {
        params.set(key, String(value));
      }
    }
    const qs = params.toString();
    if (qs) {
      url += `?${qs}`;
    }
  }

  return url;
}

async function parseErrorResponse(response: Response): Promise<ApiError> {
  const fallback = `Error HTTP ${response.status}`;
  try {
    const text = await response.text();
    if (!text) return new ApiError(response.status, fallback);
    const json = JSON.parse(text) as {
      detail?: string;
      message?: string;
      title?: string;
      errorCode?: string;
    };
    const detail = json.detail || json.message || json.title || fallback;
    return new ApiError(response.status, detail, json.errorCode);
  } catch {
    return new ApiError(response.status, fallback);
  }
}

async function refreshAccessTokenSingleFlight(): Promise<string | null> {
  if (refreshPromise) return refreshPromise;

  const refreshToken = tokenStorage.getRefreshToken();
  if (!refreshToken) {
    tokenStorage.clear();
    unauthorizedHandler?.();
    return null;
  }

  refreshPromise = (async () => {
    try {
      const response = await fetch(buildUrl('/api/v1/backoffice/auth/refresh'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken }),
      });

      if (!response.ok) {
        tokenStorage.clear();
        unauthorizedHandler?.();
        return null;
      }

      const data = (await response.json()) as PlatformAuthResponse;
      tokenStorage.saveSession(data.accessToken, data.refreshToken, data.admin);
      return data.accessToken;
    } catch {
      tokenStorage.clear();
      unauthorizedHandler?.();
      return null;
    } finally {
      refreshPromise = null;
    }
  })();

  return refreshPromise;
}

function buildFetchInit(
  rest: Omit<RequestInit, 'body'>,
  extraHeaders: HeadersInit | undefined,
  body: unknown,
  token: string | null,
): RequestInit {
  const headers = new Headers(extraHeaders);
  if (body !== undefined && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }
  return {
    ...rest,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  };
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { body, query, skipAuth, skipRefreshRetry, headers: extraHeaders, ...rest } = options;

  const url = buildUrl(path, query);
  const initialToken = !skipAuth ? tokenStorage.getAccessToken() : null;
  let response = await fetch(url, buildFetchInit(rest, extraHeaders, body, initialToken));

  if (response.status === 401 && !skipAuth && !skipRefreshRetry) {
    const newToken = await refreshAccessTokenSingleFlight();
    if (newToken) {
      response = await fetch(url, buildFetchInit(rest, extraHeaders, body, newToken));
    }
  }

  if (!response.ok) {
    throw await parseErrorResponse(response);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}
