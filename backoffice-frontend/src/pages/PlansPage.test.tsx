import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AuthContext } from '../context/authContext';
import { plansApi } from '../services/backofficeApi';
import { PlansPage } from './PlansPage';

function renderPlansPage() {
  return render(
    <AuthContext.Provider
      value={{
        admin: {
          adminId: 'admin-1',
          username: 'superadmin',
          email: 'sa@platform.local',
          fullName: 'Super Admin',
          role: 'SUPERADMIN',
          mustChangePassword: false,
        },
        isAuthenticated: true,
        isSuperAdmin: true,
        isBootstrapping: false,
        login: vi.fn(),
        logout: vi.fn(),
        refreshProfile: vi.fn(),
        applyUpdatedPrincipal: vi.fn(),
      }}
    >
      <PlansPage />
    </AuthContext.Provider>,
  );
}

describe('PlansPage - Planes Gratis Ilimitados y Categorización (TASK-623)', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('filtra por categoría y crea un plan gratuito ilimitado con durationDays=0 y límites null', async () => {
    const listSpy = vi.spyOn(plansApi, 'listAll').mockResolvedValue([
      {
        id: 'p-1',
        code: 'VIP_FREE',
        name: 'Cortesía VIP',
        description: 'Plan especial',
        billingCycle: 'CUSTOM',
        planCategory: 'CUSTOM_PRIVATE',
        durationDays: 0,
        price: 0,
        currency: 'BOB',
        maxBranches: null,
        maxUsers: null,
        maxDishes: null,
        maxInventoryItems: null,
        maxMonthlyOrders: null,
        isActive: true,
        createdAt: '2026-11-10T00:00:00Z',
        updatedAt: '2026-11-10T00:00:00Z',
      },
    ]);
    const createSpy = vi.spyOn(plansApi, 'create').mockResolvedValue({
      id: 'p-2',
      code: 'VIP_NEW',
      name: 'Cortesía Ilimitada',
      description: null,
      billingCycle: 'CUSTOM',
      planCategory: 'CUSTOM_PRIVATE',
      durationDays: 0,
      price: 0,
      currency: 'BOB',
      maxBranches: null,
      maxUsers: null,
      maxDishes: null,
      maxInventoryItems: null,
      maxMonthlyOrders: null,
      isActive: true,
      createdAt: '2026-11-10T00:00:00Z',
      updatedAt: '2026-11-10T00:00:00Z',
    });

    renderPlansPage();

    expect(await screen.findByText('Cortesía VIP')).toBeTruthy();
    expect(screen.getByText('Especial / Privado')).toBeTruthy();
    expect(screen.getByText(/Sin vencimiento/)).toBeTruthy();

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /Especiales \/ Privados/i }));
    await waitFor(() => {
      expect(listSpy).toHaveBeenCalledWith('CUSTOM_PRIVATE');
    });

    await user.click(screen.getByRole('button', { name: /Nuevo Plan/i }));
    await user.type(screen.getByLabelText(/Código Único/i), 'vip_new');
    await user.type(screen.getByLabelText(/Nombre Comercial/i), 'Cortesía Ilimitada');
    await user.click(screen.getByRole('button', { name: /Preset: Plan Gratis Ilimitado/i }));
    await user.click(screen.getByRole('button', { name: /Guardar Plan/i }));

    await waitFor(() => {
      expect(createSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          code: 'VIP_NEW',
          planCategory: 'CUSTOM_PRIVATE',
          billingCycle: 'CUSTOM',
          durationDays: 0,
          price: 0,
          maxBranches: null,
          maxUsers: null,
          maxDishes: null,
          maxInventoryItems: null,
          maxMonthlyOrders: null,
        }),
      );
    });
  });
});
