import { useEffect, type ReactNode } from 'react';
import { X } from 'lucide-react';

interface ModalProps {
  open: boolean;
  onClose: () => void;
  title: ReactNode;
  subtitle?: ReactNode;
  maxWidth?: 'md' | 'xl' | '2xl';
  closeAriaLabel?: string;
  children: ReactNode;
  contentClassName?: string;
}

const WIDTH_CLASSES: Record<NonNullable<ModalProps['maxWidth']>, string> = {
  md: 'max-w-md',
  xl: 'max-w-xl',
  '2xl': 'max-w-2xl',
};

export function Modal({
  open,
  onClose,
  title,
  subtitle,
  maxWidth = 'md',
  closeAriaLabel = 'Cerrar modal',
  children,
  contentClassName = '',
}: ModalProps) {
  useEffect(() => {
    if (!open) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [open, onClose]);

  if (!open) {
    return null;
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4 backdrop-blur-[1px]"
      role="dialog"
      aria-modal="true"
    >
      <div
        className={`card w-full ${WIDTH_CLASSES[maxWidth]} max-h-[90vh] overflow-y-auto p-6 space-y-4 ${contentClassName}`}
      >
        <div className="flex items-start justify-between gap-3">
          <div className="min-w-0">
            <h2 className="text-lg font-bold text-slate-900 dark:text-slate-100">{title}</h2>
            {subtitle && (
              <div className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">{subtitle}</div>
            )}
          </div>
          <button
            type="button"
            aria-label={closeAriaLabel}
            onClick={onClose}
            className="text-slate-400 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200 transition-colors shrink-0"
          >
            <X className="h-5 w-5" />
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}
