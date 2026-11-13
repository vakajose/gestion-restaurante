import { useCallback, useEffect, useState, type FormEvent } from 'react';
import { AlertTriangle, ExternalLink, Plus, Search, X } from 'lucide-react';
import { Link } from 'react-router-dom';
import { AlertBanner } from '../components/AlertBanner';
import { StatusBadge } from '../components/StatusBadge';
import { tenantsApi } from '../services/backofficeApi';
import { ApiError } from '../services/httpClient';
import type { BackofficeTenantListItemDto, OnboardingRequest, SubscriptionStatus } from '../types/api';
import { formatInstantDateTime, formatCurrency } from '../utils/formatters';

function buildSlug(name: string): string {
  return name
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
}

export function TenantsPage() {
  const [tenants, setTenants] = useState<BackofficeTenantListItemDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);

  // Filtros
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<SubscriptionStatus | ''>('');
  const [onlyDebt, setOnlyDebt] = useState(false);
  const [onlyOverQuota, setOnlyOverQuota] = useState(false);

  // Modal Onboarding Nuevo Inquilino
  const [modalOpen, setModalOpen] = useState(false);
  const [creating, setCreating] = useState(false);
  const [legalName, setLegalName] = useState('');
  const [slug, setSlug] = useState('');
  const [slugTouched, setSlugTouched] = useState(false);
  const [nitOrTaxId, setNitOrTaxId] = useState('');
  const [branchName, setBranchName] = useState('Casa Matriz');
  const [branchAddress, setBranchAddress] = useState('');
  const [timezone, setTimezone] = useState('America/La_Paz');
  const [businessTemplate, setBusinessTemplate] =
    useState<OnboardingRequest['businessTemplate']>('TRADITIONAL');
  const [includeDemoCatalog, setIncludeDemoCatalog] = useState(true);
  const [adminFullName, setAdminFullName] = useState('');
  const [adminUsername, setAdminUsername] = useState('');
  const [adminEmail, setAdminEmail] = useState('');
  const [adminPassword, setAdminPassword] = useState('');

  const loadTenants = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await tenantsApi.listTenants({
        search: search.trim() || undefined,
        status: statusFilter,
        hasDebt: onlyDebt ? true : null,
        overQuota: onlyOverQuota ? true : null,
      });
      setTenants(data);
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : 'Error cargando lista de inquilinos.');
    } finally {
      setLoading(false);
    }
  }, [search, statusFilter, onlyDebt, onlyOverQuota]);

  useEffect(() => {
    void loadTenants();
  }, [loadTenants]);

  const handleLegalNameChange = (val: string) => {
    setLegalName(val);
    if (!slugTouched) {
      setSlug(buildSlug(val));
    }
  };

  const handleProvision = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setFeedback(null);
    setCreating(true);
    try {
      const result = await tenantsApi.provisionTenant({
        company: {
          legalName: legalName.trim(),
          slug: slug.trim().toLowerCase(),
          nitOrTaxId: nitOrTaxId.trim() || null,
        },
        branch: {
          name: branchName.trim(),
          address: branchAddress.trim(),
          timezone: timezone.trim() || 'America/La_Paz',
          currency: 'BOB',
        },
        businessTemplate,
        includeDemoCatalog,
        adminUser: {
          fullName: adminFullName.trim(),
          username: adminUsername.trim(),
          email: adminEmail.trim(),
          password: adminPassword,
        },
      });
      setModalOpen(false);
      setLegalName('');
      setSlug('');
      setSlugTouched(false);
      setNitOrTaxId('');
      setBranchAddress('');
      setAdminFullName('');
      setAdminUsername('');
      setAdminEmail('');
      setAdminPassword('');
      setFeedback(`Inquilino "${result.slug}" aprovisionado correctamente con TRIAL_14D.`);
      await loadTenants();
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : 'No se pudo crear el inquilino.');
    } finally {
      setCreating(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Cartera de Inquilinos</h1>
          <p className="text-sm text-slate-500 mt-1">
            Supervisión comercial, deuda acumulada, alertas de cuota y accesos por tenant.
          </p>
        </div>
        <button type="button" onClick={() => setModalOpen(true)} className="btn-primary">
          <Plus className="h-4 w-4" />
          Nuevo Inquilino
        </button>
      </div>

      {error && <AlertBanner tone="error" message={error} />}
      {feedback && <AlertBanner tone="success" message={feedback} />}

      {/* Filtros */}
      <div className="card p-4 flex flex-wrap items-center gap-3">
        <div className="relative flex-1 min-w-[220px]">
          <Search className="h-4 w-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            className="input-field pl-9"
            placeholder="Buscar por nombre, NIT o slug..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>

        <select
          aria-label="Filtrar por estado"
          className="input-field w-auto min-w-[170px]"
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value as SubscriptionStatus | '')}
        >
          <option value="">Todos los estados</option>
          <option value="ACTIVE">Activos</option>
          <option value="TRIAL">En Trial</option>
          <option value="GRACE_PERIOD">En Gracia</option>
          <option value="SUSPENDED">Suspendidos</option>
        </select>

        <label className="inline-flex items-center gap-2 text-xs font-semibold text-slate-700 cursor-pointer select-none px-2">
          <input
            type="checkbox"
            checked={onlyDebt}
            onChange={(e) => setOnlyDebt(e.target.checked)}
            className="rounded border-slate-300 text-primary focus:ring-primary"
          />
          Solo con deuda
        </label>

        <label className="inline-flex items-center gap-2 text-xs font-semibold text-slate-700 cursor-pointer select-none px-2">
          <input
            type="checkbox"
            checked={onlyOverQuota}
            onChange={(e) => setOnlyOverQuota(e.target.checked)}
            className="rounded border-slate-300 text-primary focus:ring-primary"
          />
          Excedidos en cuota
        </label>
      </div>

      {/* Tabla */}
      <div className="card overflow-hidden">
        {loading ? (
          <div className="p-6 text-sm text-slate-500">Cargando cartera de inquilinos...</div>
        ) : tenants.length === 0 ? (
          <div className="p-8 text-center text-sm text-slate-500">
            No se encontraron inquilinos para los filtros actuales.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 border-b border-border text-xs uppercase text-slate-500">
                <tr>
                  <th className="py-3 px-4">Restaurante</th>
                  <th className="py-3 px-4">Plan & Vigencia</th>
                  <th className="py-3 px-4">Estado / Bloqueo</th>
                  <th className="py-3 px-4 text-right">Saldo Pendiente</th>
                  <th className="py-3 px-4 text-right">Uso Rápido</th>
                  <th className="py-3 px-4 text-right">Ventas 30d</th>
                  <th className="py-3 px-4 text-right">Ficha 360°</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {tenants.map((t) => (
                  <tr key={t.tenantId} className="hover:bg-slate-50/70">
                    <td className="py-3 px-4">
                      <Link
                        to={`/tenants/${t.tenantId}`}
                        className="font-bold text-slate-800 hover:text-primary"
                      >
                        {t.legalName}
                      </Link>
                      <div className="text-xs text-slate-500 font-mono">
                        {t.slug} {t.nitOrTaxId ? `· NIT ${t.nitOrTaxId}` : ''}
                      </div>
                    </td>
                    <td className="py-3 px-4">
                      <p className="font-semibold text-slate-700">{t.planName ?? 'Sin plan'}</p>
                      <p className="text-xs text-slate-500">
                        Vence: {formatInstantDateTime(t.currentPeriodEnd)}
                      </p>
                    </td>
                    <td className="py-3 px-4 space-y-1">
                      <div className="flex flex-wrap gap-1.5">
                        <StatusBadge value={t.subscriptionStatus} />
                        {t.effectiveSuspensionMode !== 'NONE' && (
                          <StatusBadge value={t.effectiveSuspensionMode} />
                        )}
                      </div>
                      {t.overQuota && (
                        <div className="inline-flex items-center gap-1 text-[11px] font-semibold text-danger">
                          <AlertTriangle className="h-3.5 w-3.5" />
                          Excedido: {t.overQuotaResources.join(', ')}
                        </div>
                      )}
                    </td>
                    <td className="py-3 px-4 text-right font-semibold">
                      <span className={t.pendingBalance > 0 ? 'text-warning' : 'text-slate-600'}>
                        {formatCurrency(t.pendingBalance)}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-right text-xs text-slate-600">
                      <div>{t.activeBranches} suc · {t.activeUsers} usr</div>
                      <div>{t.monthlyOrders} órd/mes</div>
                    </td>
                    <td className="py-3 px-4 text-right font-semibold text-slate-800">
                      {formatCurrency(t.salesLast30Days)}
                    </td>
                    <td className="py-3 px-4 text-right">
                      <Link to={`/tenants/${t.tenantId}`} className="btn-secondary text-xs">
                        Gestionar
                        <ExternalLink className="h-3.5 w-3.5" />
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modal Provisioning */}
      {modalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/50 p-4">
          <div className="card w-full max-w-2xl max-h-[90vh] overflow-y-auto p-6 space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-bold text-slate-900">Aprovisionar Nuevo Inquilino</h2>
              <button
                type="button"
                onClick={() => setModalOpen(false)}
                className="text-slate-400 hover:text-slate-700"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <form onSubmit={handleProvision} className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div className="sm:col-span-2">
                  <label htmlFor="onb-legal" className="label-text">
                    Razón Social / Restaurante
                  </label>
                  <input
                    id="onb-legal"
                    required
                    className="input-field"
                    value={legalName}
                    onChange={(e) => handleLegalNameChange(e.target.value)}
                    placeholder="Sabores Andinos S.R.L."
                  />
                </div>
                <div>
                  <label htmlFor="onb-nit" className="label-text">
                    NIT / Tax ID
                  </label>
                  <input
                    id="onb-nit"
                    className="input-field"
                    value={nitOrTaxId}
                    onChange={(e) => setNitOrTaxId(e.target.value)}
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div>
                  <label htmlFor="onb-slug" className="label-text">
                    Slug Identificador
                  </label>
                  <input
                    id="onb-slug"
                    required
                    className="input-field font-mono"
                    value={slug}
                    onChange={(e) => {
                      setSlugTouched(true);
                      setSlug(e.target.value);
                    }}
                  />
                </div>
                <div>
                  <label htmlFor="onb-tpl" className="label-text">
                    Plantilla Rubro
                  </label>
                  <select
                    id="onb-tpl"
                    className="input-field"
                    value={businessTemplate}
                    onChange={(e) =>
                      setBusinessTemplate(e.target.value as OnboardingRequest['businessTemplate'])
                    }
                  >
                    <option value="TRADITIONAL">TRADITIONAL</option>
                    <option value="FAST_FOOD">FAST_FOOD</option>
                    <option value="CAFE">CAFE</option>
                    <option value="BAR">BAR</option>
                    <option value="EMPTY">EMPTY</option>
                  </select>
                </div>
                <div className="flex items-end pb-2">
                  <label className="inline-flex items-center gap-2 text-xs font-semibold text-slate-700 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={includeDemoCatalog}
                      onChange={(e) => setIncludeDemoCatalog(e.target.checked)}
                      className="rounded border-slate-300 text-primary"
                    />
                    Incluir catálogo demo
                  </label>
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div>
                  <label htmlFor="onb-branch" className="label-text">
                    Sucursal Principal
                  </label>
                  <input
                    id="onb-branch"
                    required
                    className="input-field"
                    value={branchName}
                    onChange={(e) => setBranchName(e.target.value)}
                  />
                </div>
                <div>
                  <label htmlFor="onb-address" className="label-text">
                    Dirección
                  </label>
                  <input
                    id="onb-address"
                    required
                    className="input-field"
                    value={branchAddress}
                    onChange={(e) => setBranchAddress(e.target.value)}
                  />
                </div>
                <div>
                  <label htmlFor="onb-tz" className="label-text">
                    Zona Horaria
                  </label>
                  <input
                    id="onb-tz"
                    required
                    className="input-field"
                    value={timezone}
                    onChange={(e) => setTimezone(e.target.value)}
                  />
                </div>
              </div>

              <div className="pt-2 border-t border-slate-100">
                <p className="text-xs font-bold uppercase tracking-wide text-slate-500 mb-2">
                  Administrador Inicial del Tenant (`ADMIN_TENANT`)
                </p>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label htmlFor="onb-afull" className="label-text">
                      Nombre Completo
                    </label>
                    <input
                      id="onb-afull"
                      required
                      className="input-field"
                      value={adminFullName}
                      onChange={(e) => setAdminFullName(e.target.value)}
                    />
                  </div>
                  <div>
                    <label htmlFor="onb-auser" className="label-text">
                      Username
                    </label>
                    <input
                      id="onb-auser"
                      required
                      className="input-field"
                      value={adminUsername}
                      onChange={(e) => setAdminUsername(e.target.value)}
                    />
                  </div>
                  <div>
                    <label htmlFor="onb-aemail" className="label-text">
                      Correo
                    </label>
                    <input
                      id="onb-aemail"
                      type="email"
                      required
                      className="input-field"
                      value={adminEmail}
                      onChange={(e) => setAdminEmail(e.target.value)}
                    />
                  </div>
                  <div>
                    <label htmlFor="onb-apass" className="label-text">
                      Contraseña Inicial
                    </label>
                    <input
                      id="onb-apass"
                      type="password"
                      required
                      minLength={8}
                      className="input-field"
                      value={adminPassword}
                      onChange={(e) => setAdminPassword(e.target.value)}
                    />
                  </div>
                </div>
              </div>

              <div className="flex justify-end gap-2 pt-3 border-t border-slate-100">
                <button type="button" onClick={() => setModalOpen(false)} className="btn-secondary">
                  Cancelar
                </button>
                <button type="submit" disabled={creating} className="btn-primary">
                  {creating ? 'Aprovisionando...' : 'Crear Inquilino (Trial 14d)'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
