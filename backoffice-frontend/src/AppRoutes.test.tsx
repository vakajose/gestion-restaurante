import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AppRoutes } from './App';
import { AuthProvider } from './context/AuthProvider';
import { tokenStorage } from './services/tokenStorage';
import type { PlatformPrincipal } from './types/api';

function renderWithRouter(initialEntry: string) {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <AuthProvider>
        <AppRoutes />
      </AuthProvider>
    </MemoryRouter>,
  );
}

describe('AppRoutes (Auth Guard, mustChangePassword y RBAC)', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('redirige forzadamente a /profile cuando mustChangePassword=true y desbloquea al cambiar clave', async () => {
    const tempOperator: PlatformPrincipal = {
      adminId: 'admin-op-1',
      username: 'operador1',
      email: 'op1@platform.local',
      fullName: 'Operador Nuevo',
      role: 'PLATFORM_OPERATOR',
      mustChangePassword: true,
    };

    tokenStorage.saveSession('jwt-temp', 'ref-temp', tempOperator);

    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL) => {
        const url = String(input);
        if (url.endsWith('/api/v1/backoffice/auth/me')) {
          return new Response(JSON.stringify(tempOperator), {
            status: 200,
            headers: { 'Content-Type': 'application/json' },
          });
        }
        if (url.endsWith('/api/v1/backoffice/auth/change-password')) {
          return new Response(JSON.stringify({ ...tempOperator, mustChangePassword: false }), {
            status: 200,
            headers: { 'Content-Type': 'application/json' },
          });
        }
        if (url.endsWith('/api/v1/backoffice/dashboard/overview')) {
          return new Response(
            JSON.stringify({
              totalTenants: 4,
              statusBreakdown: { ACTIVE: 3, TRIAL: 1, GRACE_PERIOD: 0, SUSPENDED: 0 },
              overQuotaTenantsCount: 0,
              collectedRevenueThisMonth: 500,
              totalPendingDebt: 0,
              topTenantsBySales30d: [],
            }),
            { status: 200, headers: { 'Content-Type': 'application/json' } },
          );
        }
        return new Response('{}', { status: 404 });
      }),
    );

    renderWithRouter('/dashboard');

    await waitFor(() => {
      expect(screen.getByText(/Cambio de contraseña obligatorio/i)).toBeInTheDocument();
    });

    const user = userEvent.setup();
    await user.type(screen.getByLabelText(/Contraseña Actual/i), 'TempPassword123!');
    await user.type(screen.getByLabelText(/^Nueva Contraseña/i), 'NewStrongPassword2026!');
    await user.type(screen.getByLabelText(/Confirmar Nueva Contraseña/i), 'NewStrongPassword2026!');
    await user.click(screen.getByRole('button', { name: /Actualizar Contraseña/i }));

    await waitFor(() => {
      expect(screen.getByText(/Resumen Ejecutivo SaaS/i)).toBeInTheDocument();
    });
  });

  it('oculta ruta /platform/admins a PLATFORM_OPERATOR y muestra todos los módulos a SUPERADMIN', async () => {
    const operator: PlatformPrincipal = {
      adminId: 'admin-op-2',
      username: 'operador2',
      email: 'op2@platform.local',
      fullName: 'Operador Soporte',
      role: 'PLATFORM_OPERATOR',
      mustChangePassword: false,
    };

    tokenStorage.saveSession('jwt-op', 'ref-op', operator);

    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL) => {
        const url = String(input);
        if (url.endsWith('/api/v1/backoffice/auth/me')) {
          return new Response(JSON.stringify(operator), { status: 200 });
        }
        if (url.endsWith('/api/v1/backoffice/dashboard/overview')) {
          return new Response(
            JSON.stringify({
              totalTenants: 1,
              statusBreakdown: { ACTIVE: 1, TRIAL: 0, GRACE_PERIOD: 0, SUSPENDED: 0 },
              overQuotaTenantsCount: 0,
              collectedRevenueThisMonth: 100,
              totalPendingDebt: 0,
              topTenantsBySales30d: [],
            }),
            { status: 200 },
          );
        }
        return new Response('{}', { status: 404 });
      }),
    );

    renderWithRouter('/platform/admins');

    await waitFor(() => {
      expect(screen.getByText(/Resumen Ejecutivo SaaS/i)).toBeInTheDocument();
    });

    expect(screen.queryByRole('link', { name: /Admins Plataforma/i })).not.toBeInTheDocument();
  });

  it('permite login exitoso y redirección a /dashboard', async () => {
    const sa: PlatformPrincipal = {
      adminId: 'sa-1',
      username: 'superadmin',
      email: 'sa@platform.local',
      fullName: 'Superadmin Principal',
      role: 'SUPERADMIN',
      mustChangePassword: false,
    };

    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL) => {
        const url = String(input);
        if (url.endsWith('/api/v1/backoffice/auth/login')) {
          return new Response(
            JSON.stringify({
              accessToken: 'jwt-sa',
              refreshToken: 'ref-sa',
              expiresInSeconds: 900,
              admin: sa,
            }),
            { status: 200 },
          );
        }
        if (url.endsWith('/api/v1/backoffice/dashboard/overview')) {
          return new Response(
            JSON.stringify({
              totalTenants: 2,
              statusBreakdown: { ACTIVE: 2, TRIAL: 0, GRACE_PERIOD: 0, SUSPENDED: 0 },
              overQuotaTenantsCount: 0,
              collectedRevenueThisMonth: 350,
              totalPendingDebt: 75,
              topTenantsBySales30d: [],
            }),
            { status: 200 },
          );
        }
        return new Response('{}', { status: 404 });
      }),
    );

    renderWithRouter('/login');
    const user = userEvent.setup();
    await user.type(screen.getByLabelText(/Usuario o Correo/i), 'superadmin');
    await user.type(screen.getByLabelText(/Contraseña/i), 'SuperSecretPass123!');
    await user.click(screen.getByRole('button', { name: /Ingresar al Backoffice/i }));

    await waitFor(() => {
      expect(screen.getByText(/Resumen Ejecutivo SaaS/i)).toBeInTheDocument();
      expect(screen.getByRole('link', { name: /Admins Plataforma/i })).toBeInTheDocument();
    });
  });
});
