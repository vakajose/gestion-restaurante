import type { ReactNode } from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AuthContext, type AuthContextValue } from '../../context/authContext';
import { ObservabilityPage } from '../../pages/ObservabilityPage';
import { PlatformSettingsPage } from '../../pages/PlatformSettingsPage';
import type { BackofficeTenantDetailDto, PlatformPrincipal, SubscriptionPlanDto } from '../../types/api';
import { TenantSubscriptionTab } from './TenantSubscriptionTab';

const SUPERADMIN_PRINCIPAL: PlatformPrincipal = {
  adminId: 'sa-1',
  username: 'superadmin',
  email: 'sa@platform.local',
  fullName: 'Superadmin',
  role: 'SUPERADMIN',
  mustChangePassword: false,
};

function renderWithAuth(ui: ReactNode) {
  const ctx: AuthContextValue = {
    admin: SUPERADMIN_PRINCIPAL,
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

const SAMPLE_DETAIL: BackofficeTenantDetailDto = {
  tenant: {
    id: 'tenant-1',
    legalName: 'Parrilla El Buen Sabor',
    nitOrTaxId: '123456',
    slug: 'buen-sabor',
    logoUrl: null,
    isActive: true,
    createdAt: '2026-11-01T10:00:00Z',
    updatedAt: '2026-11-01T10:00:00Z',
  },
  entitlements: {
    tenantId: 'tenant-1',
    managedSubscription: true,
    status: 'ACTIVE',
    effectiveSuspensionMode: 'NONE',
    suspensionReason: null,
    planCode: 'PRO_MONTHLY',
    planName: 'Plan Pro',
    currentPeriodStart: '2026-11-01T10:00:00Z',
    currentPeriodEnd: '2026-12-01T10:00:00Z',
    graceUntil: null,
    graceMaxOrders: null,
    graceOrdersUsed: 0,
    pendingBalance: 0,
    currency: 'BOB',
    loginAllowed: true,
    posOperationsAllowed: true,
    quotas: {
      BRANCHES: { used: 1, limit: 2, overQuota: false },
      USERS: { used: 2, limit: 5, overQuota: false },
      DISHES: { used: 10, limit: 50, overQuota: false },
      INVENTORY_ITEMS: { used: 20, limit: 100, overQuota: false },
      MONTHLY_ORDERS: { used: 15, limit: 500, overQuota: false },
    },
    overQuotaResources: [],
  },
  autoSuspendModeOnExpire: 'POS_AND_SHIFT_ONLY',
  graceNotes: null,
  structuralUsage: {
    activeBranches: 1,
    totalBranches: 1,
    activeUsers: 2,
    totalUsers: 2,
    activeDishes: 10,
    totalDishes: 10,
    totalIngredients: 20,
    monthlyOrders: 15,
    stockRecordsCount: 20,
    distinctStockedIngredients: 18,
    totalInventoryUnits: 150,
    totalInventoryValuation: 2500,
  },
  branches: [],
  salesLast30Days: 1200,
  paidOrdersLast30Days: 30,
};

const SAMPLE_PLANS: SubscriptionPlanDto[] = [
  {
    id: 'plan-weekly',
    code: 'WEEKLY_FLEX',
    name: 'Plan Semanal Flexible',
    description: 'Uso semanal',
    billingCycle: 'WEEKLY',
    durationDays: 7,
    price: 75,
    currency: 'BOB',
    maxBranches: 1,
    maxUsers: 5,
    maxDishes: 40,
    maxInventoryItems: 80,
    maxMonthlyOrders: 300,
    isActive: true,
    createdAt: '2026-11-01T00:00:00Z',
    updatedAt: '2026-11-01T00:00:00Z',
  },
];

describe('TenantSubscriptionTab, ObservabilityPage & PlatformSettingsPage', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('permite habilitar plan a crédito pendiente (markAsPaid=false) sin registro previo de pago', async () => {
    let capturedBody: Record<string, unknown> | null = null;
    vi.stubGlobal(
      'fetch',
      vi.fn(async (_input: RequestInfo | URL, init?: RequestInit) => {
        capturedBody = JSON.parse(String(init?.body ?? '{}')) as Record<string, unknown>;
        return new Response(JSON.stringify(SAMPLE_DETAIL.entitlements), { status: 200 });
      }),
    );

    const onUpdated = vi.fn().mockResolvedValue(undefined);
    const onSuccess = vi.fn();
    const onError = vi.fn();

    render(
      <TenantSubscriptionTab
        detail={SAMPLE_DETAIL}
        plans={SAMPLE_PLANS}
        isSuperAdmin
        onUpdated={onUpdated}
        onError={onError}
        onSuccess={onSuccess}
      />,
    );

    const user = userEvent.setup();
    await user.click(screen.getByLabelText(/Uso por adelantado \/ Crédito/i));
    await user.click(screen.getByRole('button', { name: /Habilitar Plan a Crédito/i }));

    await waitFor(() => {
      expect(capturedBody).not.toBeNull();
      expect(capturedBody?.markAsPaid).toBe(false);
      expect(capturedBody?.planId).toBe('plan-weekly');
      expect(onSuccess).toHaveBeenCalled();
    });
  });

  it('muestra alerta preventiva al cambiar un logger a DEBUG en caliente', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input);
        if (url.includes('/observability/health')) {
          return new Response(
            JSON.stringify({ status: 'UP', checkedAt: '2026-11-12T10:00:00Z', components: {} }),
            { status: 200 },
          );
        }
        if (url.includes('/observability/metrics')) {
          return new Response(
            JSON.stringify({ collectedAt: '2026-11-12T10:00:00Z', metrics: [], totalAvailableMeters: 12 }),
            { status: 200 },
          );
        }
        if (url.includes('/observability/loggers/com.restaurant.app.modules.pos') && init?.method === 'PUT') {
          return new Response(
            JSON.stringify({
              name: 'com.restaurant.app.modules.pos',
              configuredLevel: 'DEBUG',
              effectiveLevel: 'DEBUG',
            }),
            { status: 200 },
          );
        }
        if (url.includes('/observability/loggers')) {
          return new Response(
            JSON.stringify({
              supportedLevels: ['TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR', 'OFF'],
              loggers: [
                {
                  name: 'com.restaurant.app.modules.pos',
                  configuredLevel: 'INFO',
                  effectiveLevel: 'INFO',
                },
              ],
            }),
            { status: 200 },
          );
        }
        return new Response('{}', { status: 404 });
      }),
    );

    renderWithAuth(<ObservabilityPage />);

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /Loggers en Caliente/i }));

    const select = await screen.findByLabelText(/Nivel para com.restaurant.app.modules.pos/i);
    await user.selectOptions(select, 'DEBUG');

    await waitFor(() => {
      expect(
        screen.getByText(/Alerta preventiva: existen loggers en DEBUG o TRACE/i),
      ).toBeInTheDocument();
    });
  });

  it('ejecuta la prueba de conectividad del proveedor de almacenamiento activo', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
        const url = String(input);
        if (url.endsWith('/api/v1/backoffice/settings')) {
          return new Response(
            JSON.stringify([
              {
                key: 'storage.active_provider',
                value: 'S3',
                valueType: 'STRING',
                description: 'Proveedor activo',
                updatedAt: '2026-11-12T00:00:00Z',
                updatedBy: null,
              },
              {
                key: 'storage.max_file_size_bytes',
                value: '1048576',
                valueType: 'LONG',
                description: 'Límite en bytes',
                updatedAt: '2026-11-12T00:00:00Z',
                updatedBy: null,
              },
            ]),
            { status: 200 },
          );
        }
        if (url.endsWith('/api/v1/backoffice/storage/test-connection') && init?.method === 'POST') {
          return new Response(
            JSON.stringify({
              provider: 'S3',
              reachable: true,
              targetDescription: 's3://restaurant-media-public',
              message: 'Bucket OCI Object Storage accesible (HeadBucket OK)',
              checkedAt: '2026-11-12T23:00:00Z',
            }),
            { status: 200 },
          );
        }
        return new Response('{}', { status: 404 });
      }),
    );

    renderWithAuth(<PlatformSettingsPage />);

    const probeBtn = await screen.findByRole('button', {
      name: /Probar Conexión \(Proveedor Activo\)/i,
    });
    const user = userEvent.setup();
    await user.click(probeBtn);

    await waitFor(() => {
      expect(screen.getByText(/Proveedor Activo: S3/i)).toBeInTheDocument();
      expect(screen.getByText(/Bucket OCI Object Storage accesible/i)).toBeInTheDocument();
    });
  });
});
