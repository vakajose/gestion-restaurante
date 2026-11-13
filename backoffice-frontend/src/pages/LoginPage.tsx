import { useState, type FormEvent } from 'react';
import { Lock, ShieldCheck } from 'lucide-react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { AlertBanner } from '../components/AlertBanner';
import { useAuth } from '../context/authContext';
import { ApiError } from '../services/httpClient';

export function LoginPage() {
  const { isAuthenticated, admin, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [loginInput, setLoginInput] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (isAuthenticated && admin) {
    return <Navigate to={admin.mustChangePassword ? '/profile' : '/dashboard'} replace />;
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    if (!loginInput.trim() || !password) {
      setError('Ingresa tu usuario/email y contraseña de plataforma.');
      return;
    }

    setSubmitting(true);
    try {
      const principal = await login(loginInput.trim(), password);
      if (principal.mustChangePassword) {
        navigate('/profile', { replace: true });
      } else {
        const from = (location.state as { from?: string } | null)?.from;
        navigate(from && from !== '/login' ? from : '/dashboard', { replace: true });
      }
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.detail);
      } else {
        setError('No se pudo conectar con el servidor del Backoffice.');
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-canvas px-4 py-10">
      <div className="w-full max-w-md card p-8 space-y-6">
        <div className="flex flex-col items-center text-center">
          <div className="h-12 w-12 rounded-2xl bg-primary-light text-primary flex items-center justify-center mb-3">
            <ShieldCheck className="h-7 w-7" />
          </div>
          <h1 className="text-xl font-bold text-slate-900">Control Plane Backoffice</h1>
          <p className="text-sm text-slate-500 mt-1">
            Consola Supra-Tenant aislada del ERP operativo
          </p>
        </div>

        {error && <AlertBanner tone="error" message={error} />}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="login" className="label-text">
              Usuario o Correo Electrónico
            </label>
            <input
              id="login"
              type="text"
              autoComplete="username"
              className="input-field"
              placeholder="superadmin o email@plataforma.com"
              value={loginInput}
              onChange={(e) => setLoginInput(e.target.value)}
            />
          </div>

          <div>
            <label htmlFor="password" className="label-text">
              Contraseña
            </label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              className="input-field"
              placeholder="••••••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>

          <button type="submit" disabled={submitting} className="btn-primary w-full py-2.5">
            <Lock className="h-4 w-4" />
            {submitting ? 'Autenticando...' : 'Ingresar al Backoffice'}
          </button>
        </form>
      </div>
    </div>
  );
}
