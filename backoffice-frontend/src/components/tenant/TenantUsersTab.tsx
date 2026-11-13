import { useMemo, useState, type FormEvent } from 'react';
import { Check, Copy, Eye, EyeOff, KeyRound, RefreshCw, Search, ShieldCheck, UserCheck, UserX, X } from 'lucide-react';
import { StatusBadge } from '../StatusBadge';
import { tenantsApi } from '../../services/backofficeApi';
import { ApiError } from '../../services/httpClient';
import type { TenantUserDto } from '../../types/api';
import { formatInstantDateTime } from '../../utils/formatters';

interface Props {
  tenantId: string;
  users: TenantUserDto[];
  isSuperAdmin: boolean;
  onUpdated: () => Promise<void>;
  onError: (msg: string) => void;
  onSuccess: (msg: string) => void;
}

function generateTempPassword(length = 12): string {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%';
  const array = new Uint32Array(length);
  crypto.getRandomValues(array);
  return Array.from(array, (n) => chars[n % chars.length]).join('');
}

export function TenantUsersTab({
  tenantId,
  users,
  isSuperAdmin,
  onUpdated,
  onError,
  onSuccess,
}: Props) {
  const [search, setSearch] = useState('');
  const [busyUserId, setBusyUserId] = useState<string | null>(null);
  const [resetTarget, setResetTarget] = useState<TenantUserDto | null>(null);
  const [newPassword, setNewPassword] = useState('');
  const [showPassword, setShowPassword] = useState(true);
  const [copied, setCopied] = useState(false);
  const [submittingReset, setSubmittingReset] = useState(false);

  const filteredUsers = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return users;
    return users.filter(
      (u) =>
        u.username.toLowerCase().includes(q) ||
        (u.fullName ?? '').toLowerCase().includes(q) ||
        (u.email ?? '').toLowerCase().includes(q) ||
        u.role.toLowerCase().includes(q),
    );
  }, [users, search]);

  const openResetModal = (user: TenantUserDto) => {
    setResetTarget(user);
    setNewPassword(generateTempPassword());
    setShowPassword(true);
    setCopied(false);
  };

  const handleCopy = async () => {
    if (!newPassword) return;
    try {
      await navigator.clipboard.writeText(newPassword);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      onError('No se pudo copiar al portapapeles.');
    }
  };

  const handleResetSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!resetTarget) return;
    if (newPassword.trim().length < 6) {
      onError('La nueva contraseña debe tener al menos 6 caracteres.');
      return;
    }

    setSubmittingReset(true);
    try {
      await tenantsApi.resetUserPassword(tenantId, resetTarget.id, newPassword);
      const targetUsername = resetTarget.username;
      setResetTarget(null);
      setNewPassword('');
      onSuccess(
        `Contraseña restablecida para '${targetUsername}'. Se exigirá cambio obligatorio en su próximo ingreso.`,
      );
      await onUpdated();
    } catch (err) {
      onError(err instanceof ApiError ? err.detail : 'Error al restablecer la contraseña.');
    } finally {
      setSubmittingReset(false);
    }
  };

  const handleToggleStatus = async (user: TenantUserDto) => {
    setBusyUserId(user.id);
    try {
      const nextState = !user.isActive;
      await tenantsApi.toggleUserStatus(tenantId, user.id, nextState);
      onSuccess(
        `Usuario '${user.username}' ${nextState ? 'activado' : 'bloqueado/desactivado'} correctamente.`,
      );
      await onUpdated();
    } catch (err) {
      onError(err instanceof ApiError ? err.detail : 'Error actualizando el estado del usuario.');
    } finally {
      setBusyUserId(null);
    }
  };

  return (
    <div className="space-y-4">
      <div className="card p-4 flex flex-wrap items-center justify-between gap-3">
        <div className="relative flex-1 min-w-[240px] max-w-md">
          <Search className="h-4 w-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            className="input-field pl-9"
            placeholder="Buscar por usuario, nombre, correo o rol..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <div className="text-xs text-slate-500">
          Mostrando <span className="font-bold text-slate-800">{filteredUsers.length}</span> de{' '}
          <span className="font-bold text-slate-800">{users.length}</span> usuarios
        </div>
      </div>

      <div className="card overflow-hidden">
        {filteredUsers.length === 0 ? (
          <div className="p-8 text-center text-sm text-slate-500">
            No se encontraron usuarios para este inquilino con el criterio aplicado.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 border-b border-border text-xs uppercase text-slate-500">
                <tr>
                  <th className="py-3 px-4">Usuario</th>
                  <th className="py-3 px-4">Rol</th>
                  <th className="py-3 px-4">Contacto</th>
                  <th className="py-3 px-4">Estado</th>
                  <th className="py-3 px-4">Creado</th>
                  <th className="py-3 px-4 text-right">Acciones</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredUsers.map((u) => (
                  <tr key={u.id} className="hover:bg-slate-50/70">
                    <td className="py-3 px-4">
                      <p className="font-semibold text-slate-900 font-mono text-xs">{u.username}</p>
                      <p className="text-xs text-slate-500">{u.fullName ?? 'Sin nombre'}</p>
                    </td>
                    <td className="py-3 px-4">
                      <span className="inline-flex items-center gap-1 rounded-md bg-slate-100 px-2 py-0.5 text-xs font-semibold text-slate-700">
                        <ShieldCheck className="h-3.5 w-3.5 text-primary" />
                        {u.role}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-xs text-slate-600">
                      <p>{u.email ?? '—'}</p>
                      {u.phoneNumber && <p className="text-slate-400">{u.phoneNumber}</p>}
                    </td>
                    <td className="py-3 px-4">
                      <div className="flex flex-wrap items-center gap-1.5">
                        <StatusBadge
                          value={u.isActive ? 'ACTIVE' : 'SUSPENDED'}
                          customLabel={u.isActive ? 'Activo' : 'Bloqueado'}
                        />
                        {u.mustChangePassword && (
                          <span className="inline-flex rounded-full bg-warning-light px-2 py-0.5 text-[11px] font-semibold text-warning">
                            Cambio de clave pendiente
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="py-3 px-4 text-xs text-slate-500 whitespace-nowrap">
                      {formatInstantDateTime(u.createdAt)}
                    </td>
                    <td className="py-3 px-4 text-right">
                      {isSuperAdmin ? (
                        <div className="inline-flex items-center gap-2">
                          <button
                            type="button"
                            onClick={() => openResetModal(u)}
                            className="btn-secondary text-xs py-1.5 px-2.5"
                          >
                            <KeyRound className="h-3.5 w-3.5" />
                            Restablecer Clave
                          </button>
                          <button
                            type="button"
                            disabled={busyUserId === u.id}
                            onClick={() => void handleToggleStatus(u)}
                            className="btn-secondary text-xs py-1.5 px-2.5"
                          >
                            {u.isActive ? (
                              <>
                                <UserX className="h-3.5 w-3.5 text-danger" />
                                Desactivar
                              </>
                            ) : (
                              <>
                                <UserCheck className="h-3.5 w-3.5 text-success" />
                                Activar
                              </>
                            )}
                          </button>
                        </div>
                      ) : (
                        <span className="text-xs text-slate-400">Solo lectura</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {resetTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4">
          <div className="card w-full max-w-md p-6 space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold text-slate-900">Restablecer Contraseña</h3>
              <button
                type="button"
                onClick={() => setResetTarget(null)}
                className="text-slate-400 hover:text-slate-700"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <p className="text-xs text-slate-600">
              Usuario destino:{' '}
              <span className="font-mono font-bold text-slate-900">{resetTarget.username}</span>{' '}
              {resetTarget.fullName ? `(${resetTarget.fullName})` : ''}
            </p>

            <form onSubmit={handleResetSubmit} className="space-y-4">
              <div>
                <div className="flex items-center justify-between mb-1">
                  <label htmlFor="reset-temp-pass" className="label-text mb-0">
                    Nueva Contraseña Temporal
                  </label>
                  <button
                    type="button"
                    onClick={() => {
                      setNewPassword(generateTempPassword());
                      setCopied(false);
                    }}
                    className="inline-flex items-center gap-1 text-xs font-semibold text-primary hover:underline"
                  >
                    <RefreshCw className="h-3 w-3" />
                    Generar aleatoria
                  </button>
                </div>

                <div className="flex items-center gap-1.5">
                  <div className="relative flex-1">
                    <input
                      id="reset-temp-pass"
                      type={showPassword ? 'text' : 'password'}
                      required
                      minLength={6}
                      className="input-field font-mono pr-9"
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                    />
                    <button
                      type="button"
                      aria-label={showPassword ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                      onClick={() => setShowPassword((v) => !v)}
                      className="absolute right-2.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-700"
                    >
                      {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                  </div>

                  <button
                    type="button"
                    onClick={() => void handleCopy()}
                    className="btn-secondary text-xs py-2 px-3 shrink-0"
                    title="Copiar contraseña temporal"
                  >
                    {copied ? (
                      <>
                        <Check className="h-3.5 w-3.5 text-success" />
                        Copiada
                      </>
                    ) : (
                      <>
                        <Copy className="h-3.5 w-3.5" />
                        Copiar
                      </>
                    )}
                  </button>
                </div>
              </div>

              <div className="rounded-lg bg-warning-light/60 border border-amber-200 px-3 py-2 text-xs text-warning">
                Al confirmar, se revocarán las sesiones activas del usuario y se marcará{' '}
                <span className="font-mono font-semibold">mustChangePassword = true</span> para su próximo ingreso.
              </div>

              <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
                <button
                  type="button"
                  onClick={() => setResetTarget(null)}
                  className="btn-secondary"
                >
                  Cancelar
                </button>
                <button type="submit" disabled={submittingReset} className="btn-primary">
                  {submittingReset ? 'Aplicando...' : 'Confirmar Restablecimiento'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
