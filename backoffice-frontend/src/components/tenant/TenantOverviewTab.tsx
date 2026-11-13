import { useState, type FormEvent } from 'react';
import { Boxes, Check, Store } from 'lucide-react';
import { QuotaBar } from '../QuotaBar';
import { StatusBadge } from '../StatusBadge';
import { subscriptionsApi } from '../../services/backofficeApi';
import { ApiError } from '../../services/httpClient';
import type { BackofficeTenantDetailDto, QuotaResource } from '../../types/api';
import { formatCurrency } from '../../utils/formatters';

const QUOTA_ORDER: QuotaResource[] = [
  'BRANCHES',
  'USERS',
  'DISHES',
  'INVENTORY_ITEMS',
  'MONTHLY_ORDERS',
];

function parseOptionalInt(raw: string): number | null {
  const t = raw.trim();
  if (!t) return null;
  const n = Number.parseInt(t, 10);
  return Number.isNaN(n) ? null : n;
}

interface TenantOverviewTabProps {
  detail: BackofficeTenantDetailDto;
  isSuperAdmin: boolean;
  onUpdated: () => Promise<void>;
  onError: (msg: string) => void;
  onSuccess: (msg: string) => void;
}

export function TenantOverviewTab({
  detail,
  isSuperAdmin,
  onUpdated,
  onError,
  onSuccess,
}: TenantOverviewTabProps) {
  const quotas = detail.entitlements.quotas;
  const [maxBranches, setMaxBranches] = useState(
    quotas.BRANCHES?.limit !== null && quotas.BRANCHES?.limit !== undefined ? String(quotas.BRANCHES.limit) : '',
  );
  const [maxUsers, setMaxUsers] = useState(
    quotas.USERS?.limit !== null && quotas.USERS?.limit !== undefined ? String(quotas.USERS.limit) : '',
  );
  const [maxDishes, setMaxDishes] = useState(
    quotas.DISHES?.limit !== null && quotas.DISHES?.limit !== undefined ? String(quotas.DISHES.limit) : '',
  );
  const [maxInventory, setMaxInventory] = useState(
    quotas.INVENTORY_ITEMS?.limit !== null && quotas.INVENTORY_ITEMS?.limit !== undefined
      ? String(quotas.INVENTORY_ITEMS.limit)
      : '',
  );
  const [maxOrders, setMaxOrders] = useState(
    quotas.MONTHLY_ORDERS?.limit !== null && quotas.MONTHLY_ORDERS?.limit !== undefined
      ? String(quotas.MONTHLY_ORDERS.limit)
      : '',
  );
  const [savingQuotas, setSavingQuotas] = useState(false);

  const handleSaveQuotas = async (e: FormEvent) => {
    e.preventDefault();
    setSavingQuotas(true);
    try {
      await subscriptionsApi.updateQuotas(detail.tenant.id, {
        maxBranches: parseOptionalInt(maxBranches),
        maxUsers: parseOptionalInt(maxUsers),
        maxDishes: parseOptionalInt(maxDishes),
        maxInventoryItems: parseOptionalInt(maxInventory),
        maxMonthlyOrders: parseOptionalInt(maxOrders),
      });
      onSuccess('Límites de cuota personalizados guardados e invalidados en caché ERP.');
      await onUpdated();
    } catch (err) {
      onError(err instanceof ApiError ? err.detail : 'No se pudieron actualizar las cuotas.');
    } finally {
      setSavingQuotas(false);
    }
  };

  const u = detail.structuralUsage;

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Uso vs Cuotas */}
        <div className="card p-5 space-y-4">
          <h2 className="text-base font-bold text-slate-800">Consumo de Cuotas Vigentes</h2>
          <div className="space-y-3.5">
            {QUOTA_ORDER.map((res) => {
              const entry = quotas[res];
              if (!entry) return null;
              return <QuotaBar key={res} resource={res} usage={entry} />;
            })}
          </div>
        </div>

        {/* Inventario y Catálogo */}
        <div className="card p-5 space-y-4">
          <div className="flex items-center gap-2">
            <Boxes className="h-5 w-5 text-primary" />
            <h2 className="text-base font-bold text-slate-800">Métricas Estructurales & Inventario</h2>
          </div>
          <dl className="grid grid-cols-2 gap-4 text-sm">
            <div className="rounded-lg bg-slate-50 p-3">
              <dt className="text-xs text-slate-500">Usuarios Activos / Totales</dt>
              <dd className="mt-1 text-lg font-bold text-slate-800">
                {u.activeUsers} / {u.totalUsers}
              </dd>
            </div>
            <div className="rounded-lg bg-slate-50 p-3">
              <dt className="text-xs text-slate-500">Platos Activos / Totales</dt>
              <dd className="mt-1 text-lg font-bold text-slate-800">
                {u.activeDishes} / {u.totalDishes}
              </dd>
            </div>
            <div className="rounded-lg bg-slate-50 p-3">
              <dt className="text-xs text-slate-500">Insumos con Stock / Definidos</dt>
              <dd className="mt-1 text-lg font-bold text-slate-800">
                {u.distinctStockedIngredients} / {u.totalIngredients}
              </dd>
            </div>
            <div className="rounded-lg bg-slate-50 p-3">
              <dt className="text-xs text-slate-500">Valuación Estimada Stock</dt>
              <dd className="mt-1 text-lg font-bold text-slate-800">
                {formatCurrency(u.totalInventoryValuation, detail.entitlements.currency)}
              </dd>
            </div>
          </dl>
        </div>
      </div>

      {/* Override de Cuotas */}
      {isSuperAdmin && (
        <form onSubmit={handleSaveQuotas} className="card p-5 space-y-4">
          <div>
            <h2 className="text-base font-bold text-slate-800">
              Ajuste Manual de Límites / Downgrade No Destructivo (SUPERADMIN)
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">
              Dejar vacío utiliza ilimitado. Reducir un límite debajo del uso actual bloquea nuevas operaciones en POS sin borrar información existente.
            </p>
          </div>
          <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
            <div>
              <label htmlFor="q-branches" className="label-text">
                Sucursales
              </label>
              <input
                id="q-branches"
                type="number"
                min={1}
                className="input-field"
                value={maxBranches}
                onChange={(e) => setMaxBranches(e.target.value)}
              />
            </div>
            <div>
              <label htmlFor="q-users" className="label-text">
                Usuarios
              </label>
              <input
                id="q-users"
                type="number"
                min={1}
                className="input-field"
                value={maxUsers}
                onChange={(e) => setMaxUsers(e.target.value)}
              />
            </div>
            <div>
              <label htmlFor="q-dishes" className="label-text">
                Platos
              </label>
              <input
                id="q-dishes"
                type="number"
                min={1}
                className="input-field"
                value={maxDishes}
                onChange={(e) => setMaxDishes(e.target.value)}
              />
            </div>
            <div>
              <label htmlFor="q-inv" className="label-text">
                Insumos
              </label>
              <input
                id="q-inv"
                type="number"
                min={1}
                className="input-field"
                value={maxInventory}
                onChange={(e) => setMaxInventory(e.target.value)}
              />
            </div>
            <div>
              <label htmlFor="q-orders" className="label-text">
                Órdenes/Mes
              </label>
              <input
                id="q-orders"
                type="number"
                min={1}
                className="input-field"
                value={maxOrders}
                onChange={(e) => setMaxOrders(e.target.value)}
              />
            </div>
          </div>
          <button type="submit" disabled={savingQuotas} className="btn-primary">
            <Check className="h-4 w-4" />
            {savingQuotas ? 'Aplicando...' : 'Aplicar Límites Personalizados'}
          </button>
        </form>
      )}

      {/* Sucursales */}
      <div className="card p-5 space-y-3">
        <div className="flex items-center gap-2">
          <Store className="h-5 w-5 text-primary" />
          <h2 className="text-base font-bold text-slate-800">Sucursales Registradas ({detail.branches.length})</h2>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-border text-xs uppercase text-slate-500">
                <th className="py-2 pr-4">Nombre</th>
                <th className="py-2 px-4">Dirección</th>
                <th className="py-2 px-4">Zona Horaria</th>
                <th className="py-2 pl-4">Estado</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {detail.branches.map((b) => (
                <tr key={b.id}>
                  <td className="py-2.5 pr-4 font-semibold text-slate-800">{b.name}</td>
                  <td className="py-2.5 px-4 text-slate-600">{b.address ?? '—'}</td>
                  <td className="py-2.5 px-4 font-mono text-xs text-slate-600">{b.timezone}</td>
                  <td className="py-2.5 pl-4">
                    <StatusBadge
                      value={b.isActive ? 'ACTIVE' : 'SUSPENDED'}
                      customLabel={b.isActive ? 'Activa' : 'Inactiva'}
                    />
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
