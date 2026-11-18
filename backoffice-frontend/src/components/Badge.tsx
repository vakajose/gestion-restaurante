import type { ReactNode } from 'react';

export type BadgeTone = 'neutral' | 'primary' | 'success' | 'warning' | 'danger' | 'info';

interface BadgeProps {
  tone?: BadgeTone;
  children: ReactNode;
  className?: string;
}

const TONE_CLASSES: Record<BadgeTone, string> = {
  neutral: 'bg-slate-100 text-slate-700 border-slate-200 dark:bg-slate-800 dark:text-slate-300 dark:border-slate-700',
  primary: 'bg-primary-light text-primary border-indigo-200 dark:bg-indigo-950/60 dark:text-indigo-300 dark:border-indigo-800',
  success: 'bg-success-light text-success border-emerald-200 dark:bg-emerald-950/60 dark:text-emerald-300 dark:border-emerald-800',
  warning: 'bg-warning-light text-warning border-amber-200 dark:bg-amber-950/60 dark:text-amber-300 dark:border-amber-800',
  danger: 'bg-danger-light text-danger border-rose-200 dark:bg-rose-950/60 dark:text-rose-300 dark:border-rose-800',
  info: 'bg-info-light text-info border-sky-200 dark:bg-sky-950/60 dark:text-sky-300 dark:border-sky-800',
};

export function Badge({ tone = 'neutral', children, className = '' }: BadgeProps) {
  return (
    <span
      className={`inline-flex items-center gap-1 rounded-full border px-2.5 py-0.5 text-xs font-semibold ${TONE_CLASSES[tone]} ${className}`}
    >
      {children}
    </span>
  );
}
