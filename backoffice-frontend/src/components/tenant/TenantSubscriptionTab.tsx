import { useState, type FormEvent } from 'react';
import { Ban, CalendarClock, CreditCard, RotateCcw } from 'lucide-react';
import { subscriptionsApi } from '../../services/backofficeApi';
import { ApiError } from '../../services/httpClient';
import type {
  BackofficeTenantDetailDto,
  PlatformPaymentMethod,
  SubscriptionPlanDto,
  SuspensionMode,
} from '../../types/api';

interface TenantSubscriptionTabProps {
  detail: BackofficeTenantDetailDto;
  plans: SubscriptionPlanDto[];
  isSuperAdmin: boolean;
  onUpdated: () => Promise<void>;
  onError: (msg: string) => void;
  onSuccess: (msg: string) => void;
}

export function TenantSubscriptionTab({
  detail,
  plans,
  isSuperAdmin,
  onUpdated,
  onError,
  onSuccess,
}: TenantSubscriptionTabProps) {
  const activePlans = plans.filter((p) => p.isActive);
  const [selectedPlanId, setSelectedPlanId] = useState(activePlans[0]?.id ?? plans[0]?.id ?? '');
  const [markAsPaid, setMarkAsPaid] = useState(true);
  const [paymentMethod, setPaymentMethod] = useState<PlatformPaymentMethod>('QR');
  const [referenceCode, setReferenceCode] = useState('');
  const [dueDate, setDueDate] = useState('');
  const [assignNotes, setAssignNotes] = useState('');
  const [assigning, setAssigning] = useState(false);

  // Gracia condicional
  const [graceUntilLocal, setGraceUntilLocal] = useState('');
  const [graceMaxOrders, setGraceMaxOrders] = useState('');
  const [graceNotes, setGraceNotes] = useState('');
  const [grantingGrace, setGrantingGrace] = useState(false);

  // Suspensión & Reactivación (SUPERADMIN)
  const [suspendMode, setSuspendMode] = useState<'POS_AND_SHIFT_ONLY' | 'TOTAL'>('POS_AND_SHIFT_ONLY');
  const [suspendReason, setSuspendReason] = useState('');
  const [reactivateReason, setReactivateReason] = useState('Regularización de servicio');
  const [extendDays, setExtendDays] = useState('');
  const [expirePolicy, setExpirePolicy] = useState<Exclude<SuspensionMode, 'NONE'>>(
    detail.autoSuspendModeOnExpire === 'TOTAL' ? 'TOTAL' : 'POS_AND_SHIFT_ONLY',
  );
  const [working, setWorking] = useState(false);

  const handleAssignPlan = async (e: FormEvent) => {
    e.preventDefault();
    if (!selectedPlanId) {
      onError('Selecciona un plan de suscripción.');
      return;
    }
    setAssigning(true);
    try {
      await subscriptionsApi.assignPlan(detail.tenant.id, {
        planId: selectedPlanId,
        markAsPaid,
        paymentMethod: markAsPaid ? paymentMethod : undefined,
        referenceCode: markAsPaid ? referenceCode.trim() || undefined : undefined,
        dueDate: !markAsPaid && dueDate ? dueDate : null,
        notes: assignNotes.trim() || undefined,
      });
      onSuccess(
        markAsPaid
          ? 'Plan asignado y pago registrado correctamente.'
          : 'Plan asignado por adelantado (cargo PENDING / crédito habilitado).',
      );
      setReferenceCode('');
      setAssignNotes('');
      await onUpdated();
    } catch (err) {
      onError(err instanceof ApiError ? err.detail : 'Error asignando plan al tenant.');
    } finally {
      setAssigning(false);
    }
  };

  const handleGrantGrace = async (e: FormEvent) => {
    e.preventDefault();
    if (!graceUntilLocal && !graceMaxOrders.trim()) {
      onError('Indica al menos una fecha límite o un tope máximo de órdenes de gracia.');
      return;
    }
    setGrantingGrace(true);
    try {
      await subscriptionsApi.grantGrace(detail.tenant.id, {
        graceUntil: graceUntilLocal ? new Date(graceUntilLocal).toISOString() : null,
        graceMaxOrders: graceMaxOrders.trim() ? Number.parseInt(graceMaxOrders, 10) : null,
        notes: graceNotes.trim() || undefined,
      });
      onSuccess('Período de gracia condicional otorgado.');
      await onUpdated();
    } catch (err) {
      onError(err instanceof ApiError ? err.detail : 'Error otorgando gracia condicional.');
    } finally {
      setGrantingGrace(false);
    }
  };

  const handleSuspend = async (e: FormEvent) => {
    e.preventDefault();
    if (!suspendReason.trim()) {
      onError('Debes indicar el motivo de suspensión.');
      return;
    }
    setWorking(true);
    try {
      await subscriptionsApi.suspend(detail.tenant.id, {
        mode: suspendMode,
        reason: suspendReason.trim(),
      });
      onSuccess(`Tenant suspendido bajo modalidad ${suspendMode}.`);
      setSuspendReason('');
      await onUpdated();
    } catch (err) {
      onError(err instanceof ApiError ? err.detail : 'Error aplicando suspensión.');
    } finally {
      setWorking(false);
    }
  };

  const handleReactivate = async () => {
    setWorking(true);
    try {
      await subscriptionsApi.reactivate(detail.tenant.id, {
        reason: reactivateReason.trim() || undefined,
        extendDays: extendDays.trim() ? Number.parseInt(extendDays, 10) : null,
      });
      onSuccess('Tenant reactivado exitosamente.');
      await onUpdated();
    } catch (err) {
      onError(err instanceof ApiError ? err.detail : 'No se pudo reactivar al tenant.');
    } finally {
      setWorking(false);
    }
  };

  const handleSaveExpirePolicy = async () => {
    setWorking(true);
    try {
      await subscriptionsApi.updateExpirePolicy(detail.tenant.id, expirePolicy);
      onSuccess(`Política de auto-suspensión al vencer actualizada a ${expirePolicy}.`);
      await onUpdated();
    } catch (err) {
      onError(err instanceof ApiError ? err.detail : 'No se pudo actualizar la política.');
    } finally {
      setWorking(false);
    }
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
      {/* Asignar Plan (Pagado vs Uso por Adelantado) */}
      <form onSubmit={handleAssignPlan} className="card p-5 space-y-4">
        <div className="flex items-center gap-2">
          <CreditCard className="h-5 w-5 text-primary" />
          <h2 className="text-base font-bold text-slate-800">Asignar / Renovar Plan</h2>
        </div>

        <div>
          <label htmlFor="sub-plan" className="label-text">
            Plan SaaS
          </label>
          <select
            id="sub-plan"
            className="input-field"
            value={selectedPlanId}
            onChange={(e) => setSelectedPlanId(e.target.value)}
          >
            {activePlans.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name} ({p.code}) — {p.currency} {p.price} / {p.durationDays}d
              </option>
            ))}
          </select>
        </div>

        <div className="rounded-lg bg-slate-50 p-3 space-y-2">
          <p className="text-xs font-bold uppercase text-slate-600">Modalidad de Facturación</p>
          <label className="flex items-center gap-2 text-sm cursor-pointer">
            <input
              type="radio"
              checked={markAsPaid}
              onChange={() => setMarkAsPaid(true)}
              className="text-primary"
            />
            Pago recibido al contado (crea cargo PAID + recibo)
          </label>
          <label className="flex items-center gap-2 text-sm cursor-pointer">
            <input
              type="radio"
              checked={!markAsPaid}
              onChange={() => setMarkAsPaid(false)}
              className="text-primary"
            />
            Uso por adelantado / Crédito (deja cargo PENDING por saldar)
          </label>
        </div>

        {markAsPaid ? (
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label htmlFor="sub-pm" className="label-text">
                Método de Pago
              </label>
              <select
                id="sub-pm"
                className="input-field"
                value={paymentMethod}
                onChange={(e) => setPaymentMethod(e.target.value as PlatformPaymentMethod)}
              >
                <option value="QR">QR</option>
                <option value="TRANSFER">TRANSFER</option>
                <option value="CASH">CASH</option>
                <option value="CARD">CARD</option>
                <option value="OTHER">OTHER</option>
              </select>
            </div>
            <div>
              <label htmlFor="sub-ref" className="label-text">
                Código Referencia
              </label>
              <input
                id="sub-ref"
                className="input-field"
                value={referenceCode}
                onChange={(e) => setReferenceCode(e.target.value)}
                placeholder="QR-2026-001"
              />
            </div>
          </div>
        ) : (
          <div>
            <label htmlFor="sub-due" className="label-text">
              Fecha Límite de Pago (Opcional)
            </label>
            <input
              id="sub-due"
              type="date"
              className="input-field"
              value={dueDate}
              onChange={(e) => setDueDate(e.target.value)}
            />
          </div>
        )}

        <div>
          <label htmlFor="sub-notes" className="label-text">
            Notas Administrativas
          </label>
          <input
            id="sub-notes"
            className="input-field"
            value={assignNotes}
            onChange={(e) => setAssignNotes(e.target.value)}
            placeholder="Acuerdo comercial o renovación semanal"
          />
        </div>

        <button type="submit" disabled={assigning} className="btn-primary w-full">
          {assigning
            ? 'Procesando...'
            : markAsPaid
              ? 'Asignar Plan y Registrar Pago'
              : 'Habilitar Plan a Crédito (Pendiente)'}
        </button>
      </form>

      {/* Gracia Condicional */}
      <form onSubmit={handleGrantGrace} className="card p-5 space-y-4">
        <div className="flex items-center gap-2">
          <CalendarClock className="h-5 w-5 text-warning" />
          <h2 className="text-base font-bold text-slate-800">Otorgar Gracia Condicional</h2>
        </div>
        <p className="text-xs text-slate-500">
          Permite operar temporalmente por fecha tope y/o por cantidad máxima de órdenes pagadas; al agotarse se auto-suspende el tenant.
        </p>

        <div>
          <label htmlFor="grace-until" className="label-text">
            Gracia Hasta (Fecha y Hora Local)
          </label>
          <input
            id="grace-until"
            type="datetime-local"
            className="input-field"
            value={graceUntilLocal}
            onChange={(e) => setGraceUntilLocal(e.target.value)}
          />
        </div>

        <div>
          <label htmlFor="grace-orders" className="label-text">
            Tope Máximo de Ventas Durante Gracia (Opcional)
          </label>
          <input
            id="grace-orders"
            type="number"
            min={1}
            className="input-field"
            value={graceMaxOrders}
            onChange={(e) => setGraceMaxOrders(e.target.value)}
            placeholder="Ej. 25"
          />
        </div>

        <div>
          <label htmlFor="grace-notes" className="label-text">
            Nota / Compromiso de Pago
          </label>
          <input
            id="grace-notes"
            className="input-field"
            value={graceNotes}
            onChange={(e) => setGraceNotes(e.target.value)}
            placeholder="Cliente comprometió pago el viernes"
          />
        </div>

        <button type="submit" disabled={grantingGrace} className="btn-warning w-full">
          {grantingGrace ? 'Otorgando...' : 'Activar Período de Gracia'}
        </button>
      </form>

      {/* Suspensión & Política (Solo SUPERADMIN) */}
      {isSuperAdmin && (
        <>
          <form onSubmit={handleSuspend} className="card p-5 space-y-4">
            <div className="flex items-center gap-2">
              <Ban className="h-5 w-5 text-danger" />
              <h2 className="text-base font-bold text-slate-800">Suspensión Manual (SUPERADMIN)</h2>
            </div>

            <div>
              <label htmlFor="susp-mode" className="label-text">
                Nivel de Bloqueo
              </label>
              <select
                id="susp-mode"
                className="input-field"
                value={suspendMode}
                onChange={(e) => setSuspendMode(e.target.value as 'POS_AND_SHIFT_ONLY' | 'TOTAL')}
              >
                <option value="POS_AND_SHIFT_ONLY">
                  POS_AND_SHIFT_ONLY (Permite login ERP, bloquea caja/ventas)
                </option>
                <option value="TOTAL">
                  TOTAL (Bloquea login, refresh y revoca todas las sesiones activas)
                </option>
              </select>
            </div>

            <div>
              <label htmlFor="susp-reason" className="label-text">
                Motivo Obligatorio
              </label>
              <input
                id="susp-reason"
                required
                className="input-field"
                value={suspendReason}
                onChange={(e) => setSuspendReason(e.target.value)}
                placeholder="Mora vencida o solicitud administrativa"
              />
            </div>

            <button type="submit" disabled={working} className="btn-danger w-full">
              Aplicar Suspensión
            </button>
          </form>

          <div className="card p-5 space-y-4">
            <div className="flex items-center gap-2">
              <RotateCcw className="h-5 w-5 text-success" />
              <h2 className="text-base font-bold text-slate-800">Reactivación & Política Automática</h2>
            </div>

            <div className="space-y-3 pb-4 border-b border-slate-100">
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div className="sm:col-span-2">
                  <label htmlFor="react-reason" className="label-text">
                    Motivo Reactivación
                  </label>
                  <input
                    id="react-reason"
                    className="input-field"
                    value={reactivateReason}
                    onChange={(e) => setReactivateReason(e.target.value)}
                  />
                </div>
                <div>
                  <label htmlFor="react-days" className="label-text">
                    Extender Días
                  </label>
                  <input
                    id="react-days"
                    type="number"
                    min={1}
                    className="input-field"
                    value={extendDays}
                    onChange={(e) => setExtendDays(e.target.value)}
                    placeholder="Opcional"
                  />
                </div>
              </div>
              <button type="button" onClick={handleReactivate} disabled={working} className="btn-success w-full">
                Reactivar Suscripción
              </button>
            </div>

            <div className="space-y-2">
              <label htmlFor="exp-policy" className="label-text">
                Bloqueo Automático al Vencer Vigencia / Gracia
              </label>
              <div className="flex gap-2">
                <select
                  id="exp-policy"
                  className="input-field flex-1"
                  value={expirePolicy}
                  onChange={(e) =>
                    setExpirePolicy(e.target.value as Exclude<SuspensionMode, 'NONE'>)
                  }
                >
                  <option value="POS_AND_SHIFT_ONLY">POS_AND_SHIFT_ONLY</option>
                  <option value="TOTAL">TOTAL</option>
                </select>
                <button
                  type="button"
                  onClick={handleSaveExpirePolicy}
                  disabled={working}
                  className="btn-secondary shrink-0"
                >
                  Guardar Política
                </button>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
