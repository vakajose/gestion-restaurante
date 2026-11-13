import { useCallback, useEffect, useState } from 'react';
import { ArrowLeft } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import { AlertBanner } from '../components/AlertBanner';
import { StatusBadge } from '../components/StatusBadge';
import { TenantLedgerTab } from '../components/tenant/TenantLedgerTab';
import { TenantOverviewTab } from '../components/tenant/TenantOverviewTab';
import { TenantSubscriptionTab } from '../components/tenant/TenantSubscriptionTab';
import { TenantTelemetryTab } from '../components/tenant/TenantTelemetryTab';
import { useAuth } from '../context/authContext';
import { plansApi, subscriptionsApi, tenantsApi } from '../services/backofficeApi';
import { ApiError } from '../services/httpClient';
import type {
  BackofficeTenantDetailDto,
  SubscriptionPaymentDto,
  SubscriptionPlanDto,
  TenantChargesSummaryDto,
  TenantTelemetryReportDto,
} from '../types/api';
import { formatCurrency, formatInstantDateTime, isoDaysAgo, isoToday } from '../utils/formatters';

type TenantTab = 'overview' | 'telemetry' | 'subscription' | 'ledger';

export function TenantDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { isSuperAdmin } = useAuth();

  const [activeTab, setActiveTab] = useState<TenantTab>('overview');
  const [detail, setDetail] = useState<BackofficeTenantDetailDto | null>(null);
  const [plans, setPlans] = useState<SubscriptionPlanDto[]>([]);
  const [chargesSummary, setChargesSummary] = useState<TenantChargesSummaryDto | null>(null);
  const [payments, setPayments] = useState<SubscriptionPaymentDto[]>([]);
  const [telemetry, setTelemetry] = useState<TenantTelemetryReportDto | null>(null);

  const [telFrom, setTelFrom] = useState(() => isoDaysAgo(14));
  const [telTo, setTelTo] = useState(() => isoToday());

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);

  const loadAll = useCallback(async () => {
    if (!id) return;
    setLoading(true);
    setError(null);
    try {
      const [d, p, c, pay, tel] = await Promise.all([
        tenantsApi.getTenantDetail(id),
        plansApi.listAll(),
        subscriptionsApi.listCharges(id),
        subscriptionsApi.listPayments(id),
        tenantsApi.getTenantTelemetry(id, telFrom, telTo),
      ]);
      setDetail(d);
      setPlans(p);
      setChargesSummary(c);
      setPayments(pay);
      setTelemetry(tel);
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : 'Error cargando ficha 360° del tenant.');
    } finally {
      setLoading(false);
    }
  }, [id, telFrom, telTo]);

  useEffect(() => {
    void loadAll();
  }, [loadAll]);

  const reloadTelemetry = async () => {
    if (!id) return;
    setError(null);
    try {
      setTelemetry(await tenantsApi.getTenantTelemetry(id, telFrom, telTo));
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : 'Error actualizando telemetría.');
    }
  };

  if (loading && !detail) {
    return <div className="text-sm text-slate-500">Cargando ficha 360° del tenant...</div>;
  }

  if (!detail) {
    return <AlertBanner tone="error" message={error ?? 'Inquilino no encontrado.'} />;
  }

  const ent = detail.entitlements;

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-3">
        <Link to="/tenants" className="btn-secondary text-xs">
          <ArrowLeft className="h-4 w-4" />
          Cartera
        </Link>
        <span className="font-mono text-xs text-slate-400">{detail.tenant.id}</span>
      </div>

      {/* Header 360 */}
      <div className="card p-6 flex flex-wrap items-start justify-between gap-4">
        <div className="space-y-2">
          <div className="flex flex-wrap items-center gap-2.5">
            <h1 className="text-2xl font-bold text-slate-900">{detail.tenant.legalName}</h1>
            <StatusBadge value={ent.status} />
            {ent.effectiveSuspensionMode !== 'NONE' && (
              <StatusBadge value={ent.effectiveSuspensionMode} />
            )}
          </div>
          <p className="text-xs text-slate-500">
            Slug: <span className="font-mono">{detail.tenant.slug}</span> · NIT:{' '}
            {detail.tenant.nitOrTaxId ?? '—'} · Plan: <span className="font-semibold">{ent.planName ?? 'Sin plan'}</span>
          </p>
          {ent.suspensionReason && (
            <p className="text-xs font-semibold text-danger">Motivo bloqueo: {ent.suspensionReason}</p>
          )}
          {ent.graceUntil && (
            <p className="text-xs text-warning">
              Gracia hasta {formatInstantDateTime(ent.graceUntil)}
              {ent.graceMaxOrders !== null
                ? ` · Ventas gracia: ${ent.graceOrdersUsed}/${ent.graceMaxOrders}`
                : ''}
            </p>
          )}
        </div>

        <div className="flex flex-wrap gap-4 text-right text-sm">
          <div>
            <p className="text-xs text-slate-400 uppercase font-semibold">Saldo Pendiente</p>
            <p className={`text-lg font-bold ${ent.pendingBalance > 0 ? 'text-warning' : 'text-slate-800'}`}>
              {formatCurrency(ent.pendingBalance, ent.currency)}
            </p>
          </div>
          <div>
            <p className="text-xs text-slate-400 uppercase font-semibold">Ventas 30d</p>
            <p className="text-lg font-bold text-slate-900">
              {formatCurrency(detail.salesLast30Days, ent.currency)}
            </p>
          </div>
        </div>
      </div>

      {error && <AlertBanner tone="error" message={error} />}
      {feedback && <AlertBanner tone="success" message={feedback} />}

      {/* Navigation Tabs */}
      <div className="flex flex-wrap gap-2 border-b border-border pb-3">
        {[
          { id: 'overview', label: 'Resumen, Cuotas & Sucursales' },
          { id: 'telemetry', label: 'Telemetría & Ventas' },
          { id: 'subscription', label: 'Planes, Gracia & Bloqueos' },
          { id: 'ledger', label: 'Cuenta Corriente & Pagos' },
        ].map((tab) => (
          <button
            key={tab.id}
            type="button"
            onClick={() => setActiveTab(tab.id as TenantTab)}
            className={`rounded-lg px-4 py-2 text-sm font-medium transition-colors ${
              activeTab === tab.id
                ? 'bg-primary text-white shadow-sm'
                : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {activeTab === 'overview' && (
        <TenantOverviewTab
          detail={detail}
          isSuperAdmin={isSuperAdmin}
          onUpdated={loadAll}
          onError={(m) => {
            setFeedback(null);
            setError(m);
          }}
          onSuccess={(m) => {
            setError(null);
            setFeedback(m);
          }}
        />
      )}

      {activeTab === 'telemetry' && (
        <TenantTelemetryTab
          telemetry={telemetry}
          currency={ent.currency}
          from={telFrom}
          to={telTo}
          onChangeFrom={setTelFrom}
          onChangeTo={setTelTo}
          onRefresh={() => void reloadTelemetry()}
        />
      )}

      {activeTab === 'subscription' && (
        <TenantSubscriptionTab
          detail={detail}
          plans={plans}
          isSuperAdmin={isSuperAdmin}
          onUpdated={loadAll}
          onError={(m) => {
            setFeedback(null);
            setError(m);
          }}
          onSuccess={(m) => {
            setError(null);
            setFeedback(m);
          }}
        />
      )}

      {activeTab === 'ledger' && (
        <TenantLedgerTab
          tenantId={detail.tenant.id}
          chargesSummary={chargesSummary}
          payments={payments}
          isSuperAdmin={isSuperAdmin}
          onUpdated={loadAll}
          onError={(m) => {
            setFeedback(null);
            setError(m);
          }}
          onSuccess={(m) => {
            setError(null);
            setFeedback(m);
          }}
        />
      )}
    </div>
  );
}
