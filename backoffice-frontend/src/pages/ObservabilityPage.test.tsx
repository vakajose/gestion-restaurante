import type { ReactNode } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AuthContext, type AuthContextValue } from '../context/authContext';
import { operationsApi } from '../services/backofficeApi';
import type { PlatformPrincipal } from '../types/api';
import { ObservabilityPage } from './ObservabilityPage';

const SUPERADMIN: PlatformPrincipal = {
  adminId: 'sa-1',
  username: 'superadmin',
  email: 'sa@platform.local',
  fullName: 'Superadmin',
  role: 'SUPERADMIN',
  mustChangePassword: false,
};

function renderWithAuth(ui: ReactNode) {
  const ctx: AuthContextValue = {
    admin: SUPERADMIN,
    isAuthenticated: true,
    isSuperAdmin: true,
    isBootstrapping: false,
    login: vi.fn(),
    logout: vi.fn(),
    refreshProfile: vi.fn(),
    applyUpdatedPrincipal: vi.fn(),
  };
  return render(<AuthContext.Provider value={ctx}>{ui}</AuthContext.Provider>);
}

describe('ObservabilityPage (TASK-616)', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('permite filtrar loggers en caliente reactivamente por nombre', async () => {
    vi.spyOn(operationsApi, 'getHealth').mockResolvedValue({
      status: 'UP',
      checkedAt: '2026-11-13T10:00:00Z',
      components: {},
    });
    vi.spyOn(operationsApi, 'getMetrics').mockResolvedValue({
      collectedAt: '2026-11-13T10:00:00Z',
      metrics: [],
      totalAvailableMeters: 1,
    });
    vi.spyOn(operationsApi, 'getLoggers').mockResolvedValue({
      supportedLevels: ['INFO', 'DEBUG'],
      loggers: [
        { name: 'com.restaurant.app.modules.pos', configuredLevel: 'INFO', effectiveLevel: 'INFO' },
        { name: 'com.restaurant.app.modules.catalog', configuredLevel: 'WARN', effectiveLevel: 'WARN' },
      ],
    });

    renderWithAuth(<ObservabilityPage />);
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: /Loggers en Caliente/i }));
    await screen.findByText('com.restaurant.app.modules.pos');

    await user.type(screen.getByLabelText(/Buscar Logger/i), 'catalog');

    expect(screen.queryByText('com.restaurant.app.modules.pos')).not.toBeInTheDocument();
    expect(screen.getByText('com.restaurant.app.modules.catalog')).toBeInTheDocument();
  });

  it('soporta búsqueda en detalle, paginación y visor JSON formateado en Auditoría Inmutable', async () => {
    vi.spyOn(operationsApi, 'getHealth').mockResolvedValue({
      status: 'UP',
      checkedAt: '2026-11-13T10:00:00Z',
      components: {},
    });
    vi.spyOn(operationsApi, 'getMetrics').mockResolvedValue({
      collectedAt: '2026-11-13T10:00:00Z',
      metrics: [],
      totalAvailableMeters: 1,
    });
    const searchSpy = vi.spyOn(operationsApi, 'searchAuditLogs').mockImplementation(async (q = {}) => ({
      content: [
        {
          id: q.page === 1 ? 'audit-2' : 'audit-1',
          actorAdminId: 'sa-1',
          actorUsername: 'superadmin',
          action: 'TENANT_USER_PASSWORD_RESET',
          targetType: 'APP_USER',
          targetId: q.page === 1 ? 'usr-page-2' : 'usr-page-1',
          detailsJson: '{"username":"cajero1","mustChangePassword":true}',
          ipAddress: '10.0.0.1',
          createdAt: '2026-11-13T12:00:00Z',
        },
      ],
      totalElements: 40,
      totalPages: 2,
      number: q.page ?? 0,
      size: q.size ?? 25,
    }));

    renderWithAuth(<ObservabilityPage />);
    const user = userEvent.setup();

    await user.click(await screen.findByRole('button', { name: /Auditoría Inmutable/i }));
    await screen.findByText('APP_USER: usr-page-1');

    // Buscar sobre detalle
    await user.type(screen.getByLabelText(/Buscar en Detalle/i), 'cajero1');
    await user.click(screen.getByRole('button', { name: /Filtrar/i }));

    await waitFor(() => {
      expect(searchSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          search: 'cajero1',
          page: 0,
        }),
      );
    });

    // Avanzar a página 2
    await user.click(screen.getByRole('button', { name: /Siguiente/i }));
    await screen.findByText('APP_USER: usr-page-2');

    // Abrir visor JSON formateado
    await user.click(screen.getByRole('button', { name: /Ver JSON/i }));
    const pre = await screen.findByTestId('audit-json-pre');
    expect(pre.textContent).toContain('"mustChangePassword": true');
  });
});
