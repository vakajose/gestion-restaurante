import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError, apiRequest } from './httpClient';
import { tokenStorage } from './tokenStorage';

const SAMPLE_ADMIN = {
  adminId: '00000000-0000-0000-0000-000000000001',
  username: 'superadmin',
  email: 'sa@platform.local',
  fullName: 'Superadmin',
  role: 'SUPERADMIN' as const,
  mustChangePassword: false,
};

describe('httpClient', () => {
  afterEach(() => {
    vi.restoreAllMocks();
    window.__ENV__ = { API_BASE_URL: '' };
  });

  it('ejecuta refresh single-flight ante múltiples respuestas 401 concurrentes y reintenta con el nuevo token', async () => {
    window.__ENV__ = { API_BASE_URL: '' };
    tokenStorage.saveSession('expired-jwt', 'valid-refresh', SAMPLE_ADMIN);

    let refreshCalls = 0;
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      if (url.endsWith('/api/v1/backoffice/auth/refresh')) {
        refreshCalls += 1;
        return new Response(
          JSON.stringify({
            accessToken: 'fresh-jwt',
            refreshToken: 'fresh-refresh',
            expiresInSeconds: 900,
            admin: SAMPLE_ADMIN,
          }),
          { status: 200, headers: { 'Content-Type': 'application/json' } },
        );
      }

      const authHeader = new Headers(init?.headers).get('Authorization');
      if (authHeader === 'Bearer expired-jwt') {
        return new Response(JSON.stringify({ detail: 'Token expirado' }), {
          status: 401,
          headers: { 'Content-Type': 'application/json' },
        });
      }

      return new Response(JSON.stringify({ ok: true, endpoint: url }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      });
    });

    vi.stubGlobal('fetch', fetchMock);

    const [r1, r2] = await Promise.all([
      apiRequest<{ ok: boolean }>('/api/v1/backoffice/plans'),
      apiRequest<{ ok: boolean }>('/api/v1/backoffice/dashboard/overview'),
    ]);

    expect(r1.ok).toBe(true);
    expect(r2.ok).toBe(true);
    expect(refreshCalls).toBe(1);
    expect(tokenStorage.getAccessToken()).toBe('fresh-jwt');
    expect(tokenStorage.getRefreshToken()).toBe('fresh-refresh');
  });

  it('parsea ProblemDetail RFC 7807 con errorCode en ApiError', async () => {
    window.__ENV__ = { API_BASE_URL: '' };
    tokenStorage.saveSession('valid-jwt', 'refresh', SAMPLE_ADMIN);

    vi.stubGlobal(
      'fetch',
      vi.fn(async () =>
        new Response(
          JSON.stringify({
            status: 422,
            errorCode: 'LAST_SUPERADMIN_PROTECTED',
            detail: 'No se puede desactivar el último SUPERADMIN activo.',
          }),
          { status: 422, headers: { 'Content-Type': 'application/json' } },
        ),
      ),
    );

    await expect(apiRequest('/api/v1/backoffice/admins/1/status', { method: 'PATCH' })).rejects.toEqual(
      expect.objectContaining<Partial<ApiError>>({
        status: 422,
        errorCode: 'LAST_SUPERADMIN_PROTECTED',
        detail: 'No se puede desactivar el último SUPERADMIN activo.',
      }),
    );
  });
});
