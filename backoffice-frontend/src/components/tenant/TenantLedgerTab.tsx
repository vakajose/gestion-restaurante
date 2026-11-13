import { useState, type FormEvent } from 'react';
import { Banknote, CheckCircle2, FileX } from 'lucide-react';
import { StatusBadge } from '../StatusBadge';
import { subscriptionsApi } from '../../services/backofficeApi';
import { ApiError } from '../../services/httpClient';
import type {
  PlatformPaymentMethod,
  SubscriptionChargeDto,
  SubscriptionPaymentDto,
  TenantChargesSummaryDto,
} from '../../types/api';
import { formatCalendarDate, formatCurrency, formatInstantDateTime } from '../../utils/formatters';

interface TenantLedgerTabProps {
  tenantId: string;
  chargesSummary: TenantChargesSummaryDto | null;
  payments: SubscriptionPaymentDto[];
  isSuperAdmin: boolean;
  onUpdated: () => Promise<void>;
  onError: (msg: string) => void;
  onSuccess: (msg: string) => void;
}

export function TenantLedgerTab({
  tenantId,
  chargesSummary,
  payments,
  isSuperAdmin,
  onUpdated,
  onError,
  onSuccess,
}: TenantLedgerTabProps) {
  const [amount, setAmount] = useState('');
  const [paymentMethod, setPaymentMethod] = useState<PlatformPaymentMethod>('QR');
  const [referenceCode, setReferenceCode] = useState('');
  const [targetChargeId, setTargetChargeId] = useState('');
  const [autoReactivate, setAutoReactivate] = useState(true);
  const [notes, setNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const openCharges =
    chargesSummary?.charges.filter((c) => c.chargeStatus === 'PENDING' || c.chargeStatus === 'PARTIAL') ??
    [];

  const handleRecordPayment = async (e: FormEvent) => {
    e.preventDefault();
    const numeric = Number.parseFloat(amount);
    if (Number.isNaN(numeric) || numeric <= 0) {
      onError('Ingresa un monto de pago mayor a cero.');
      return;
    }

    setSubmitting(true);
    try {
      const res = await subscriptionsApi.recordPayment(tenantId, {
        amount: numeric,
        currency: chargesSummary?.currency ?? 'BOB',
        paymentMethod,
        referenceCode: referenceCode.trim() || undefined,
        chargeId: targetChargeId || null,
        notes: notes.trim() || undefined,
        autoReactivateIfSettled: autoReactivate,
      });
      setAmount('');
      setReferenceCode('');
      setNotes('');
      onSuccess(
        `Pago registrado (${res.allocatedChargeIds.length} cargo(s) conciliados). Saldo pendiente: ${formatCurrency(res.remainingPendingBalance, chargesSummary?.currency ?? 'BOB')}.`,
      );
      await onUpdated();
    } catch (err) {
      onError(err instanceof ApiError ? err.detail : 'No se pudo registrar el pago.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleWaive = async (charge: SubscriptionChargeDto) => {
    setSubmitting(true);
    try {
      await subscriptionsApi.waiveCharge(charge.id, 'Condonación administrativa desde Backoffice');
      onSuccess(`Cargo "${charge.planNameSnapshot}" condonado.`);
      await onUpdated();
    } catch (err) {
      onError(err instanceof ApiError ? err.detail : 'No se pudo condonar el cargo.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Formulario de Cobro / Abono FIFO */}
      <form onSubmit={handleRecordPayment} className="card p-5 space-y-4">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <Banknote className="h-5 w-5 text-success" />
            <h2 className="text-base font-bold text-slate-800">Registrar Pago / Abono FIFO</h2>
          </div>
          <span className="text-sm font-bold text-slate-700">
            Saldo Pendiente Total:{' '}
            <span
              className={
                (chargesSummary?.totalPendingBalance ?? 0) > 0 ? 'text-warning' : 'text-success'
              }
            >
              {formatCurrency(chargesSummary?.totalPendingBalance ?? 0, chargesSummary?.currency)}
            </span>
          </span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
          <div>
            <label htmlFor="pay-amount" className="label-text">
              Monto ({chargesSummary?.currency ?? 'BOB'})
            </label>
            <input
              id="pay-amount"
              type="number"
              min="0.01"
              step="0.01"
              required
              className="input-field"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
            />
          </div>
          <div>
            <label htmlFor="pay-method" className="label-text">
              Método
            </label>
            <select
              id="pay-method"
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
            <label htmlFor="pay-ref" className="label-text">
              Comprobante / Referencia
            </label>
            <input
              id="pay-ref"
              className="input-field"
              value={referenceCode}
              onChange={(e) => setReferenceCode(e.target.value)}
              placeholder="TRX-9981"
            />
          </div>
          <div>
            <label htmlFor="pay-charge" className="label-text">
              Aplicación de Abono
            </label>
            <select
              id="pay-charge"
              className="input-field"
              value={targetChargeId}
              onChange={(e) => setTargetChargeId(e.target.value)}
            >
              <option value="">Automático FIFO (cargos más antiguos)</option>
              {openCharges.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.planNameSnapshot} (Resta {formatCurrency(c.remainingAmount, c.currency)})
                </option>
              ))}
            </select>
          </div>
        </div>

        <div className="flex flex-wrap items-center justify-between gap-3">
          <input
            aria-label="Notas del pago"
            className="input-field flex-1 min-w-[220px]"
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            placeholder="Notas del cobro..."
          />
          <label className="inline-flex items-center gap-2 text-xs font-semibold text-slate-700 cursor-pointer">
            <input
              type="checkbox"
              checked={autoReactivate}
              onChange={(e) => setAutoReactivate(e.target.checked)}
              className="rounded border-slate-300 text-primary"
            />
            Reactivar si el saldo llega a 0
          </label>
          <button type="submit" disabled={submitting} className="btn-success">
            <CheckCircle2 className="h-4 w-4" />
            {submitting ? 'Registrando...' : 'Registrar Abono'}
          </button>
        </div>
      </form>

      {/* Tabla de Cargos */}
      <div className="card p-5 space-y-3">
        <h2 className="text-base font-bold text-slate-800">Libro de Cargos de Suscripción</h2>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="border-b border-border uppercase text-slate-500">
              <tr>
                <th className="py-2 pr-4">Concepto / Plan</th>
                <th className="py-2 px-4">Vigencia</th>
                <th className="py-2 px-4">Venc. Pago</th>
                <th className="py-2 px-4 text-right">Total Cargo</th>
                <th className="py-2 px-4 text-right">Pagado</th>
                <th className="py-2 px-4 text-right">Saldo</th>
                <th className="py-2 px-4">Estado</th>
                {isSuperAdmin && <th className="py-2 pl-4 text-right">Acción</th>}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {(chargesSummary?.charges ?? []).map((c) => (
                <tr key={c.id}>
                  <td className="py-2.5 pr-4 font-semibold text-slate-800">
                    {c.planNameSnapshot}
                    {c.notes && <p className="font-normal text-slate-400">{c.notes}</p>}
                  </td>
                  <td className="py-2.5 px-4 text-slate-500">
                    {formatInstantDateTime(c.periodStart)} → {formatInstantDateTime(c.periodEnd)}
                  </td>
                  <td className="py-2.5 px-4 text-slate-600">{formatCalendarDate(c.dueDate)}</td>
                  <td className="py-2.5 px-4 text-right font-semibold">
                    {formatCurrency(c.amountDue, c.currency)}
                  </td>
                  <td className="py-2.5 px-4 text-right text-success">
                    {formatCurrency(c.paidAmount, c.currency)}
                  </td>
                  <td className="py-2.5 px-4 text-right font-bold text-slate-800">
                    {formatCurrency(c.remainingAmount, c.currency)}
                  </td>
                  <td className="py-2.5 px-4">
                    <StatusBadge value={c.chargeStatus} />
                  </td>
                  {isSuperAdmin && (
                    <td className="py-2.5 pl-4 text-right">
                      {(c.chargeStatus === 'PENDING' || c.chargeStatus === 'PARTIAL') && (
                        <button
                          type="button"
                          disabled={submitting}
                          onClick={() => handleWaive(c)}
                          className="btn-secondary text-xs py-1 px-2"
                        >
                          <FileX className="h-3.5 w-3.5" />
                          Condonar
                        </button>
                      )}
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Histórico de Pagos */}
      <div className="card p-5 space-y-3">
        <h2 className="text-base font-bold text-slate-800">Histórico de Pagos Recibidos ({payments.length})</h2>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="border-b border-border uppercase text-slate-500">
              <tr>
                <th className="py-2 pr-4">Fecha Pago</th>
                <th className="py-2 px-4">Método</th>
                <th className="py-2 px-4">Referencia</th>
                <th className="py-2 px-4">Notas</th>
                <th className="py-2 pl-4 text-right">Monto</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {payments.map((p) => (
                <tr key={p.id}>
                  <td className="py-2.5 pr-4 text-slate-600">{formatInstantDateTime(p.paidAt)}</td>
                  <td className="py-2.5 px-4 font-semibold text-slate-800">{p.paymentMethod}</td>
                  <td className="py-2.5 px-4 font-mono text-slate-500">{p.referenceCode ?? '—'}</td>
                  <td className="py-2.5 px-4 text-slate-500">{p.notes ?? '—'}</td>
                  <td className="py-2.5 pl-4 text-right font-bold text-success">
                    {formatCurrency(p.amount, p.currency)}
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
