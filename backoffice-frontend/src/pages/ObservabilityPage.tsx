import { useCallback, useEffect, useState } from 'react';
import { Activity, AlertTriangle, FileSearch, Gauge, RefreshCw, ScrollText, Terminal } from 'lucide-react';
import { AlertBanner } from '../components/AlertBanner';
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
  const [updatingLogger, setUpdatingLogger] = useState<string | null>(null);

  const [envData, setEnvData] = useState<EnvironmentOverviewDto | null>(null);
  const [auditLogs, setAuditLogs] = useState<AuditLogDto[]>([]);
  const [auditTotal, setAuditTotal] = useState(0);
  const [auditActor, setAuditActor] = useState('');
  const [auditAction, setAuditAction] = useState('');

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

  const loadAudit = useCallback(async () => {
    if (!isSuperAdmin) return;
    setError(null);
    try {
      const page = await operationsApi.searchAuditLogs({
        actor: auditActor.trim() || undefined,
        action: auditAction.trim() || undefined,
        page: 0,
        size: 30,
      });
      setAuditLogs(page.content);
      setAuditTotal(page.totalElements);
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : 'Error cargando bitácora de auditoría.');
    }
  }, [isSuperAdmin, auditActor, auditAction]);

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

          <div className="card p-4 flex flex-wrap items-center gap-3">
            <label htmlFor="logger-prefix" className="text-xs font-semibold uppercase text-slate-500">
              Prefijo de Paquete:
            </label>
            <input
              id="logger-prefix"
              className="input-field max-w-sm font-mono"
              value={loggerPrefix}
              onChange={(e) => setLoggerPrefix(e.target.value)}
              placeholder="com.restaurant.app"
            />
          </div>

          {loggersData && (
            <div className="card overflow-hidden">
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
                    {loggersData.loggers.map((l) => (
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
            <button type="button" onClick={() => void loadAudit()} className="btn-primary">
              Filtrar ({auditTotal})
            </button>
          </div>

          <div className="card overflow-hidden">
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
                      <td className="py-2.5 px-4 font-mono text-slate-600 max-w-xs truncate">
                        {log.detailsJson ?? '—'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
