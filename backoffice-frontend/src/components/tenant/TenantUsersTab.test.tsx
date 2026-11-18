import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { tenantsApi } from '../../services/backofficeApi';
import type { TenantUserDto } from '../../types/api';
import { TenantUsersTab } from './TenantUsersTab';

const SAMPLE_USERS: TenantUserDto[] = [
  {
    id: 'u-1',
    tenantId: 't-1',
    branchId: 'b-1',
    username: 'admin.central',
    fullName: 'Lucia Admin',
    email: 'lucia@restaurante.bo',
    phoneNumber: '70011223',
    role: 'ADMIN_TENANT',
    isActive: true,
    roleValidUntil: null,
    mustChangePassword: false,
    authorizedBranchIds: ['b-1'],
    createdAt: '2026-11-10T10:00:00Z',
  },
  {
    id: 'u-2',
    tenantId: 't-1',
    branchId: 'b-1',
    username: 'cajero.norte',
    fullName: 'Marco Cajero',
    email: null,
    phoneNumber: null,
    role: 'CASHIER',
    isActive: false,
    roleValidUntil: null,
    mustChangePassword: true,
    authorizedBranchIds: ['b-1'],
    createdAt: '2026-11-11T10:00:00Z',
  },
];

describe('TenantUsersTab (TASK-625)', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('filtra usuarios por búsqueda y permite restablecer contraseña temporal', async () => {
    const resetSpy = vi.spyOn(tenantsApi, 'resetUserPassword').mockResolvedValue({
      ...SAMPLE_USERS[0],
      mustChangePassword: true,
    });
    const onUpdated = vi.fn().mockResolvedValue(undefined);
    const onSuccess = vi.fn();
    const onError = vi.fn();

    render(
      <TenantUsersTab
        tenantId="t-1"
        users={SAMPLE_USERS}
        isSuperAdmin={true}
        onUpdated={onUpdated}
        onError={onError}
        onSuccess={onSuccess}
      />,
    );

    const user = userEvent.setup();
    const searchInput = screen.getByPlaceholderText(/Buscar por usuario/i);
    await user.type(searchInput, 'admin.central');

    expect(screen.getByText('Lucia Admin')).toBeInTheDocument();
    expect(screen.queryByText('Marco Cajero')).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /Restablecer Clave/i }));

    const passInput = screen.getByLabelText(/Nueva Contraseña Temporal/i) as HTMLInputElement;
    expect(passInput.value.length).toBeGreaterThanOrEqual(8);

    await user.clear(passInput);
    await user.type(passInput, 'Temp2026!');

    await user.click(screen.getByRole('button', { name: /Confirmar Restablecimiento/i }));

    await waitFor(() => {
      expect(resetSpy).toHaveBeenCalledWith('t-1', 'u-1', 'Temp2026!');
      expect(onSuccess).toHaveBeenCalled();
      expect(onUpdated).toHaveBeenCalled();
    });
  });

  it('permite activar o desactivar un usuario del tenant', async () => {
    const toggleSpy = vi.spyOn(tenantsApi, 'toggleUserStatus').mockResolvedValue({
      ...SAMPLE_USERS[1],
      isActive: true,
    });
    const onUpdated = vi.fn().mockResolvedValue(undefined);
    const onSuccess = vi.fn();

    render(
      <TenantUsersTab
        tenantId="t-1"
        users={[SAMPLE_USERS[1]]}
        isSuperAdmin={true}
        onUpdated={onUpdated}
        onError={vi.fn()}
        onSuccess={onSuccess}
      />,
    );

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /Activar/i }));

    await waitFor(() => {
      expect(toggleSpy).toHaveBeenCalledWith('t-1', 'u-2', true);
      expect(onSuccess).toHaveBeenCalled();
      expect(onUpdated).toHaveBeenCalled();
    });
  });
});
