import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { tenantsApi } from '../services/backofficeApi';
import { TenantsPage } from './TenantsPage';

describe('TenantsPage - Provisionamiento con campos opcionales (TASK-617)', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('permite aprovisionar un nuevo inquilino sin dirección ni correo del admin', async () => {
    vi.spyOn(tenantsApi, 'listTenants').mockResolvedValue([]);
    const provisionSpy = vi.spyOn(tenantsApi, 'provisionTenant').mockResolvedValue({
      tenantId: 't-100',
      branchId: 'b-100',
      adminUserId: 'u-100',
      slug: 'el-mirador',
    });

    render(
      <MemoryRouter>
        <TenantsPage />
      </MemoryRouter>,
    );

    const user = userEvent.setup();
    await user.click(await screen.findByRole('button', { name: /Nuevo Inquilino/i }));

    await user.type(screen.getByLabelText(/Razón Social/i), 'El Mirador');
    await user.type(screen.getByLabelText(/Nombre Completo/i), 'Carlos Admin');
    await user.type(screen.getByLabelText(/Username/i), 'admin.mirador');
    await user.type(screen.getByLabelText(/Contraseña Inicial/i), 'Secret123!');

    const addressInput = screen.getByLabelText(/Dirección/i) as HTMLInputElement;
    const emailInput = screen.getByLabelText(/Correo/i) as HTMLInputElement;
    expect(addressInput.required).toBe(false);
    expect(emailInput.required).toBe(false);

    await user.click(screen.getByRole('button', { name: /Crear Inquilino/i }));

    await waitFor(() => {
      expect(provisionSpy).toHaveBeenCalledTimes(1);
    });

    expect(provisionSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        company: expect.objectContaining({
          legalName: 'El Mirador',
          slug: 'el-mirador',
        }),
        branch: expect.objectContaining({
          name: 'Casa Matriz',
          address: null,
        }),
        adminUser: expect.objectContaining({
          fullName: 'Carlos Admin',
          username: 'admin.mirador',
          email: null,
        }),
      }),
    );
  });
});
