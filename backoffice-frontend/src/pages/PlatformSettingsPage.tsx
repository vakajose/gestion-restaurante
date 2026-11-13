import { useCallback, useEffect, useState } from 'react';
import { Cloud, HardDrive, PlugZap, Save } from 'lucide-react';
import { AlertBanner } from '../components/AlertBanner';
import { StatusBadge } from '../components/StatusBadge';
import { useAuth } from '../context/authContext';
import { operationsApi } from '../services/backofficeApi';
import { ApiError } from '../services/httpClient';
import type { PlatformSettingDto, StorageConnectionTestResult } from '../types/api';
import { formatBytes, formatInstantDateTime } from '../utils/formatters';

export function PlatformSettingsPage() {
  const { isSuperAdmin } = useAuth();
  const [settings, setSettings] = useState<PlatformSettingDto[]>([]);
  const [draftValues, setDraftValues] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(true);
  const [savingKey, setSavingKey] = useState<string | null>(null);
  const [testingConn, setTestingConn] = useState(false);
  const [testResult, setTestResult] = useState<StorageConnectionTestResult | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<string | null>(null);

  const loadSettings = useCallback(async () => {
    if (!isSuperAdmin) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const data = await operationsApi.listSettings();
      setSettings(data);
      const nextDraft: Record<string, string> = {};
      for (const s of data) {
        nextDraft[s.key] = s.value;
      }
      setDraftValues(nextDraft);
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : 'No se pudo cargar platform_settings.');
    } finally {
      setLoading(false);
    }
  }, [isSuperAdmin]);

  useEffect(() => {
    void loadSettings();
  }, [loadSettings]);

  const handleSaveSetting = async (key: string) => {
    setError(null);
    setFeedback(null);
    setSavingKey(key);
    try {
      const updated = await operationsApi.updateSetting(key, draftValues[key] ?? '');
      setSettings((prev) => prev.map((item) => (item.key === key ? updated : item)));
      setDraftValues((prev) => ({ ...prev, [key]: updated.value }));
      setFeedback(`Parámetro "${key}" actualizado e invalidado en caché.`);
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : `No se pudo guardar ${key}.`);
    } finally {
      setSavingKey(null);
    }
  };

  const handleTestConnection = async () => {
    setError(null);
    setTestingConn(true);
    try {
      const result = await operationsApi.testActiveStorageConnection();
      setTestResult(result);
    } catch (err) {
      setError(err instanceof ApiError ? err.detail : 'Falló la prueba de conectividad de Storage.');
    } finally {
      setTestingConn(false);
    }
  };

  const maxFileBytes = Number(draftValues['storage.max_file_size_bytes'] ?? 1048576);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Storage & Configuración de Plataforma</h1>
          <p className="text-sm text-slate-500 mt-1">
            Control dinámico del motor `core.storage` (Local / OCI Object Storage S3) y prueba activa sin reiniciar.
          </p>
        </div>

        <button
          type="button"
          onClick={handleTestConnection}
          disabled={testingConn}
          className="btn-primary"
        >
          <PlugZap className="h-4 w-4" />
          {testingConn ? 'Probando proveedor activo...' : 'Probar Conexión (Proveedor Activo)'}
        </button>
      </div>

      {error && <AlertBanner tone="error" message={error} />}
      {feedback && <AlertBanner tone="success" message={feedback} />}

      {testResult && (
        <div
          className={`card p-4 border-l-4 ${
            testResult.reachable ? 'border-l-success' : 'border-l-danger'
          } flex flex-wrap items-center justify-between gap-4`}
        >
          <div className="flex items-start gap-3">
            {testResult.provider === 'S3' ? (
              <Cloud className="h-5 w-5 text-primary mt-0.5" />
            ) : (
              <HardDrive className="h-5 w-5 text-primary mt-0.5" />
            )}
            <div className="text-sm">
              <div className="flex items-center gap-2">
                <span className="font-bold text-slate-900">Proveedor Activo: {testResult.provider}</span>
                <StatusBadge
                  value={testResult.reachable ? 'UP' : 'DOWN'}
                  customLabel={testResult.reachable ? 'Conectado' : 'Sin Conexión'}
                />
              </div>
              <p className="text-slate-600 mt-0.5">{testResult.message}</p>
              <p className="text-xs font-mono text-slate-400 mt-0.5">
                Destino: {testResult.targetDescription} · Verificado: {formatInstantDateTime(testResult.checkedAt)}
              </p>
            </div>
          </div>
        </div>
      )}

      {!isSuperAdmin ? (
        <AlertBanner
          tone="info"
          title="Vista restringida para PLATFORM_OPERATOR"
          message="Puedes ejecutar pruebas de conectividad del proveedor de almacenamiento activo. Solo SUPERADMIN puede modificar los parámetros globales."
        />
      ) : loading ? (
        <div className="text-sm text-slate-500">Cargando parámetros de `platform_settings`...</div>
      ) : (
        <div className="space-y-4">
          {settings.map((setting) => {
            const val = draftValues[setting.key] ?? '';
            const isProvider = setting.key === 'storage.active_provider';
            const isMaxSize = setting.key === 'storage.max_file_size_bytes';

            return (
              <div key={setting.key} className="card p-5 space-y-3">
                <div className="flex flex-wrap items-start justify-between gap-2">
                  <div>
                    <h2 className="font-mono text-sm font-bold text-slate-900">{setting.key}</h2>
                    {setting.description && (
                      <p className="text-xs text-slate-500 mt-0.5">{setting.description}</p>
                    )}
                  </div>
                  <span className="text-xs text-slate-400">
                    Actualizado: {formatInstantDateTime(setting.updatedAt)}
                  </span>
                </div>

                <div className="flex flex-col sm:flex-row gap-3 items-stretch sm:items-end">
                  <div className="flex-1 space-y-2">
                    {isProvider ? (
                      <select
                        aria-label={setting.key}
                        className="input-field"
                        value={val}
                        onChange={(e) =>
                          setDraftValues((prev) => ({ ...prev, [setting.key]: e.target.value }))
                        }
                      >
                        <option value="LOCAL">LOCAL (Sistema de archivos local)</option>
                        <option value="S3">S3 (OCI Object Storage / S3-Compatible)</option>
                      </select>
                    ) : (
                      <input
                        aria-label={setting.key}
                        className="input-field font-mono"
                        value={val}
                        onChange={(e) =>
                          setDraftValues((prev) => ({ ...prev, [setting.key]: e.target.value }))
                        }
                      />
                    )}

                    {isMaxSize && (
                      <div className="flex flex-wrap items-center gap-3 text-xs text-slate-500">
                        <span>Equivalente actual: {formatBytes(maxFileBytes)}</span>
                        <span>· Rango permitido: 100 KB (102400) a 10 MB (10485760)</span>
                        <div className="flex gap-1.5">
                          {[
                            { label: '512 KB', bytes: '524288' },
                            { label: '1 MB', bytes: '1048576' },
                            { label: '2 MB', bytes: '2097152' },
                            { label: '5 MB', bytes: '5242880' },
                          ].map((preset) => (
                            <button
                              key={preset.bytes}
                              type="button"
                              onClick={() =>
                                setDraftValues((prev) => ({ ...prev, [setting.key]: preset.bytes }))
                              }
                              className="rounded border border-slate-300 px-2 py-0.5 hover:bg-slate-100 text-slate-700"
                            >
                              {preset.label}
                            </button>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>

                  <button
                    type="button"
                    onClick={() => handleSaveSetting(setting.key)}
                    disabled={savingKey === setting.key}
                    className="btn-secondary shrink-0"
                  >
                    <Save className="h-4 w-4" />
                    {savingKey === setting.key ? 'Guardando...' : 'Guardar Cambio'}
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
