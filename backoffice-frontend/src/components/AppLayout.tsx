import { useState } from 'react';
import {
  Activity,
  Building2,
  CreditCard,
  LayoutDashboard,
  LogOut,
  Menu,
  Settings,
  ShieldCheck,
  UserCog,
  X,
} from 'lucide-react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/authContext';
import { StatusBadge } from './StatusBadge';

export function AppLayout() {
  const { admin, isSuperAdmin, logout } = useAuth();
  const navigate = useNavigate();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const navItems = [
    { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard, superAdminOnly: false },
    { to: '/tenants', label: 'Inquilinos', icon: Building2, superAdminOnly: false },
    { to: '/plans', label: 'Planes SaaS', icon: CreditCard, superAdminOnly: false },
    { to: '/platform/settings', label: 'Storage & Config', icon: Settings, superAdminOnly: false },
    { to: '/platform/observability', label: 'Observabilidad & Logs', icon: Activity, superAdminOnly: false },
    { to: '/platform/admins', label: 'Admins Plataforma', icon: UserCog, superAdminOnly: true },
  ].filter((item) => !item.superAdminOnly || isSuperAdmin);

  const handleLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  return (
    <div className="min-h-screen flex bg-canvas">
      {/* Sidebar Desktop / Mobile Drawer */}
      <aside
        className={`fixed inset-y-0 left-0 z-40 w-64 bg-sidebar text-sidebar-text border-r border-sidebar-border flex flex-col transition-transform duration-200 lg:static lg:translate-x-0 ${
          mobileMenuOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <div className="flex items-center justify-between h-16 px-5 border-b border-sidebar-border">
          <div className="flex items-center gap-2.5">
            <div className="h-9 w-9 rounded-lg bg-primary/20 text-primary-light flex items-center justify-center">
              <ShieldCheck className="h-5 w-5 text-primary" />
            </div>
            <div>
              <p className="text-sm font-bold text-white leading-tight">Control Plane</p>
              <p className="text-[11px] text-sidebar-muted">SaaS Backoffice</p>
            </div>
          </div>
          <button
            type="button"
            onClick={() => setMobileMenuOpen(false)}
            className="lg:hidden text-slate-400 hover:text-white"
            aria-label="Cerrar menú"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
          {navItems.map((item) => {
            const Icon = item.icon;
            return (
              <NavLink
                key={item.to}
                to={item.to}
                onClick={() => setMobileMenuOpen(false)}
                className={({ isActive }) =>
                  `flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors ${
                    isActive
                      ? 'bg-primary text-white shadow-sm'
                      : 'text-sidebar-text hover:bg-sidebar-hover hover:text-white'
                  }`
                }
              >
                <Icon className="h-4 w-4 shrink-0" />
                <span>{item.label}</span>
              </NavLink>
            );
          })}
        </nav>

        <div className="p-4 border-t border-sidebar-border text-xs text-sidebar-muted">
          <p className="font-medium text-slate-300 truncate">{admin?.fullName}</p>
          <p className="truncate">{admin?.email}</p>
        </div>
      </aside>

      {mobileMenuOpen && (
        <div
          className="fixed inset-0 z-30 bg-slate-900/50 lg:hidden"
          onClick={() => setMobileMenuOpen(false)}
        />
      )}

      {/* Main Content Area */}
      <div className="flex-1 flex flex-col min-w-0">
        <header className="sticky top-0 z-20 h-16 bg-surface/95 backdrop-blur border-b border-border px-4 lg:px-8 flex items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => setMobileMenuOpen(true)}
              className="lg:hidden rounded-lg p-2 text-slate-600 hover:bg-slate-100"
              aria-label="Abrir menú"
            >
              <Menu className="h-5 w-5" />
            </button>
            <span className="text-xs font-semibold uppercase tracking-wider text-slate-400 hidden sm:inline">
              Administración Supra-Tenant
            </span>
          </div>

          <div className="flex items-center gap-3">
            {admin && <StatusBadge value={admin.role} />}
            <NavLink
              to="/profile"
              className="text-sm font-medium text-slate-700 hover:text-primary px-2.5 py-1.5 rounded-lg hover:bg-slate-100 transition-colors"
            >
              Mi Perfil ({admin?.username})
            </NavLink>
            <button
              type="button"
              onClick={handleLogout}
              className="inline-flex items-center gap-1.5 rounded-lg border border-slate-300 px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-100 transition-colors"
            >
              <LogOut className="h-3.5 w-3.5" />
              Salir
            </button>
          </div>
        </header>

        <main className="flex-1 p-4 lg:p-8 max-w-7xl w-full mx-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
