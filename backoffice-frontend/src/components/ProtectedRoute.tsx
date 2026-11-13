import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../context/authContext';

interface ProtectedRouteProps {
  requireSuperAdmin?: boolean;
}

export function ProtectedRoute({ requireSuperAdmin = false }: ProtectedRouteProps) {
  const { isAuthenticated, isSuperAdmin, isBootstrapping, admin } = useAuth();
  const location = useLocation();

  if (isBootstrapping) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-canvas text-slate-500 text-sm">
        Verificando sesión de plataforma...
      </div>
    );
  }

  if (!isAuthenticated || !admin) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }

  if (admin.mustChangePassword && location.pathname !== '/profile') {
    return <Navigate to="/profile" replace />;
  }

  if (requireSuperAdmin && !isSuperAdmin) {
    return <Navigate to="/dashboard" replace />;
  }

  return <Outlet />;
}
