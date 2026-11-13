import { useState, type FormEvent } from 'react';
import { KeyRound } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { AlertBanner } from '../components/AlertBanner';
import { StatusBadge } from '../components/StatusBadge';
import { useAuth } from '../context/authContext';
import { authApi } from '../services/backofficeApi';
import { ApiError } from '../services/httpClient';

export function ProfilePage() {
  const { admin, applyUpdatedPrincipal } = useAuth();
  const navigate = useNavigate();

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  if (!admin) return null;

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    if (newPassword.length < 12) {
      setError('La nueva contraseña de plataforma debe tener al menos 12 caracteres.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setError('La confirmación no coincide con la nueva contraseña.');
      return;
    }

    setSaving(true);
    try {
      const updated = await authApi.changePassword(currentPassword, newPassword);
      const wasForced = admin.mustChangePassword;
      applyUpdatedPrincipal(updated);
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setSuccess('Contraseña actualizada exitosamente.');
      if (wasForced && !updated.mustChangePassword) {
        navigate('/dashboard', { replace: true });
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : 'No se pudo actualizar la contraseña.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="max-w-2xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Mi Perfil de Plataforma</h1>
        <p className="text-sm text-slate-500 mt-1">
          Identidad administrativa separada del sistema de tenants.
        </p>
      </div>

      {admin.mustChangePassword && (
        <AlertBanner
          tone="warning"
          title="Cambio de contraseña obligatorio"
          message="Debes reemplazar la contraseña inicial o temporal antes de operar los módulos del Backoffice."
        />
      )}

      <div className="card p-6 grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm">
        <div>
          <p className="text-xs font-semibold uppercase text-slate-400">Nombre Completo</p>
          <p className="font-semibold text-slate-800 mt-0.5">{admin.fullName}</p>
        </div>
        <div>
          <p className="text-xs font-semibold uppercase text-slate-400">Rol de Plataforma</p>
          <div className="mt-1">
            <StatusBadge value={admin.role} />
          </div>
        </div>
        <div>
          <p className="text-xs font-semibold uppercase text-slate-400">Usuario</p>
          <p className="font-mono text-slate-700 mt-0.5">{admin.username}</p>
        </div>
        <div>
          <p className="text-xs font-semibold uppercase text-slate-400">Correo</p>
          <p className="text-slate-700 mt-0.5">{admin.email}</p>
        </div>
      </div>

      <div className="card p-6 space-y-4">
        <div className="flex items-center gap-2">
          <KeyRound className="h-5 w-5 text-primary" />
          <h2 className="text-lg font-bold text-slate-800">Cambiar Contraseña</h2>
        </div>

        {error && <AlertBanner tone="error" message={error} />}
        {success && <AlertBanner tone="success" message={success} />}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="currentPassword" className="label-text">
              Contraseña Actual
            </label>
            <input
              id="currentPassword"
              type="password"
              required
              className="input-field"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label htmlFor="newPassword" className="label-text">
                Nueva Contraseña (mín. 12 caracteres)
              </label>
              <input
                id="newPassword"
                type="password"
                required
                minLength={12}
                className="input-field"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
              />
            </div>

            <div>
              <label htmlFor="confirmPassword" className="label-text">
                Confirmar Nueva Contraseña
              </label>
              <input
                id="confirmPassword"
                type="password"
                required
                minLength={12}
                className="input-field"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
              />
            </div>
          </div>

          <button type="submit" disabled={saving} className="btn-primary">
            {saving ? 'Guardando...' : 'Actualizar Contraseña'}
          </button>
        </form>
      </div>
    </div>
  );
}
