import { useEffect, useState } from 'react';
import {
  AlertTriangle,
  ArrowRight,
  Building2,
  CreditCard,
  TrendingUp,
  Wallet,
} from 'lucide-react';
import { Link } from 'react-router-dom';
import { AlertBanner } from '../components/AlertBanner';
import { KpiCard } from '../components/KpiCard';
import { StatusBadge } from '../components/StatusBadge';
import { tenantsApi } from '../services/backofficeApi';
import { ApiError } from '../services/httpClient';
import type { DashboardOverviewDto } from '../types/api';
import { formatCurrency } from '../utils/formatters';

export function DashboardPage() {
  const [overview, setOverview] = useState<DashboardOverviewDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    tenantsApi
      .getDashboardOverview()
      .then((data) => {
        if (!cancelled) setOverview(data);
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err instanceof ApiError ? err.detail : 'No se pudo cargar el dashboard global.');
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  if (loading) {
    return <div className="text-sm text-slate-500">Cargando indicadores de plataforma...</div>;
  }

  if (error || !overview) {
    return <AlertBanner tone="error" message={error ?? 'Sin datos disponibles.'} />;
  }

  const statuses = ['ACTIVE', 'TRIAL', 'GRACE_PERIOD', 'SUSPENDED'];

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Resumen Ejecutivo SaaS</h1>
          <p className="text-sm text-slate-500 mt-1">
            Telemetría multi-tenant consolidada, cobranza y alertas operativas.
          </p>
        </div>
        <Link to="/tenants" className="btn-primary">
          Ver Cartera de Inquilinos
          <ArrowRight className="h-4 w-4" />
        </Link>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
        <KpiCard
          title="Inquilinos Totales"
          value={overview.totalTenants}
          subtitle={`${overview.statusBreakdown['ACTIVE'] ?? 0} activos · ${overview.statusBreakdown['TRIAL'] ?? 0} en trial`}
          icon={Building2}
          tone="primary"
        />
        <KpiCard
          title="Recaudado Este Mes"
          value={formatCurrency(overview.collectedRevenueThisMonth)}
          subtitle="Pagos de suscripción registrados"
          icon={Wallet}
          tone="success"
        />
        <KpiCard
          title="Mora / Saldo Pendiente"
          value={formatCurrency(overview.totalPendingDebt)}
          subtitle="Cargos pendientes o parciales"
          icon={CreditCard}
          tone="warning"
        />
        <KpiCard
          title="Alertas Over-Quota"
          value={overview.overQuotaTenantsCount}
          subtitle="Tenants excediendo límites tras downgrade"
          icon={AlertTriangle}
          tone={overview.overQuotaTenantsCount > 0 ? 'danger' : 'info'}
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Status Distribution */}
        <div className="card p-5 space-y-4">
          <h2 className="text-base font-bold text-slate-800">Distribución por Estado</h2>
          <div className="space-y-3">
            {statuses.map((st) => {
              const count = overview.statusBreakdown[st] ?? 0;
              const pct = overview.totalTenants > 0 ? Math.round((count / overview.totalTenants) * 100) : 0;
              return (
                <div key={st} className="space-y-1">
                  <div className="flex items-center justify-between text-xs">
                    <StatusBadge value={st} />
                    <span className="font-semibold text-slate-700">
                      {count} ({pct}%)
                    </span>
                  </div>
                  <div className="h-2 w-full rounded-full bg-slate-100 overflow-hidden">
                    <div className="h-full bg-primary rounded-full" style={{ width: `${pct}%` }} />
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        {/* Top Tenants 30d */}
        <div className="card p-5 lg:col-span-2 space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <TrendingUp className="h-5 w-5 text-primary" />
              <h2 className="text-base font-bold text-slate-800">Top Inquilinos por Ventas (30 días)</h2>
            </div>
          </div>

          {overview.topTenantsBySales30d.length === 0 ? (
            <p className="text-sm text-slate-500">Aún no existen ventas registradas en los últimos 30 días.</p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-border text-xs uppercase text-slate-500">
                    <th className="py-2 pr-4">Restaurante</th>
                    <th className="py-2 px-4">Slug</th>
                    <th className="py-2 px-4 text-right">Órdenes Pagadas</th>
                    <th className="py-2 pl-4 text-right">Ventas Brutas</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {overview.topTenantsBySales30d.map((item) => (
                    <tr key={item.tenantId} className="hover:bg-slate-50/80">
                      <td className="py-2.5 pr-4 font-semibold text-slate-800">
                        <Link to={`/tenants/${item.tenantId}`} className="text-primary hover:underline">
                          {item.legalName}
                        </Link>
                      </td>
                      <td className="py-2.5 px-4 font-mono text-xs text-slate-500">{item.slug}</td>
                      <td className="py-2.5 px-4 text-right text-slate-700">{item.paidOrdersCount30d}</td>
                      <td className="py-2.5 pl-4 text-right font-semibold text-slate-800">
                        {formatCurrency(item.grossSalesAmount30d)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
