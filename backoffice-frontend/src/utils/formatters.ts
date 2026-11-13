/**
 * Formatea una fecha de calendario pura ISO ("YYYY-MM-DD") a dd/MM/yyyy sin desplazamientos horarios (ADR-0006).
 */
export function formatCalendarDate(isoDate: string | null | undefined): string {
  if (!isoDate) return '—';
  const match = /^(\d{4})-(\d{2})-(\d{2})/.exec(isoDate.trim());
  if (!match) return isoDate;
  return `${match[3]}/${match[2]}/${match[1]}`;
}

/**
 * Formatea un Instant ISO UTC a hora local del operador en dd/MM/yyyy HH:mm (ADR-0006).
 */
export function formatInstantDateTime(isoInstant: string | null | undefined): string {
  if (!isoInstant) return '—';
  const d = new Date(isoInstant);
  if (Number.isNaN(d.getTime())) return isoInstant;
  return new Intl.DateTimeFormat('es-BO', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(d);
}

export function formatCurrency(amount: number | null | undefined, currency = 'BOB'): string {
  const safe = typeof amount === 'number' && !Number.isNaN(amount) ? amount : 0;
  return `${currency} ${safe.toLocaleString('es-BO', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}

export function formatBytes(bytes: number | null | undefined): string {
  if (bytes === null || bytes === undefined || Number.isNaN(bytes)) return '—';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;
}

export function isoDaysAgo(days: number): string {
  const d = new Date();
  d.setUTCDate(d.getUTCDate() - days);
  return d.toISOString().slice(0, 10);
}

export function isoToday(): string {
  return new Date().toISOString().slice(0, 10);
}
