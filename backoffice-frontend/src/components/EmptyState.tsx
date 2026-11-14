import type { LucideIcon } from 'lucide-react';

interface EmptyStateProps {
  message: string;
  icon?: LucideIcon;
  className?: string;
}

export function EmptyState({ message, icon: Icon, className = '' }: EmptyStateProps) {
  return (
    <div
      className={`p-8 text-center text-sm text-slate-500 dark:text-slate-400 flex flex-col items-center gap-2 ${className}`}
    >
      {Icon && <Icon className="h-6 w-6 text-slate-400 dark:text-slate-500" />}
      <span>{message}</span>
    </div>
  );
}

interface TableEmptyRowProps {
  colSpan: number;
  message: string;
}

export function TableEmptyRow({ colSpan, message }: TableEmptyRowProps) {
  return (
    <tr>
      <td colSpan={colSpan} className="p-8 text-center text-sm text-slate-500 dark:text-slate-400">
        {message}
      </td>
    </tr>
  );
}
