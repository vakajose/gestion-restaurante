import { Receipt, TrendingUp, XCircle } from 'lucide-react';
import { DailySalesChart } from '../DailySalesChart';
import { KpiCard } from '../KpiCard';
import { StatusBadge } from '../StatusBadge';
import type { TenantTelemetryReportDto } from '../../types/api';
import { formatCalendarDate, formatCurrency } from '../../utils/formatters';

interface TenantTelemetryTabProps {
  telemetry: TenantTelemetryReportDto | null;
  currency: string;
  from: string;
  to: string;
  onChangeFrom: (v: string) => void;
  onChangeTo: (v: string) => void;
  onRefresh: () => void;
}

export function TenantTelemetryTab({
  telemetry,
  currency,
  from,
  to,
  onChangeFrom,
  onChangeTo,
  onRefresh,
}: TenantTelemetryTabProps) {
  if (!telemetry) {
    return <div className="text-sm text-slate-500">Cargando reporte de telemetría...</div>;
  }

  return (
    <div className="space-y-6">
      <div className="card p-4 flex flex-wrap items-end gap-3">
        <div>
          <label htmlFor="tel-from" className="label-text">
            Desde (Calendario Local)
          </label>
          <input
            id="tel-from"
            type="date"
            className="input-field"
            value={from}
            onChange={(e) => onChangeFrom(e.target.value)}
          />
        </div>
        <div>
          <label htmlFor="tel-to" className="label-text">
            Hasta
          </label>
          <input
            id="tel-to"
            type="date"
            className="input-field"
            value={to}
            onChange={(e) => onChangeTo(e.target.value)}
          />
        </div>
        <button type="button" onClick={onRefresh} className="btn-primary">
          Consultar Rango
        </button>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <KpiCard
          title="Ventas Brutas"
          value={formatCurrency(telemetry.summary.grossSalesAmount, currency)}
          icon={TrendingUp}
          tone="success"
        />
        <KpiCard
          title="Órdenes Pagadas"
          value={telemetry.summary.paidOrdersCount}
          icon={Receipt}
          tone="primary"
        />
        <KpiCard
          title="Ticket Promedio"
          value={formatCurrency(telemetry.summary.averageTicket, currency)}
          icon={TrendingUp}
          tone="info"
        />
        <KpiCard
          title="Órdenes Anuladas"
          value={telemetry.summary.cancelledOrdersCount}
          icon={XCircle}
          tone={telemetry.summary.cancelledOrdersCount > 0 ? 'warning' : 'primary'}
        />
      </div>

      <div className="card p-5 space-y-4">
        <h2 className="text-base font-bold text-slate-800">
          Serie Diaria ({formatCalendarDate(telemetry.from)} – {formatCalendarDate(telemetry.to)})
        </h2>
        <DailySalesChart points={telemetry.dailySeries} currency={currency} />
      </div>

      <div className="card p-5 space-y-3">
        <h2 className="text-base font-bold text-slate-800">Desglose por Sucursal</h2>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-border text-xs uppercase text-slate-500">
                <th className="py-2 pr-4">Sucursal</th>
                <th className="py-2 px-4">Zona Horaria</th>
                <th className="py-2 px-4">Estado</th>
                <th className="py-2 px-4 text-right">Órdenes Pagadas</th>
                <th className="py-2 px-4 text-right">Anuladas</th>
                <th className="py-2 pl-4 text-right">Ventas Brutas</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {telemetry.branchBreakdown.map((b) => (
                <tr key={b.branchId}>
                  <td className="py-2.5 pr-4 font-semibold text-slate-800">{b.branchName}</td>
                  <td className="py-2.5 px-4 font-mono text-xs text-slate-500">{b.timezone}</td>
                  <td className="py-2.5 px-4">
                    <StatusBadge
                      value={b.isActive ? 'ACTIVE' : 'SUSPENDED'}
                      customLabel={b.isActive ? 'Activa' : 'Inactiva'}
                    />
                  </td>
                  <td className="py-2.5 px-4 text-right text-slate-700">{b.paidOrdersCount}</td>
                  <td className="py-2.5 px-4 text-right text-slate-500">{b.cancelledOrdersCount}</td>
                  <td className="py-2.5 pl-4 text-right font-bold text-slate-900">
                    {formatCurrency(b.grossSalesAmount, currency)}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
