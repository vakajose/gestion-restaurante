import { AlertTriangle, CheckCircle2, Info, XCircle } from 'lucide-react';

interface AlertBannerProps {
  tone: 'error' | 'success' | 'warning' | 'info';
  title?: string;
  message: string;
}

const ALERT_STYLES = {
  error: {
    container: 'border-rose-200 bg-danger-light text-danger',
    icon: XCircle,
  },
  success: {
    container: 'border-emerald-200 bg-success-light text-success',
    icon: CheckCircle2,
  },
  warning: {
    container: 'border-amber-200 bg-warning-light text-warning',
    icon: AlertTriangle,
  },
  info: {
    container: 'border-sky-200 bg-info-light text-info',
    icon: Info,
  },
};

export function AlertBanner({ tone, title, message }: AlertBannerProps) {
  const { container, icon: Icon } = ALERT_STYLES[tone];
  return (
    <div role="alert" className={`flex items-start gap-3 rounded-xl border px-4 py-3 text-sm ${container}`}>
      <Icon className="h-5 w-5 shrink-0 mt-0.5" />
      <div>
        {title && <p className="font-bold">{title}</p>}
        <p>{message}</p>
      </div>
    </div>
  );
}
