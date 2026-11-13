import type { DailyTelemetryPointDto } from '../types/api';
import { formatCalendarDate, formatCurrency } from '../utils/formatters';

interface DailySalesChartProps {
  points: DailyTelemetryPointDto[];
  currency?: string;
}

export function DailySalesChart({ points, currency = 'BOB' }: DailySalesChartProps) {
  if (points.length === 0) {
    return (
      <div className="rounded-lg border border-dashed border-slate-300 p-6 text-center text-sm text-slate-500">
        No existen ventas registradas en el rango seleccionado.
      </div>
    );
  }

  const maxSales = Math.max(...points.map((p) => p.grossSalesAmount), 1);

  return (
    <div className="space-y-3">
      <div className="h-44 flex items-end gap-2 rounded-lg border border-slate-200 bg-slate-50/70 p-4 overflow-x-auto">
        {points.map((pt) => {
          const heightPct = Math.max(6, Math.round((pt.grossSalesAmount / maxSales) * 100));
          return (
            <div
              key={pt.date}
              className="group relative flex flex-1 min-w-[28px] max-w-[56px] flex-col items-center h-full justify-end"
              title={`${formatCalendarDate(pt.date)}: ${formatCurrency(pt.grossSalesAmount, currency)} (${pt.paidOrdersCount} órdenes)`}
            >
              <div className="w-full flex-1 flex items-end">
                <div
                  className="w-full rounded-t-md bg-primary/85 transition-all group-hover:bg-primary-hover"
                  style={{ height: `${heightPct}%` }}
                />
              </div>
              <span className="mt-1.5 text-[10px] font-medium text-slate-500 whitespace-nowrap">
                {formatCalendarDate(pt.date).slice(0, 5)}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
