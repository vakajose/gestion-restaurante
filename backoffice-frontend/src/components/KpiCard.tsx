import type { LucideIcon } from 'lucide-react';

interface KpiCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: LucideIcon;
  tone?: 'primary' | 'success' | 'warning' | 'danger' | 'info';
}

const TONE_CLASSES: Record<NonNullable<KpiCardProps['tone']>, string> = {
  primary: 'bg-primary-light text-primary',
  success: 'bg-success-light text-success',
  warning: 'bg-warning-light text-warning',
  danger: 'bg-danger-light text-danger',
  info: 'bg-info-light text-info',
};

export function KpiCard({ title, value, subtitle, icon: Icon, tone = 'primary' }: KpiCardProps) {
  return (
    <div className="card p-5 flex items-start justify-between gap-4">
      <div className="min-w-0">
        <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">{title}</p>
        <p className="mt-1.5 text-2xl font-bold text-slate-800 truncate">{value}</p>
        {subtitle && <p className="mt-1 text-xs text-slate-500">{subtitle}</p>}
      </div>
      <div className={`rounded-xl p-3 shrink-0 ${TONE_CLASSES[tone]}`}>
        <Icon className="h-5 w-5" />
      </div>
    </div>
  );
}
