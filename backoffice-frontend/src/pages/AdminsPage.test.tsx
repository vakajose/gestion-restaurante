import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AuthContext } from '../context/authContext';
import { adminsApi, authApi } from '../services/backofficeApi';
import { AdminsPage } from './AdminsPage';

function renderAdminsPage() {
  return render(
    <AuthContext.Provider
      value={{
        admin: {
          adminId: 'sa-1',
          username: 'superadmin',
          email: null,
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
      <AdminsPage />
    </AuthContext.Provider>,
  );
}

describe('AdminsPage - Email opcional y política configurable (TASK-624)', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('permite crear un administrador sin correo electrónico respetando la longitud mínima configurada (8 chars)', async () => {
    vi.spyOn(adminsApi, 'listAll').mockResolvedValue([]);
    vi.spyOn(authApi, 'getPasswordPolicy').mockResolvedValue({
      minLength: 8,
      requireComplexity: false,
    });
    const createSpy = vi.spyOn(adminsApi, 'create').mockResolvedValue({
      id: 'adm-2',
      username: 'soporte1',
      email: null,
      fullName: 'Soporte Uno',
      role: 'PLATFORM_OPERATOR',
      isActive: true,
      mustChangePassword: true,
      lastLoginAt: null,
      createdAt: '2026-11-12T00:00:00Z',
    });

    renderAdminsPage();

    const user = userEvent.setup();
    await user.click(await screen.findByRole('button', { name: /Nuevo Administrador/i }));

    const emailInput = screen.getByLabelText(/Correo Electrónico \(Opcional\)/i) as HTMLInputElement;
    expect(emailInput.required).toBe(false);
    expect(screen.getByText(/mín\. 8 caracteres/i)).toBeTruthy();

    await user.type(screen.getByLabelText(/Username/i), 'soporte1');
    await user.type(screen.getByLabelText(/Nombre Completo/i), 'Soporte Uno');
    await user.type(screen.getByLabelText(/Contraseña Temporal/i), 'Clave123');
    await user.click(screen.getByRole('button', { name: /Crear Cuenta/i }));

    await waitFor(() => {
      expect(createSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          username: 'soporte1',
          email: null,
          fullName: 'Soporte Uno',
          initialPassword: 'Clave123',
        }),
      );
    });
  });
});
