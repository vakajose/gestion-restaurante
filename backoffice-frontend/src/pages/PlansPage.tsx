import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { Check, Edit3, Plus, Power, X } from 'lucide-react';
import { AlertBanner } from '../components/AlertBanner';
import { StatusBadge } from '../components/StatusBadge';
import { useAuth } from '../context/authContext';
import { plansApi } from '../services/backofficeApi';
import { ApiError } from '../services/httpClient';
import type { BillingCycle, SubscriptionPlanDto } from '../types/api';
import { formatCurrency } from '../utils/formatters';

interface PlanFormState {
  code: string;
  name: string;
  description: string;
  billingCycle: BillingCycle;
  durationDays: string;
  price: string;
  currency: string;
  maxBranches: string;
  maxUsers: string;
  maxDishes: string;
  maxInventoryItems: string;
  maxMonthlyOrders: string;
}

const EMPTY_FORM: PlanFormState = {
  code: '',
  name: '',
  description: '',
  billingCycle: 'MONTHLY',
  durationDays: '30',
  price: '0',
  currency: 'BOB',
  maxBranches: '1',
  maxUsers: '5',
  maxDishes: '50',
  maxInventoryItems: '100',
  maxMonthlyOrders: '1000',
};

function toNullableInt(raw: string): number | null {
  const trimmed = raw.trim();
  if (trimmed === '') return null;
  const parsed = Number.parseInt(trimmed, 10);
  return Number.isNaN(parsed) ? null : parsed;
}

export function PlansPage() {
  const { isSuperAdmin } = useAuth();
  const [plans, setPlans] = useState<SubscriptionPlanDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);

  const [modalOpen, setModalOpen] = useState(false);
  const [editingPlan, setEditingPlan] = useState<SubscriptionPlanDto | null>(null);
  const [form, setForm] = useState<PlanFormState>(EMPTY_FORM);
  const [saving, setSaving] = useState(false);

  const loadPlans = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setPlans(await plansApi.listAll());
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : 'Error cargando planes de suscripción.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadPlans();
  }, [loadPlans]);

  const openCreateModal = () => {
    setEditingPlan(null);
    setForm(EMPTY_FORM);
    setModalOpen(true);
  };

  const openEditModal = (plan: SubscriptionPlanDto) => {
    setEditingPlan(plan);
    setForm({
      code: plan.code,
      name: plan.name,
      description: plan.description ?? '',
      billingCycle: plan.billingCycle,
      durationDays: String(plan.durationDays),
      price: String(plan.price),
      currency: plan.currency,
      maxBranches: plan.maxBranches !== null ? String(plan.maxBranches) : '',
      maxUsers: plan.maxUsers !== null ? String(plan.maxUsers) : '',
      maxDishes: plan.maxDishes !== null ? String(plan.maxDishes) : '',
      maxInventoryItems: plan.maxInventoryItems !== null ? String(plan.maxInventoryItems) : '',
      maxMonthlyOrders: plan.maxMonthlyOrders !== null ? String(plan.maxMonthlyOrders) : '',
    });
    setModalOpen(true);
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    setFeedback(null);
    try {
      const commonPayload = {
        name: form.name.trim(),
        description: form.description.trim() || null,
        billingCycle: form.billingCycle,
        durationDays: Number.parseInt(form.durationDays, 10) || 30,
        price: Number.parseFloat(form.price) || 0,
        currency: form.currency.trim().toUpperCase() || 'BOB',
        maxBranches: toNullableInt(form.maxBranches),
        maxUsers: toNullableInt(form.maxUsers),
        maxDishes: toNullableInt(form.maxDishes),
        maxInventoryItems: toNullableInt(form.maxInventoryItems),
        maxMonthlyOrders: toNullableInt(form.maxMonthlyOrders),
      };

      if (editingPlan) {
        await plansApi.update(editingPlan.id, commonPayload);
        setFeedback(`Plan "${commonPayload.name}" actualizado correctamente.`);
      } else {
        await plansApi.create({ ...commonPayload, code: form.code.trim().toUpperCase() });
        setFeedback(`Plan "${commonPayload.name}" creado correctamente.`);
      }

      setModalOpen(false);
      await loadPlans();
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : 'No se pudo guardar el plan.');
    } finally {
      setSaving(false);
    }
  };

  const toggleStatus = async (plan: SubscriptionPlanDto) => {
    setError(null);
    setFeedback(null);
    try {
      await plansApi.updateStatus(plan.id, !plan.isActive);
      setFeedback(`Plan ${plan.code} ${!plan.isActive ? 'activado' : 'desactivado'}.`);
      await loadPlans();
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : 'No se pudo cambiar el estado del plan.');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Catálogo de Planes SaaS</h1>
          <p className="text-sm text-slate-500 mt-1">
            Define ciclos de facturación, tarifas y cuotas base configurables por tenant.
          </p>
        </div>
        {isSuperAdmin && (
          <button type="button" onClick={openCreateModal} className="btn-primary">
            <Plus className="h-4 w-4" />
            Nuevo Plan
          </button>
        )}
      </div>

      {error && <AlertBanner tone="error" message={error} />}
      {feedback && <AlertBanner tone="success" message={feedback} />}

      {loading ? (
        <div className="text-sm text-slate-500">Cargando catálogo de planes...</div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5">
          {plans.map((plan) => (
            <div key={plan.id} className="card p-5 flex flex-col justify-between gap-4">
              <div className="space-y-3">
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <span className="font-mono text-xs font-semibold text-primary">{plan.code}</span>
                    <h2 className="text-lg font-bold text-slate-900">{plan.name}</h2>
                  </div>
                  <StatusBadge
                    value={plan.isActive ? 'ACTIVE' : 'SUSPENDED'}
                    customLabel={plan.isActive ? 'Activo' : 'Inactivo'}
                  />
                </div>

                {plan.description && <p className="text-xs text-slate-500">{plan.description}</p>}

                <div className="flex items-baseline gap-2 pt-1">
                  <span className="text-2xl font-extrabold text-slate-900">
                    {formatCurrency(plan.price, plan.currency)}
                  </span>
                  <span className="text-xs text-slate-500">
                    / {plan.durationDays} días ({plan.billingCycle})
                  </span>
                </div>

                <dl className="grid grid-cols-2 gap-x-4 gap-y-1.5 rounded-lg bg-slate-50 p-3 text-xs">
                  <div>
                    <dt className="text-slate-500">Sucursales</dt>
                    <dd className="font-semibold text-slate-800">{plan.maxBranches ?? 'Ilimitado'}</dd>
                  </div>
                  <div>
                    <dt className="text-slate-500">Usuarios</dt>
                    <dd className="font-semibold text-slate-800">{plan.maxUsers ?? 'Ilimitado'}</dd>
                  </div>
                  <div>
                    <dt className="text-slate-500">Platos</dt>
                    <dd className="font-semibold text-slate-800">{plan.maxDishes ?? 'Ilimitado'}</dd>
                  </div>
                  <div>
                    <dt className="text-slate-500">Insumos</dt>
                    <dd className="font-semibold text-slate-800">{plan.maxInventoryItems ?? 'Ilimitado'}</dd>
                  </div>
                  <div className="col-span-2">
                    <dt className="text-slate-500 inline mr-2">Órdenes/Mes:</dt>
                    <dd className="font-semibold text-slate-800 inline">
                      {plan.maxMonthlyOrders ?? 'Ilimitado'}
                    </dd>
                  </div>
                </dl>
              </div>

              {isSuperAdmin && (
                <div className="flex items-center gap-2 pt-2 border-t border-slate-100">
                  <button
                    type="button"
                    onClick={() => openEditModal(plan)}
                    className="btn-secondary flex-1 text-xs"
                  >
                    <Edit3 className="h-3.5 w-3.5" />
                    Editar
                  </button>
                  <button
                    type="button"
                    onClick={() => toggleStatus(plan)}
                    className="btn-secondary text-xs"
                  >
                    <Power className="h-3.5 w-3.5" />
                    {plan.isActive ? 'Desactivar' : 'Activar'}
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4">
          <div className="card w-full max-w-xl max-h-[90vh] overflow-y-auto p-6 space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-bold text-slate-900">
                {editingPlan ? `Editar Plan ${editingPlan.code}` : 'Crear Plan de Suscripción'}
              </h2>
              <button
                type="button"
                onClick={() => setModalOpen(false)}
                className="text-slate-400 hover:text-slate-700"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label htmlFor="plan-code" className="label-text">
                    Código Único
                  </label>
                  <input
                    id="plan-code"
                    required
                    disabled={Boolean(editingPlan)}
                    className="input-field font-mono uppercase"
                    value={form.code}
                    onChange={(e) => setForm({ ...form, code: e.target.value })}
                    placeholder="PRO_MONTHLY"
                  />
                </div>
                <div>
                  <label htmlFor="plan-name" className="label-text">
                    Nombre Comercial
                  </label>
                  <input
                    id="plan-name"
                    required
                    className="input-field"
                    value={form.name}
                    onChange={(e) => setForm({ ...form, name: e.target.value })}
                    placeholder="Plan Profesional"
                  />
                </div>
              </div>

              <div>
                <label htmlFor="plan-desc" className="label-text">
                  Descripción
                </label>
                <input
                  id="plan-desc"
                  className="input-field"
                  value={form.description}
                  onChange={(e) => setForm({ ...form, description: e.target.value })}
                />
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                <div>
                  <label htmlFor="plan-cycle" className="label-text">
                    Ciclo
                  </label>
                  <select
                    id="plan-cycle"
                    className="input-field"
                    value={form.billingCycle}
                    onChange={(e) => setForm({ ...form, billingCycle: e.target.value as BillingCycle })}
                  >
                    <option value="WEEKLY">WEEKLY</option>
                    <option value="MONTHLY">MONTHLY</option>
                    <option value="ANNUAL">ANNUAL</option>
                    <option value="CUSTOM">CUSTOM</option>
                  </select>
                </div>
                <div>
                  <label htmlFor="plan-days" className="label-text">
                    Días
                  </label>
                  <input
                    id="plan-days"
                    type="number"
                    min={1}
                    required
                    className="input-field"
                    value={form.durationDays}
                    onChange={(e) => setForm({ ...form, durationDays: e.target.value })}
                  />
                </div>
                <div>
                  <label htmlFor="plan-price" className="label-text">
                    Precio
                  </label>
                  <input
                    id="plan-price"
                    type="number"
                    min={0}
                    step="0.01"
                    required
                    className="input-field"
                    value={form.price}
                    onChange={(e) => setForm({ ...form, price: e.target.value })}
                  />
                </div>
                <div>
                  <label htmlFor="plan-currency" className="label-text">
                    Moneda
                  </label>
                  <input
                    id="plan-currency"
                    maxLength={10}
                    required
                    className="input-field uppercase"
                    value={form.currency}
                    onChange={(e) => setForm({ ...form, currency: e.target.value })}
                  />
                </div>
              </div>

              <div>
                <p className="text-xs font-bold text-slate-600 mb-2">
                  Límites de Cuotas Base (dejar vacío = ilimitado)
                </p>
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                  <div>
                    <label htmlFor="max-branches" className="label-text">
                      Sucursales
                    </label>
                    <input
                      id="max-branches"
                      type="number"
                      min={1}
                      className="input-field"
                      value={form.maxBranches}
                      onChange={(e) => setForm({ ...form, maxBranches: e.target.value })}
                    />
                  </div>
                  <div>
                    <label htmlFor="max-users" className="label-text">
                      Usuarios
                    </label>
                    <input
                      id="max-users"
                      type="number"
                      min={1}
                      className="input-field"
                      value={form.maxUsers}
                      onChange={(e) => setForm({ ...form, maxUsers: e.target.value })}
                    />
                  </div>
                  <div>
                    <label htmlFor="max-dishes" className="label-text">
                      Platos
                    </label>
                    <input
                      id="max-dishes"
                      type="number"
                      min={1}
                      className="input-field"
                      value={form.maxDishes}
                      onChange={(e) => setForm({ ...form, maxDishes: e.target.value })}
                    />
                  </div>
                  <div>
                    <label htmlFor="max-inv" className="label-text">
                      Insumos
                    </label>
                    <input
                      id="max-inv"
                      type="number"
                      min={1}
                      className="input-field"
                      value={form.maxInventoryItems}
                      onChange={(e) => setForm({ ...form, maxInventoryItems: e.target.value })}
                    />
                  </div>
                  <div className="col-span-2 sm:col-span-2">
                    <label htmlFor="max-orders" className="label-text">
                      Órdenes Mensuales
                    </label>
                    <input
                      id="max-orders"
                      type="number"
                      min={1}
                      className="input-field"
                      value={form.maxMonthlyOrders}
                      onChange={(e) => setForm({ ...form, maxMonthlyOrders: e.target.value })}
                    />
                  </div>
                </div>
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
                <button type="button" onClick={() => setModalOpen(false)} className="btn-secondary">
                  Cancelar
                </button>
                <button type="submit" disabled={saving} className="btn-primary">
                  <Check className="h-4 w-4" />
                  {saving ? 'Guardando...' : 'Guardar Plan'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
