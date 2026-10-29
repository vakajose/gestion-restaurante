import { Component, inject } from '@angular/core';
import { KeyValuePipe } from '@angular/common';
import { NotificationService, ToastItem } from '../../../../core/services/notification.service';
import { IconComponent, SolarIconName } from '../icon/icon.component';

/**
 * Portal Global de Notificaciones Toast (OBS-UI-08 / ADR-0014).
 * Se monta en la raíz del AppComponent con z-[9999] para proyectar errores
 * de negocio (RFC 7807 ProblemDetail) y confirmaciones siempre por encima
 * de cualquier modal activo (z-[100]) o cabecera (z-30).
 */
@Component({
  selector: 'app-toast-portal',
  standalone: true,
  imports: [KeyValuePipe, IconComponent],
  template: `
    @if (notificationService.toasts().length > 0) {
      <div
        data-testid="toast-portal"
        aria-live="assertive"
        class="fixed top-4 right-4 z-[9999] flex flex-col gap-2.5 max-w-md w-[calc(100vw-2rem)] sm:w-96 pointer-events-none"
      >
        @for (toast of notificationService.toasts(); track toast.id) {
          <div
            [attr.data-testid]="'toast-item-' + toast.type"
            role="alert"
            [class]="toastClasses(toast)"
          >
            <div class="flex items-start gap-3">
              <div [class]="iconBadgeClasses(toast)" class="w-8 h-8 rounded-md flex items-center justify-center shrink-0 mt-0.5">
                <app-icon [name]="iconName(toast)" size="md"></app-icon>
              </div>

              <div class="flex-1 min-w-0">
                <div class="flex items-center justify-between gap-2">
                  <p class="text-sm font-bold text-link dark:text-white truncate">
                    {{ toast.title }}
                  </p>
                  @if (toast.errorCode) {
                    <span class="text-[10px] font-mono px-1.5 py-0.5 rounded bg-slate-200/70 dark:bg-slate-700/80 text-bodytext dark:text-darklink shrink-0">
                      {{ toast.errorCode }}
                    </span>
                  }
                </div>

                <p class="text-xs text-bodytext dark:text-slate-300 mt-1 leading-relaxed break-words">
                  {{ toast.message }}
                </p>

                @if (toast.invalidParams) {
                  <ul class="mt-2 space-y-1 border-t border-border pt-1.5 text-[11px] text-[#be123c] dark:text-rose-300">
                    @for (entry of toast.invalidParams | keyvalue; track entry.key) {
                      <li class="flex items-start gap-1">
                        <span class="font-semibold">{{ entry.key }}:</span>
                        <span>{{ entry.value }}</span>
                      </li>
                    }
                  </ul>
                }
              </div>

              <button
                type="button"
                data-testid="toast-dismiss-btn"
                (click)="notificationService.dismiss(toast.id)"
                class="p-1 rounded-md text-bodytext/70 hover:text-link dark:text-darklink dark:hover:text-white hover:bg-canvas transition-colors shrink-0 cursor-pointer"
                aria-label="Cerrar notificación"
              >
                <app-icon name="close" size="sm"></app-icon>
              </button>
            </div>
          </div>
        }
      </div>
    }
  `,
})
export class ToastPortalComponent {
  readonly notificationService = inject(NotificationService);

  toastClasses(toast: ToastItem): string {
    const borderAccent: Record<ToastItem['type'], string> = {
      success: 'border-l-4 border-l-[#16a34a]',
      error: 'border-l-4 border-l-[#e11d48]',
      warning: 'border-l-4 border-l-[#d97706]',
      info: 'border-l-4 border-l-primary',
    };
    return `pointer-events-auto bg-surface border border-border ${borderAccent[toast.type]} rounded-md shadow-xl p-4 transition-all`;
  }

  iconBadgeClasses(toast: ToastItem): string {
    switch (toast.type) {
      case 'success':
        return 'bg-[#dcfce7] text-[#15803d] dark:bg-emerald-500/20 dark:text-emerald-300';
      case 'error':
        return 'bg-[#ffe4e6] text-[#be123c] dark:bg-rose-500/20 dark:text-rose-300';
      case 'warning':
        return 'bg-[#fef3c7] text-[#b45309] dark:bg-amber-500/20 dark:text-amber-300';
      case 'info':
      default:
        return 'bg-lightprimary text-primary-emphasis dark:bg-primary/20 dark:text-[#93b4ff]';
    }
  }

  iconName(toast: ToastItem): SolarIconName {
    switch (toast.type) {
      case 'success':
        return 'check-circle';
      case 'error':
        return 'close-circle';
      case 'warning':
        return 'danger-triangle';
      case 'info':
      default:
        return 'info-circle';
    }
  }
}
