const STYLE_MAP: Record<string, { label: string; classes: string }> = {
  ACTIVE: { label: 'Activo', classes: 'bg-success-light text-success border-emerald-200' },
  TRIAL: { label: 'Prueba (Trial)', classes: 'bg-info-light text-info border-sky-200' },
  GRACE_PERIOD: { label: 'En Gracia', classes: 'bg-warning-light text-warning border-amber-200' },
  SUSPENDED: { label: 'Suspendido', classes: 'bg-danger-light text-danger border-rose-200' },
  NONE: { label: 'Sin Bloqueo', classes: 'bg-slate-100 text-slate-600 border-slate-200' },
  POS_AND_SHIFT_ONLY: {
    label: 'Bloqueo POS / Turno',
    classes: 'bg-warning-light text-warning border-amber-200',
  },
  TOTAL: { label: 'Bloqueo Total', classes: 'bg-danger-light text-danger border-rose-200' },
  PAID: { label: 'Pagado', classes: 'bg-success-light text-success border-emerald-200' },
  PARTIAL: { label: 'Pago Parcial', classes: 'bg-info-light text-info border-sky-200' },
  PENDING: { label: 'Pendiente / Mora', classes: 'bg-warning-light text-warning border-amber-200' },
  WAIVED: { label: 'Condonado', classes: 'bg-slate-100 text-slate-600 border-slate-200' },
  UP: { label: 'UP', classes: 'bg-success-light text-success border-emerald-200' },
  DOWN: { label: 'DOWN', classes: 'bg-danger-light text-danger border-rose-200' },
  UNKNOWN: { label: 'UNKNOWN', classes: 'bg-slate-100 text-slate-600 border-slate-200' },
  SUPERADMIN: { label: 'SUPERADMIN', classes: 'bg-primary-light text-primary-hover border-indigo-200' },
  PLATFORM_OPERATOR: {
    label: 'OPERADOR',
    classes: 'bg-secondary-light text-info border-sky-200',
  },
};

export function StatusBadge({ value, customLabel }: { value: string; customLabel?: string }) {
  const entry = STYLE_MAP[value] ?? {
    label: value,
    classes: 'bg-slate-100 text-slate-700 border-slate-200',
  };

  return (
    <span
      className={`inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold ${entry.classes}`}
    >
      {customLabel ?? entry.label}
    </span>
  );
}
