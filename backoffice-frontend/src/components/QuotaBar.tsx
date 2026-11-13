import type { QuotaResource, ResourceQuotaUsage } from '../types/api';

const RESOURCE_LABELS: Record<QuotaResource, string> = {
  BRANCHES: 'Sucursales Activas',
  USERS: 'Usuarios Activos',
  DISHES: 'Platos en Catálogo',
  INVENTORY_ITEMS: 'Insumos de Inventario',
  MONTHLY_ORDERS: 'Órdenes del Mes',
};

interface QuotaBarProps {
  resource: QuotaResource;
  usage: ResourceQuotaUsage;
}

export function QuotaBar({ resource, usage }: QuotaBarProps) {
  const limit = usage.limit;
  const pct = limit && limit > 0 ? Math.min(100, Math.round((usage.used / limit) * 100)) : 0;

  let barColor = 'bg-primary';
  if (usage.overQuota) {
    barColor = 'bg-danger';
  } else if (limit && usage.used >= limit * 0.85) {
    barColor = 'bg-warning';
  }

  return (
    <div className="space-y-1.5">
      <div className="flex items-center justify-between text-xs">
        <span className="font-semibold text-slate-700">{RESOURCE_LABELS[resource]}</span>
        <span className={usage.overQuota ? 'font-bold text-danger' : 'text-slate-600'}>
          {usage.used} / {limit ?? 'Ilimitado'}
          {usage.overQuota ? ' (EXCEDIDO)' : ''}
        </span>
      </div>
      <div className="h-2 w-full overflow-hidden rounded-full bg-slate-200">
        {limit !== null ? (
          <div className={`h-full rounded-full transition-all ${barColor}`} style={{ width: `${pct}%` }} />
        ) : (
          <div className="h-full w-1/4 rounded-full bg-emerald-400/70" />
        )}
      </div>
    </div>
  );
}
