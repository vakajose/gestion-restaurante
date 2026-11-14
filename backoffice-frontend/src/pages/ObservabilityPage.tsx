import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Activity,
  AlertTriangle,
  Braces,
  Check,
  ChevronLeft,
  ChevronRight,
  Copy,
  FileSearch,
  Gauge,
  RefreshCw,
  ScrollText,
  Search,
  Terminal,
} from 'lucide-react';
import { AlertBanner } from '../components/AlertBanner';
import { Card } from '../components/Card';
import { EmptyState } from '../components/EmptyState';
import { Modal } from '../components/Modal';
import { StatusBadge } from '../components/StatusBadge';
import { useAuth } from '../context/authContext';
import { operationsApi } from '../services/backofficeApi';
import { ApiError } from '../services/httpClient';
import type {
  AuditLogDto,
  EnvironmentOverviewDto,
  HealthOverviewDto,
  LoggersOverviewDto,
  MetricsOverviewDto,
} from '../types/api';
import { formatInstantDateTime } from '../utils/formatters';

type ObsTab = 'health' | 'metrics' | 'loggers' | 'environment' | 'audit';

export function ObservabilityPage() {
  const { isSuperAdmin } = useAuth();
  const [activeTab, setActiveTab] = useState<ObsTab>('health');
  const [error, setError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);

  const [health, setHealth] = useState<HealthOverviewDto | null>(null);
  const [metrics, setMetrics] = useState<MetricsOverviewDto | null>(null);
  const [loggersData, setLoggersData] = useState<LoggersOverviewDto | null>(null);
  const [loggerPrefix, setLoggerPrefix] = useState('com.restaurant.app');
  const [loggerSearch, setLoggerSearch] = useState('');
  const [loggerLevelFilter, setLoggerLevelFilter] = useState('ALL');
  const [updatingLogger, setUpdatingLogger] = useState<string | null>(null);

  const [envData, setEnvData] = useState<EnvironmentOverviewDto | null>(null);
  const [auditLogs, setAuditLogs] = useState<AuditLogDto[]>([]);
  const [auditTotal, setAuditTotal] = useState(0);
  const [auditTotalPages, setAuditTotalPages] = useState(0);
  const [auditPage, setAuditPage] = useState(0);
  const [auditSize, setAuditSize] = useState(25);
  const [auditActor, setAuditActor] = useState('');
  const [auditAction, setAuditAction] = useState('');
  const [auditDetailSearch, setAuditDetailSearch] = useState('');
  const [appliedAuditFilters, setAppliedAuditFilters] = useState({ actor: '', action: '', search: '' });
  const [selectedAuditLog, setSelectedAuditLog] = useState<AuditLogDto | null>(null);
  const [copiedJson, setCopiedJson] = useState(false);

  const loadHealthAndMetrics = useCallback(async () => {
    setError(null);
    try {
      const [h, m] = await Promise.all([operationsApi.getHealth(), operationsApi.getMetrics()]);
      setHealth(h);
      setMetrics(m);
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : 'Error consultando salud/métricas Actuator.');
    }
  }, []);

  const loadLoggers = useCallback(async (prefix: string) => {
    setError(null);
    try {
      setLoggersData(await operationsApi.getLoggers(prefix));
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : 'Error cargando loggers del servidor.');
    }
  }, []);

  const loadEnvironment = useCallback(async () => {
    if (!isSuperAdmin) return;
    setError(null);
    try {
      setEnvData(await operationsApi.getEnvironment());
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : 'Error consultando propiedades de entorno.');
    }
  }, [isSuperAdmin]);

  const loadAudit = useCallback(
    async (pageOverride?: number) => {
      if (!isSuperAdmin) return;
      setError(null);
      const targetPage = pageOverride ?? auditPage;
      try {
        const page = await operationsApi.searchAuditLogs({
          actor: appliedAuditFilters.actor.trim() || undefined,
          action: appliedAuditFilters.action.trim() || undefined,
          search: appliedAuditFilters.search.trim() || undefined,
          page: targetPage,
          size: auditSize,
        });
        setAuditLogs(page.content);
        setAuditTotal(page.totalElements);
        setAuditTotalPages(page.totalPages);
        setAuditPage(page.number);
      } catch (err) {
        setError(err instanceof ApiError ? err.detail : 'Error cargando bitácora de auditoría.');
      }
    },
    [isSuperAdmin, appliedAuditFilters, auditPage, auditSize],
  );

  useEffect(() => {
    if (activeTab === 'health' || activeTab === 'metrics') {
      void loadHealthAndMetrics();
    } else if (activeTab === 'loggers') {
      void loadLoggers(loggerPrefix);
    } else if (activeTab === 'environment') {
      void loadEnvironment();
    } else if (activeTab === 'audit') {
      void loadAudit();
    }
  }, [activeTab, loggerPrefix, loadHealthAndMetrics, loadLoggers, loadEnvironment, loadAudit]);

  const handleChangeLoggerLevel = async (loggerName: string, level: string) => {
    setError(null);
    setFeedback(null);
    setUpdatingLogger(loggerName);
    try {
      const updated = await operationsApi.configureLogger(loggerName, level);
      setLoggersData((prev) =>
        prev
          ? {
              ...prev,
              loggers: prev.loggers.map((l) => (l.name === loggerName ? updated : l)),
            }
          : prev,
      );
      setFeedback(`Logger "${loggerName}" actualizado en caliente a nivel ${updated.effectiveLevel ?? level}.`);
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : 'Error actualizando nivel de logger.');
    } finally {
      setUpdatingLogger(null);
    }
  };

  const filteredLoggers = useMemo(() => {
    const all = loggersData?.loggers ?? [];
    const q = loggerSearch.trim().toLowerCase();
    return all.filter((l) => {
      if (q && !l.name.toLowerCase().includes(q)) return false;
      if (loggerLevelFilter !== 'ALL' && (l.effectiveLevel ?? 'INHERITED') !== loggerLevelFilter) {
        return false;
      }
      return true;
    });
  }, [loggersData, loggerSearch, loggerLevelFilter]);

  const formattedSelectedJson = useMemo(() => {
    const raw = selectedAuditLog?.detailsJson;
    if (!raw) return 'Sin payload adicional registrado.';
    try {
      return JSON.stringify(JSON.parse(raw), null, 2);
    } catch {
      return raw;
    }
  }, [selectedAuditLog]);

  const handleCopyAuditJson = async () => {
    try {
      await navigator.clipboard.writeText(formattedSelectedJson);
      setCopiedJson(true);
      setTimeout(() => setCopiedJson(false), 2000);
    } catch {
      setError('No se pudo copiar el JSON al portapapeles.');
    }
  };

  const hasVerboseLoggers = Boolean(
    loggersData?.loggers.some((l) => l.effectiveLevel === 'DEBUG' || l.effectiveLevel === 'TRACE'),
  );

  const allTabs: Array<{ id: ObsTab; label: string; icon: typeof Activity; superAdminOnly?: boolean }> = [
    { id: 'health', label: 'Salud del Sistema', icon: Activity },
    { id: 'metrics', label: 'Métricas JVM & HTTP', icon: Gauge },
    { id: 'loggers', label: 'Loggers en Caliente', icon: Terminal },
    { id: 'environment', label: 'Configuración Activa', icon: FileSearch, superAdminOnly: true },
    { id: 'audit', label: 'Auditoría Inmutable', icon: ScrollText, superAdminOnly: true },
  ];
  const tabs = allTabs.filter((t) => !t.superAdminOnly || isSuperAdmin);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Observabilidad & Diagnóstico en Vivo</h1>
          <p className="text-sm text-slate-500 mt-1">
            Salud Actuator, métricas JVM/HikariCP, cambio en caliente de logs y auditoría de plataforma.
          </p>
        </div>
        <button
          type="button"
          onClick={() => {
            if (activeTab === 'health' || activeTab === 'metrics') void loadHealthAndMetrics();
            else if (activeTab === 'loggers') void loadLoggers(loggerPrefix);
            else if (activeTab === 'environment') void loadEnvironment();
            else void loadAudit();
          }}
          className="btn-secondary"
        >
          <RefreshCw className="h-4 w-4" />
          Refrescar
        </button>
      </div>

      {/* Tabs */}
      <div className="flex flex-wrap gap-2 border-b border-border pb-3">
        {tabs.map((tab) => {
          const Icon = tab.icon;
          const active = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              type="button"
              onClick={() => setActiveTab(tab.id)}
              className={`inline-flex items-center gap-2 rounded-lg px-3.5 py-2 text-sm font-medium transition-colors ${
                active
                  ? 'bg-primary text-white shadow-sm'
                  : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-50'
              }`}
            >
              <Icon className="h-4 w-4" />
              {tab.label}
            </button>
          );
        })}
      </div>

      {error && <AlertBanner tone="error" message={error} />}
      {feedback && <AlertBanner tone="success" message={feedback} />}

      {/* Tab Health */}
      {activeTab === 'health' && health && (
        <div className="space-y-4">
          <div className="card p-5 flex items-center justify-between">
            <div>
              <p className="text-xs font-semibold uppercase text-slate-400">Estado Global del Backend</p>
              <p className="text-xs text-slate-500 mt-0.5">
                Verificado: {formatInstantDateTime(health.checkedAt)}
              </p>
            </div>
            <StatusBadge value={health.status} />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {Object.entries(health.components).map(([name, node]) => (
              <div key={name} className="card p-4 space-y-2">
                <div className="flex items-center justify-between">
                  <span className="font-mono text-sm font-bold text-slate-800">{name}</span>
                  <StatusBadge value={node.status} />
                </div>
                {Object.keys(node.details).length > 0 && (
                  <dl className="text-xs space-y-1 bg-slate-50 rounded-lg p-2.5 overflow-x-auto">
                    {Object.entries(node.details).map(([k, v]) => (
                      <div key={k} className="flex justify-between gap-3">
                        <dt className="text-slate-500 font-medium">{k}</dt>
                        <dd className="font-mono text-slate-700 truncate">{String(v)}</dd>
                      </div>
                    ))}
                  </dl>
                )}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Tab Metrics */}
      {activeTab === 'metrics' && metrics && (
        <div className="space-y-4">
          <div className="flex items-center justify-between text-xs text-slate-500">
            <span>Total de medidores Micrometer activos: {metrics.totalAvailableMeters}</span>
            <span>Colectado: {formatInstantDateTime(metrics.collectedAt)}</span>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {metrics.metrics.map((metric) => (
              <div key={metric.name} className="card p-4 space-y-2">
                <div className="flex items-start justify-between gap-2">
                  <div>
                    <p className="font-mono text-sm font-bold text-slate-800">{metric.name}</p>
                    {metric.description && <p className="text-xs text-slate-500">{metric.description}</p>}
                  </div>
                  {metric.baseUnit && (
                    <span className="rounded bg-slate-100 px-2 py-0.5 text-[11px] font-mono text-slate-600">
                      {metric.baseUnit}
                    </span>
                  )}
                </div>
                <div className="flex flex-wrap gap-3 pt-1">
                  {metric.measurements.map((m) => (
                    <div key={m.statistic} className="rounded-lg bg-slate-50 px-3 py-1.5 text-xs">
                      <span className="text-slate-400 mr-1.5">{m.statistic}:</span>
                      <span className="font-mono font-bold text-slate-800">
                        {Number.isFinite(m.value) ? m.value.toLocaleString('es-BO', { maximumFractionDigits: 3 }) : String(m.value)}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Tab Loggers */}
      {activeTab === 'loggers' && (
        <div className="space-y-4">
          {hasVerboseLoggers && (
            <div
              role="alert"
              className="flex items-start gap-3 rounded-xl border border-amber-300 bg-warning-light px-4 py-3 text-sm text-warning"
            >
              <AlertTriangle className="h-5 w-5 shrink-0 mt-0.5" />
              <div>
                <p className="font-bold">Alerta preventiva: existen loggers en DEBUG o TRACE</p>
                <p>
                  Mantener niveles verbosos prolongadamente incrementa el consumo de disco e I/O en producción. Recuerda restaurar a `INFO` tras diagnosticar.
                </p>
              </div>
            </div>
          )}

          <div className="card p-4 flex flex-wrap items-end gap-3">
            <div>
              <label htmlFor="logger-prefix" className="label-text">
                Prefijo Backend
              </label>
              <input
                id="logger-prefix"
                className="input-field max-w-xs font-mono"
                value={loggerPrefix}
                onChange={(e) => setLoggerPrefix(e.target.value)}
                placeholder="com.restaurant.app"
              />
            </div>

            <div className="flex-1 min-w-[220px]">
              <label htmlFor="logger-search" className="label-text">
                Buscar Logger
              </label>
              <div className="relative">
                <Search className="h-4 w-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  id="logger-search"
                  className="input-field pl-9 font-mono"
                  value={loggerSearch}
                  onChange={(e) => setLoggerSearch(e.target.value)}
                  placeholder="Filtrar por clase o paquete (ej. pos, catalog)..."
                />
              </div>
            </div>

            <div>
              <label htmlFor="logger-level-filter" className="label-text">
                Nivel Efectivo
              </label>
              <select
                id="logger-level-filter"
                className="input-field w-auto min-w-[130px]"
                value={loggerLevelFilter}
                onChange={(e) => setLoggerLevelFilter(e.target.value)}
              >
                <option value="ALL">Todos</option>
                {['TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR', 'OFF'].map((lvl) => (
                  <option key={lvl} value={lvl}>
                    {lvl}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {loggersData && (
            <div className="card overflow-hidden">
              <div className="px-4 py-2.5 bg-slate-50 border-b border-border flex items-center justify-between text-xs text-slate-500">
                <span>
                  Mostrando <strong className="text-slate-800">{filteredLoggers.length}</strong> de{' '}
                  <strong className="text-slate-800">{loggersData.loggers.length}</strong> loggers
                </span>
              </div>
              {filteredLoggers.length === 0 ? (
                <EmptyState message="Ningún logger coincide con el filtro actual." />
              ) : (
                <div className="overflow-x-auto max-h-[520px]">
                  <table className="w-full text-left text-sm">
                    <thead className="bg-slate-50 border-b border-border text-xs uppercase text-slate-500 sticky top-0">
                      <tr>
                        <th className="py-3 px-4">Logger / Paquete</th>
                        <th className="py-3 px-4">Nivel Efectivo</th>
                        <th className="py-3 px-4 text-right">Cambiar en Caliente</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-100">
                      {filteredLoggers.map((l) => (
                        <tr key={l.name} className="hover:bg-slate-50/70">
                          <td className="py-2.5 px-4 font-mono text-xs text-slate-800">{l.name}</td>
                          <td className="py-2.5 px-4">
                            <span
                              className={`inline-flex rounded px-2 py-0.5 text-xs font-bold ${
                                l.effectiveLevel === 'DEBUG' || l.effectiveLevel === 'TRACE'
                                  ? 'bg-warning-light text-warning'
                                  : l.effectiveLevel === 'ERROR'
                                    ? 'bg-danger-light text-danger'
                                    : 'bg-slate-100 text-slate-700'
                              }`}
                            >
                              {l.effectiveLevel ?? 'INHERITED'}
                            </span>
                          </td>
                          <td className="py-2.5 px-4 text-right">
                            {isSuperAdmin ? (
                              <select
                                aria-label={`Nivel para ${l.name}`}
                                disabled={updatingLogger === l.name}
                                className="rounded-lg border border-slate-300 bg-white px-2.5 py-1 text-xs font-mono"
                                value={l.configuredLevel ?? l.effectiveLevel ?? 'INFO'}
                                onChange={(e) => handleChangeLoggerLevel(l.name, e.target.value)}
                              >
                                {['TRACE', 'DEBUG', 'INFO', 'WARN', 'ERROR', 'OFF', 'RESET'].map((lvl) => (
                                  <option key={lvl} value={lvl}>
                                    {lvl}
                                  </option>
                                ))}
                              </select>
                            ) : (
                              <span className="text-xs text-slate-400">Solo SUPERADMIN</span>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* Tab Environment */}
      {activeTab === 'environment' && envData && (
        <div className="card p-5 space-y-4">
          <div className="flex flex-wrap gap-2 text-xs">
            <span className="font-semibold text-slate-500">Perfiles activos:</span>
            {envData.activeProfiles.map((p) => (
              <StatusBadge key={p} value="INFO" customLabel={p} />
            ))}
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 border-b border-border uppercase text-slate-500">
                <tr>
                  <th className="py-2.5 px-3">Propiedad</th>
                  <th className="py-2.5 px-3">Valor Efectivo (Secretos Enmascarados)</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {Object.entries(envData.properties).map(([k, v]) => (
                  <tr key={k}>
                    <td className="py-2 px-3 font-mono font-semibold text-slate-700">{k}</td>
                    <td className="py-2 px-3 font-mono text-slate-600">{v}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Tab Audit */}
      {activeTab === 'audit' && (
        <div className="space-y-4">
          <div className="card p-4 flex flex-wrap gap-3 items-end">
            <div>
              <label htmlFor="audit-actor" className="label-text">
                Actor Username
              </label>
              <input
                id="audit-actor"
                className="input-field"
                value={auditActor}
                onChange={(e) => setAuditActor(e.target.value)}
                placeholder="superadmin"
              />
            </div>
            <div>
              <label htmlFor="audit-action" className="label-text">
                Acción
              </label>
              <input
                id="audit-action"
                className="input-field font-mono"
                value={auditAction}
                onChange={(e) => setAuditAction(e.target.value)}
                placeholder="LOGGER_LEVEL_CHANGED"
              />
            </div>
            <div className="flex-1 min-w-[220px]">
              <label htmlFor="audit-search" className="label-text">
                Buscar en Detalle / Objetivo
              </label>
              <div className="relative">
                <Search className="h-4 w-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  id="audit-search"
                  className="input-field pl-9 font-mono"
                  value={auditDetailSearch}
                  onChange={(e) => setAuditDetailSearch(e.target.value)}
                  placeholder="tenantId, username, JSON..."
                />
              </div>
            </div>
            <div>
              <label htmlFor="audit-size" className="label-text">
                Por página
              </label>
              <select
                id="audit-size"
                className="input-field w-auto"
                value={auditSize}
                onChange={(e) => {
                  setAuditSize(Number(e.target.value));
                  setAuditPage(0);
                }}
              >
                {[20, 25, 50, 100].map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </select>
            </div>
            <button
              type="button"
              onClick={() => {
                setAuditPage(0);
                setAppliedAuditFilters({
                  actor: auditActor,
                  action: auditAction,
                  search: auditDetailSearch,
                });
              }}
              className="btn-primary"
            >
              Filtrar ({auditTotal})
            </button>
          </div>

          <Card padding="none">
            {auditLogs.length === 0 ? (
              <EmptyState message="No se encontraron eventos de auditoría con los filtros seleccionados." />
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead className="bg-slate-50 border-b border-border uppercase text-slate-500">
                    <tr>
                      <th className="py-3 px-4">Fecha</th>
                      <th className="py-3 px-4">Actor</th>
                      <th className="py-3 px-4">Acción</th>
                      <th className="py-3 px-4">Objetivo</th>
                      <th className="py-3 px-4">IP</th>
                      <th className="py-3 px-4">Detalle</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {auditLogs.map((log) => (
                      <tr key={log.id} className="hover:bg-slate-50/70">
                        <td className="py-2.5 px-4 whitespace-nowrap text-slate-500">
                          {formatInstantDateTime(log.createdAt)}
                        </td>
                        <td className="py-2.5 px-4 font-semibold text-slate-800">{log.actorUsername}</td>
                        <td className="py-2.5 px-4 font-mono text-primary font-semibold">{log.action}</td>
                        <td className="py-2.5 px-4 font-mono text-slate-600">
                          {log.targetType}: {log.targetId ?? '—'}
                        </td>
                        <td className="py-2.5 px-4 font-mono text-slate-500">{log.ipAddress ?? '—'}</td>
                        <td className="py-2.5 px-4 font-mono text-slate-600">
                          <div className="flex items-center justify-between gap-2 max-w-xs">
                            <span className="truncate">{log.detailsJson ?? '—'}</span>
                            <button
                              type="button"
                              onClick={() => {
                                setSelectedAuditLog(log);
                                setCopiedJson(false);
                              }}
                              className="btn-secondary text-[11px] py-1 px-2 shrink-0"
                            >
                              <Braces className="h-3.5 w-3.5" />
                              Ver JSON
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            <div className="px-4 py-3 bg-slate-50 border-t border-border flex flex-wrap items-center justify-between gap-3 text-xs text-slate-600">
              <span>
                Página <strong>{auditTotalPages === 0 ? 0 : auditPage + 1}</strong> de{' '}
                <strong>{auditTotalPages}</strong> · <strong>{auditTotal}</strong> registros totales
              </span>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  disabled={auditPage <= 0}
                  onClick={() => void loadAudit(auditPage - 1)}
                  className="btn-secondary text-xs py-1 px-2.5 disabled:opacity-40"
                >
                  <ChevronLeft className="h-4 w-4" />
                  Anterior
                </button>
                <button
                  type="button"
                  disabled={auditPage + 1 >= auditTotalPages}
                  onClick={() => void loadAudit(auditPage + 1)}
                  className="btn-secondary text-xs py-1 px-2.5 disabled:opacity-40"
                >
                  Siguiente
                  <ChevronRight className="h-4 w-4" />
                </button>
              </div>
            </div>
          </Card>
        </div>
      )}

      {/* Modal Visor JSON Formateado de Auditoría */}
      <Modal
        open={selectedAuditLog !== null}
        onClose={() => setSelectedAuditLog(null)}
        maxWidth="2xl"
        closeAriaLabel="Cerrar visor JSON"
        contentClassName="flex flex-col overflow-hidden"
        title={
          <span className="flex items-center gap-2">
            <Braces className="h-5 w-5 text-primary" />
            Detalle de Evento de Auditoría
          </span>
        }
        subtitle={
          selectedAuditLog ? (
            <span className="font-mono">
              {selectedAuditLog.action} · {selectedAuditLog.actorUsername} ·{' '}
              {formatInstantDateTime(selectedAuditLog.createdAt)}
            </span>
          ) : undefined
        }
      >
        {selectedAuditLog && (
          <>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs bg-slate-50 rounded-lg p-3 border border-border">
              <div>
                <span className="text-slate-400">Objetivo:</span>{' '}
                <span className="font-mono font-semibold text-slate-800">
                  {selectedAuditLog.targetType}: {selectedAuditLog.targetId ?? '—'}
                </span>
              </div>
              <div>
                <span className="text-slate-400">IP Origen:</span>{' '}
                <span className="font-mono text-slate-700">{selectedAuditLog.ipAddress ?? '—'}</span>
              </div>
            </div>

            <div className="flex-1 overflow-auto rounded-xl bg-slate-900 text-slate-100 p-4 text-xs font-mono leading-relaxed">
              <pre data-testid="audit-json-pre" className="whitespace-pre-wrap break-words">
                {formattedSelectedJson}
              </pre>
            </div>

            <div className="flex justify-end gap-2 pt-2 border-t border-slate-100">
              <button
                type="button"
                onClick={() => void handleCopyAuditJson()}
                className="btn-secondary text-xs"
              >
                {copiedJson ? (
                  <>
                    <Check className="h-4 w-4 text-success" />
                    Copiado
                  </>
                ) : (
                  <>
                    <Copy className="h-4 w-4" />
                    Copiar JSON
                  </>
                )}
              </button>
              <button
                type="button"
                onClick={() => setSelectedAuditLog(null)}
                className="btn-primary text-xs"
              >
                Cerrar
              </button>
            </div>
          </>
        )}
      </Modal>
    </div>
  );
}
