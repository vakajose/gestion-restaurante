import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Plus, Power, ShieldAlert, X } from 'lucide-react';
import { AlertBanner } from '../components/AlertBanner';
import { StatusBadge } from '../components/StatusBadge';
import { useAuth } from '../context/authContext';
import { adminsApi } from '../services/backofficeApi';
import { ApiError } from '../services/httpClient';
import type { PlatformAdminDto, PlatformRole } from '../types/api';
import { formatInstantDateTime } from '../utils/formatters';

export function AdminsPage() {
  const { admin: currentAdmin } = useAuth();
  const [admins, setAdmins] = useState<PlatformAdminDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);

  const [modalOpen, setModalOpen] = useState(false);
  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [fullName, setFullName] = useState('');
  const [initialPassword, setInitialPassword] = useState('');
  const [role, setRole] = useState<PlatformRole>('PLATFORM_OPERATOR');
  const [saving, setSaving] = useState(false);

  const loadAdmins = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setAdmins(await adminsApi.listAll());
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : 'No se pudo cargar el equipo backoffice.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadAdmins();
  }, [loadAdmins]);

  const handleCreate = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setFeedback(null);
    if (initialPassword.length < 12) {
      setError('La contraseña inicial debe tener al menos 12 caracteres.');
      return;
    }

    setSaving(true);
    try {
      await adminsApi.create({
        username: username.trim(),
        email: email.trim(),
        fullName: fullName.trim(),
        initialPassword,
        role,
      });
      setModalOpen(false);
      setUsername('');
      setEmail('');
      setFullName('');
      setInitialPassword('');
      setRole('PLATFORM_OPERATOR');
      setFeedback('Administrador de plataforma creado exitosamente (deberá cambiar clave al ingresar).');
      await loadAdmins();
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : 'Error creando administrador.');
    } finally {
      setSaving(false);
    }
  };

  const handleToggle = async (target: PlatformAdminDto) => {
    setError(null);
    setFeedback(null);
    try {
      await adminsApi.updateStatus(target.id, !target.isActive);
      setFeedback(`Cuenta ${target.username} ${!target.isActive ? 'activada' : 'desactivada'}.`);
      await loadAdmins();
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : 'No se pudo actualizar el estado de la cuenta.');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Equipo Backoffice (`platform_admins`)</h1>
          <p className="text-sm text-slate-500 mt-1">
            Gestión exclusiva de Superadmins y Operadores de Soporte con resguardo del último Superadmin activo.
          </p>
        </div>
        <button type="button" onClick={() => setModalOpen(true)} className="btn-primary">
          <Plus className="h-4 w-4" />
          Nuevo Administrador
        </button>
      </div>

      {error && <AlertBanner tone="error" message={error} />}
      {feedback && <AlertBanner tone="success" message={feedback} />}

      <div className="card overflow-hidden">
        {loading ? (
          <div className="p-6 text-sm text-slate-500">Cargando cuentas de plataforma...</div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 border-b border-border text-xs uppercase text-slate-500">
                <tr>
                  <th className="py-3 px-4">Usuario</th>
                  <th className="py-3 px-4">Nombre</th>
                  <th className="py-3 px-4">Rol</th>
                  <th className="py-3 px-4">Estado</th>
                  <th className="py-3 px-4">Último Login</th>
                  <th className="py-3 px-4 text-right">Acciones</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {admins.map((item) => {
                  const isSelf = currentAdmin?.adminId === item.id;
                  return (
                    <tr key={item.id} className="hover:bg-slate-50/70">
                      <td className="py-3 px-4">
                        <p className="font-mono font-semibold text-slate-800">{item.username}</p>
                        <p className="text-xs text-slate-500">{item.email}</p>
                      </td>
                      <td className="py-3 px-4 text-slate-800">
                        {item.fullName}
                        {item.mustChangePassword && (
                          <span className="ml-2 inline-flex items-center gap-1 rounded bg-warning-light px-1.5 py-0.5 text-[10px] font-semibold text-warning">
                            <ShieldAlert className="h-3 w-3" /> Clave Temporal
                          </span>
                        )}
                      </td>
                      <td className="py-3 px-4">
                        <StatusBadge value={item.role} />
                      </td>
                      <td className="py-3 px-4">
                        <StatusBadge
                          value={item.isActive ? 'ACTIVE' : 'SUSPENDED'}
                          customLabel={item.isActive ? 'Activo' : 'Inactivo'}
                        />
                      </td>
                      <td className="py-3 px-4 text-xs text-slate-500">
                        {formatInstantDateTime(item.lastLoginAt)}
                      </td>
                      <td className="py-3 px-4 text-right">
                        <button
                          type="button"
                          disabled={isSelf}
                          title={isSelf ? 'No puedes desactivar tu propia cuenta' : undefined}
                          onClick={() => handleToggle(item)}
                          className="btn-secondary text-xs"
                        >
                          <Power className="h-3.5 w-3.5" />
                          {item.isActive ? 'Desactivar' : 'Activar'}
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4">
          <div className="card w-full max-w-md p-6 space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-bold text-slate-900">Alta de Administrador</h2>
              <button
                type="button"
                onClick={() => setModalOpen(false)}
                className="text-slate-400 hover:text-slate-700"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <form onSubmit={handleCreate} className="space-y-3">
              <div>
                <label htmlFor="admin-username" className="label-text">
                  Username
                </label>
                <input
                  id="admin-username"
                  required
                  className="input-field"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                />
              </div>
              <div>
                <label htmlFor="admin-email" className="label-text">
                  Email
                </label>
                <input
                  id="admin-email"
                  type="email"
                  required
                  className="input-field"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                />
              </div>
              <div>
                <label htmlFor="admin-fullname" className="label-text">
                  Nombre Completo
                </label>
                <input
                  id="admin-fullname"
                  required
                  className="input-field"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                />
              </div>
              <div>
                <label htmlFor="admin-role" className="label-text">
                  Rol
                </label>
                <select
                  id="admin-role"
                  className="input-field"
                  value={role}
                  onChange={(e) => setRole(e.target.value as PlatformRole)}
                >
                  <option value="PLATFORM_OPERATOR">PLATFORM_OPERATOR (Soporte / Cobros)</option>
                  <option value="SUPERADMIN">SUPERADMIN (Control Total)</option>
                </select>
              </div>
              <div>
                <label htmlFor="admin-password" className="label-text">
                  Contraseña Temporal (mín. 12 caracteres)
                </label>
                <input
                  id="admin-password"
                  type="password"
                  required
                  minLength={12}
                  className="input-field"
                  value={initialPassword}
                  onChange={(e) => setInitialPassword(e.target.value)}
                />
              </div>
              <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
                <button type="button" onClick={() => setModalOpen(false)} className="btn-secondary">
                  Cancelar
                </button>
                <button type="submit" disabled={saving} className="btn-primary">
                  {saving ? 'Creando...' : 'Crear Cuenta'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
