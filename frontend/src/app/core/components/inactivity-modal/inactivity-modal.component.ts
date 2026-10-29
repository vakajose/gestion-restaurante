import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IdleTimerService } from '../../services/idle-timer.service';

@Component({
  selector: 'app-inactivity-modal',
  standalone: true,
  imports: [CommonModule],
  template: `
    @if (idleTimerService.isWarningModalOpen()) {
      <div
        data-testid="inactivity-warning-modal"
        class="fixed inset-0 z-50 flex items-center justify-center bg-dark/60 backdrop-blur-xs p-4"
        role="dialog"
        aria-modal="true"
        aria-labelledby="inactivity-modal-title"
      >
        <div class="card max-w-md w-full bg-white dark:bg-darkcard border border-border dark:border-darkborder rounded-md shadow-xl p-6 space-y-5 text-center">
          <div class="w-14 h-14 rounded-full bg-lightwarning text-warning flex items-center justify-center text-2xl mx-auto">
            ⏳
          </div>

          <div class="space-y-1.5">
            <h2 id="inactivity-modal-title" class="text-lg font-bold text-link dark:text-white">
              Tu sesión está por expirar por inactividad
            </h2>
            <p class="text-xs sm:text-sm text-bodytext dark:text-darklink">
              Por seguridad de la terminal, cerraremos tu sesión automáticamente en:
            </p>
          </div>

          <div
            data-testid="idle-countdown-display"
            class="py-3 px-4 rounded-md bg-lightgray dark:bg-dark border border-border dark:border-darkborder text-2xl font-bold font-mono text-warning"
          >
            {{ idleTimerService.remainingSeconds() }} s
          </div>

          <div class="flex items-center justify-center gap-3 pt-2">
            <button
              type="button"
              data-testid="idle-logout-btn"
              (click)="idleTimerService.handleSessionExpiredByInactivity()"
              class="px-4 py-2.5 rounded-md border border-border dark:border-darkborder text-xs sm:text-sm font-semibold text-error hover:bg-lighterror/30 transition-colors"
            >
              Cerrar sesión ahora
            </button>

            <button
              type="button"
              data-testid="idle-continue-btn"
              (click)="idleTimerService.continueWorking()"
              class="btn bg-primary hover:bg-primary/90 text-white px-5 py-2.5 text-xs sm:text-sm font-semibold shadow-sm"
            >
              Continuar trabajando
            </button>
          </div>
        </div>
      </div>
    }
  `,
})
export class InactivityWarningModalComponent {
  readonly idleTimerService = inject(IdleTimerService);
}
